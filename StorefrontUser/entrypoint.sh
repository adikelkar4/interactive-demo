#!/usr/bin/env bash

args="workload.multi_browse_and_review.users=$ARG_workloadmulti_browse_and_reviewusers db.url=$ARG_dburl workload.multi_shop.users=$ARG_workloadmulti_shopusers workload.admin_run_report.users=$ARG_workloadadmin_run_reportusers' 'app
.host=$ARG_apphost workload.multi_browse.users=$ARG_workloadmulti_browseusers db.password=$ARG_dbpassword db.user=$ARG_dbuser"

echo "java -jar /usr/bin/StorefrontUser.jar $args"

exec java -jar /usr/bin/StorefrontUser.jar $args

tail -f /var/log/dmesg