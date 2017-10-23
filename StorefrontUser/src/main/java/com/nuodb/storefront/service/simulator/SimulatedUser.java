/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service.simulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.exception.SQLGrammarException;

import com.nuodb.storefront.dal.BaseDao;
import com.nuodb.storefront.exception.CartEmptyException;
import com.nuodb.storefront.exception.CustomerNotFoundException;
import com.nuodb.storefront.exception.ProductNotFoundException;
import com.nuodb.storefront.exception.UnsupportedStepException;
import com.nuodb.storefront.model.dto.Category;
import com.nuodb.storefront.model.dto.ProductFilter;
import com.nuodb.storefront.model.dto.ProductReviewFilter;
import com.nuodb.storefront.model.dto.Workload;
import com.nuodb.storefront.model.dto.WorkloadStep;
import com.nuodb.storefront.model.entity.Cart;
import com.nuodb.storefront.model.entity.CartSelection;
import com.nuodb.storefront.model.entity.Customer;
import com.nuodb.storefront.model.entity.Product;
import com.nuodb.storefront.model.type.ProductSort;

/**
 * Runs through the steps specified by a {@class WorkloadType} field.
 */
public class SimulatedUser implements IWorker, Runnable {
	private static final long MIN_BACKOFF_DELAY = 1000 * 1;
	private static final long MAX_BACKOFF_DELAY = 1000 * 60;

	private final ISimulator simulator;
	private final Workload workloadType;
	private final Random rnd = new Random();
	private Customer customer;
	private ProductFilter filter;
	private List<Long> productIds;
	private static List<String> s_categories;
	private boolean isCartEmpty = true;
	private Long selectedProductId;

	// Fibonacci backoff retry tracking
	private long priorBackoffDelay = 0;
	private long nextBackoffDelay = MIN_BACKOFF_DELAY;
	public static final int DEFAULT_ANALYTIC_MAX_AGE = 60 * 30;// 30 min
	public static final int DEFAULT_SESSION_TIMEOUT_SEC = 60 * 20;// 20 min

	public SimulatedUser(ISimulator simulator, Workload workloadType) {
		if (workloadType == null) {
			throw new IllegalArgumentException("workloadType");
		}
		this.simulator = simulator;
		this.workloadType = workloadType;
	}

	public Workload getWorkload() {
		return workloadType;
	}

	public long doWork() throws InterruptedException {
		customer = simulator.getService().getOrCreateCustomer((customer == null) ? 0 : customer.getId(), workloadType);
		WorkloadStep[] steps = workloadType.getSteps();

		if (steps.length == 0) {
			return IWorker.COMPLETE_NO_REPEAT;
		}
		Logger logger = simulator.getService().getLogger(getClass());
		try {
			for (int i = 0; i < steps.length; i++) {				
				doWork(steps[i]);
//				logger.info(workloadType.getName() + ": Sleeping for " + sleepTime + "ms between steps " + i + " and " + (i+1));
				Thread.sleep(500);
			}
			long sleepTime = workloadType.calcNextThinkTimeMs();
			Thread.sleep(sleepTime);
		} catch (RuntimeException e) {
			if (isExceptionRecoverable(e)) {
				recoverFromException(e);

				long retryDelay = getRetryDelay();
				logger.info("Encountered recoverable exception with simulated user \"" + getWorkload().getName()
						+ "\". " + "Will retry in " + retryDelay + " ms.", e);
				Thread.sleep(retryDelay);
				throw new RetryWorkException(retryDelay);
			}
			throw e;
		}
		return IWorker.COMPLETE;
	}

	protected long getRetryDelay() {
		if (nextBackoffDelay >= MAX_BACKOFF_DELAY) {
			return MAX_BACKOFF_DELAY;
		}

		long retryDelay = nextBackoffDelay;
		nextBackoffDelay += priorBackoffDelay;
		priorBackoffDelay = retryDelay;
		return retryDelay;
	}

