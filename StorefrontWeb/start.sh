#!/bin/bash

# Purpose: Create appropriate command line from environment variables
# and launch the java process.

# Usage:

# 1) rename this file to the same base name as the jar file

# 2) put the jar file in /usr/bin

# 3) It will process all environment variables that begin with "ARG_",
#    convert all '_' to '.', and pass them through as name/value pairs
#    on the command line

PROPFILE=/usr/local/tomcat/conf/catalina.properties

for var in $(env | cut -d= -f 1 | egrep "^ARG_"); do
    value=$(eval echo \$$var)
    if egrep -q "^$var=" $PROPFILE ; then
	    sed -i "s/^$var=.*/$name=$value/" $PROPFILE
    else
	    echo "$var=$value"  >> $PROPFILE
    fi
done

exec catalina.sh run
