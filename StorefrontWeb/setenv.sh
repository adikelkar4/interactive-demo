#!/usr/bin/env bash

JAVA_OPTS="${JAVA_OPTS}"

echo JAVA_OPTS: \"${JAVA_OPTS}\"
export JAVA_OPTS

CATALINA_OPTS=""

CATALINA_OPTS=" ${CATALINA_OPTS} -Dstorefront.url=\"${ARG_storefronturl}\""
CATALINA_OPTS=" ${CATALINA_OPTS} -Dstorefront.db.name=\"${ARG_dbname}@${ARG_JDBC_URL}:48004\""
CATALINA_OPTS=" ${CATALINA_OPTS} -Dstorefront.db.user=\"${ARG_dbuser}\""
CATALINA_OPTS=" ${CATALINA_OPTS} -Dstorefront.db.password=\"${ARG_dbpassword}\""

CATALINA_OPTS=" ${CATALINA_OPTS} -Dstorefront.dbapi.user=domain"
CATALINA_OPTS=" ${CATALINA_OPTS} -Dstorefront.dbapi.password=bird"
CATALINA_OPTS=" ${CATALINA_OPTS} -Dstorefront.dbapi.host=\"${ARG_JDBC_URL}\""
CATALINA_OPTS=" ${CATALINA_OPTS} -Dstorefront.dbapi.port=8888"

echo CATALINA_OPTS: \"${CATALINA_OPTS}\"