	protected boolean isExceptionRecoverable(RuntimeException e) {
		if (e instanceof SQLGrammarException) {
			// The schema has been busted. Someone may have dropped a critical
			// table. Retrying the worker won't help.
			return false;
		}

		return true;
	}

	protected void recoverFromException(RuntimeException e) {
		if (e instanceof ObjectNotFoundException || e instanceof CustomerNotFoundException) {
			customer = null;
			filter = null;
			productIds = null;
			s_categories = null;
			isCartEmpty = true;
			selectedProductId = null;
		} else if (e instanceof ProductNotFoundException) {
			// Product is missing. Just reset product-related stats.
			productIds = null;
			selectedProductId = null;
			filter = null;
			s_categories = null;
		}
	}

	protected void doWork(WorkloadStep step) {

		switch (step) {
		case BROWSE:
			doBrowse();
			break;

		case BROWSE_NEXT_PAGE:
			doBrowseNextPage();
			break;

		case BROWSE_SEARCH:
			doBrowseSearch();
			break;

		case BROWSE_CATEGORY:
			doBrowseCategory();
			break;

		case BROWSE_SORT:
			doBrowseCategory();
			break;

		case PRODUCT_VIEW_DETAILS:
			doProductViewDetails();
			break;

		case PRODUCT_ADD_TO_CART:
			doProductAddToCart();
			break;

		case PRODUCT_ADD_REVIEW:
			doProductAddReview();
			break;

		case CART_VIEW:
			doCartView();
			break;

		case CART_UPDATE:
			doCartUpdate();
			break;

		case CART_CHECKOUT:
			doCartCheckout();
			break;

		case ADMIN_RUN_REPORT:
			doRunReport();
			break;

		default:
			throw new UnsupportedStepException();
		}

		simulator.incrementStepCompletionCount(step);
	}

	protected void doBrowse() {
		filter = new ProductFilter();
		setProductIds(simulator.getService().getProducts(filter).getResult());
		setCategoryNames(simulator.getService().getCategories().getResult());
	}

	protected void doBrowseNextPage() {
		getOrFetchProductList();
		filter.setPage(filter.getPage() + 1);
		setProductIds(simulator.getService().getProducts(filter).getResult());
	}

	protected void doBrowseSearch() {
		filter = new ProductFilter();
		filter.setCategories(new ArrayList<String>());

		// Base the search off a random product name (if available)
		if (getOrFetchProductList()) {
			Long productId = pickRandomProductId();
			if (productId != null) {
				Product product = simulator.getService().getProductDetails(productId.intValue());
				if (product != null) {
					filter.setMatchText(product.getName().substring(0, Math.min(5, product.getName().length())));
					return;
				}
			}
		}

		filter.setMatchText("DNE search");
	}

	protected void doBrowseCategory() {
		filter = new ProductFilter();
		filter.setCategories(new ArrayList<String>());
		if (getOrFetchCategories()) {
			String categoryName = pickRandomCategoryName();
			filter.getCategories().add(categoryName);
		} else {
			filter.getCategories().add("DNE category");
		}
		setProductIds(simulator.getService().getProducts(filter).getResult());
		setCategoryNames(simulator.getService().getCategories().getResult());
	}

	protected void doBrowseSort() {
		filter = new ProductFilter();
		filter.setSort(ProductSort.values()[rnd.nextInt(ProductSort.values().length)]);
		setProductIds(simulator.getService().getProducts(filter).getResult());
	}

	protected void doProductViewDetails() {
		if (getOrFetchProductList()) {
			selectedProductId = pickRandomProductId();
			simulator.getService().getProductDetails(selectedProductId);
			simulator.getService().getProductReviews(new ProductReviewFilter(1, 10, selectedProductId));
		}
	}

	protected void doProductAddToCart() {
		if (getOrFetchProduct()) {
			simulator.getService().addToCart(customer.getId(), selectedProductId, rnd.nextInt(10) + 1);
			simulator.getService().getCustomerCart(customer.getId());
			isCartEmpty = false;
		}
	}

