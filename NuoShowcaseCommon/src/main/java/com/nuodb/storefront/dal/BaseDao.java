/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.dal;

import java.util.concurrent.Callable;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.googlecode.genericdao.dao.hibernate.GeneralDAOImpl;

public class BaseDao extends GeneralDAOImpl implements IBaseDao {
    private static final ThreadLocal<Long> transactionStartTime = new ThreadLocal<Long>();
    
    public static void setThreadTransactionStartTime(long startTimeMs) {
        transactionStartTime.set(startTimeMs);
    }

    public void runTransaction(TransactionType transactionType, String name, final Runnable r) {
        runTransaction(transactionType, name, new Callable<Object>() {
            public Object call() throws Exception {
                r.run();
                return null;
            }
        });
    }

    public <T> T runTransaction(TransactionType transactionType, String name, Callable<T> c) {
        long startTime = System.currentTimeMillis();

        Session session = null;
        Transaction t = null;
        try {
        	session = this.getSessionFactory().getCurrentSession();
            t = session.beginTransaction();
            prepareSession(transactionType);
            T result = c.call();
            t.commit();
            onTransactionComplete(name, System.currentTimeMillis() - startTime, true);
            return result;
        } catch (Exception e) {
            if (t != null) {
                t.rollback();
            } else {
            	if (session != null) {
            		if (session.isOpen()) {            			
            			session.close();
            		}
            	}
            }
            onTransactionComplete(name, System.currentTimeMillis() - startTime, false);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    protected void prepareSession(TransactionType transactionType) {
        switch (transactionType) {
            case READ_ONLY:
                Session session = getSession();

                // FIXME: Can't mark transaction as read-only with NuoDB right
                // now, or SQL exceptions get thrown even with select statements
                // session.doWork(new Work() {
                // @Override
                // public void execute(Connection connection) throws
                // SQLException {
                // connection.setReadOnly(true);
                // }
                // });

                session.setFlushMode(FlushMode.MANUAL);
                break;

            default:
                break;
        }
    }

    protected void onTransactionComplete(String transactionName, long startTimeMs, boolean success) {
    }
}