	protected void doProductAddReview() {
		if (getOrFetchProduct()) {
			int rating = rnd.nextInt(5) + 1;
			String email = "Customer" + customer.getId() + "@test.com";
			String title = "Review " + System.currentTimeMillis();
			String comments = "This review was added by a load simulation tool.";
			simulator.getService().addProductReview(customer.getId(), pickRandomProductId(), title, comments, email,
					rating);
		}
	}

	protected void doCartView() {
		simulator.getService().getCustomerCart(customer.getId());
	}

	protected void doCartUpdate() {
		if (getOrFetchNonEmptyCart()) {
			Map<Long, Integer> updates = new HashMap<Long, Integer>();
			Cart cart = simulator.getService().getCustomerCart(customer.getId());
			int itemCount = 0;
			for (CartSelection item : cart.getResult()) {
				updates.put(item.getProduct().getId(), itemCount += rnd.nextInt(10));
			}
			isCartEmpty = itemCount == 0;
			simulator.getService().updateCart(customer.getId(), updates);
		}
	}

	protected void doCartCheckout() {
		if (!getOrFetchNonEmptyCart()) {
			return;
		}
		try {
			simulator.getService().checkout(customer.getId());
		} catch (CartEmptyException e) {
			// This should happen only if there's nothing in the store.
		}
		isCartEmpty = true;
	}

	protected void doRunReport() {
		simulator.getService().getStorefrontStats(SimulatedUser.DEFAULT_SESSION_TIMEOUT_SEC,
				SimulatedUser.DEFAULT_ANALYTIC_MAX_AGE);
	}

	protected boolean getOrFetchCategories() {
		if (s_categories == null) {
			doBrowse();
		}
		return !s_categories.isEmpty();
	}

	protected boolean getOrFetchProductList() {
		if (productIds == null || productIds.isEmpty()) {
			doBrowse();
		}

		return !productIds.isEmpty();
	}

	protected boolean getOrFetchProduct() {
		if (selectedProductId == null) {
			doProductViewDetails();
			if (selectedProductId == null) {
				return false;
			}
		}
		return true;
	}

	protected boolean getOrFetchNonEmptyCart() {
		if (isCartEmpty) {
			doProductAddToCart();
		}
		return !isCartEmpty;
	}

	protected String pickRandomCategoryName() {
		if (s_categories == null) {
			setCategoryNames(simulator.getService().getCategories().getResult());
		}
		return (s_categories == null || s_categories.isEmpty()) ? null
				: s_categories.get(rnd.nextInt(s_categories.size()));
	}

	protected Long pickRandomProductId() {
		return (productIds == null || productIds.isEmpty()) ? null : productIds.get(rnd.nextInt(productIds.size()));

	}

	private void setCategoryNames(Collection<Category> result) {
		if (s_categories != null) {
			return;
		}
		if (s_categories == null) {
			s_categories = new ArrayList<String>(result.size());
		}
		for (Category category : simulator.getService().getCategories().getResult()) {
			s_categories.add(category.getName());
		}
	}

	private void setProductIds(Collection<Product> products) {
		if (productIds == null) {
			productIds = new LinkedList<Long>();
		}
		productIds.clear();
		for (Product product : products) {
			productIds.add(product.getId());
		}
	}

	@Override
	public void run() {
		BaseDao.setThreadTransactionStartTime(0);
		boolean workerFailed = false;
		long startTime = System.currentTimeMillis();
		try {
			this.doWork();
		} catch (RetryWorkException e) {
			e.getRetryDelayMs();
		} catch (Exception e) {
			Logger logger = this.simulator.getService().getLogger(getClass());
			logger.warn("Simulated worker failed", e);
		}
		long duration = System.currentTimeMillis() - startTime;
		if (workerFailed) {
			this.simulator.updateWorkloadStats(this.getWorkload(), "FAILED", duration);
		} else {
			this.simulator.updateWorkloadStats(this.getWorkload(), "COMPLETE", duration);
		}
	}
}
