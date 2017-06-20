#!/bin/bash

# Purpose: Create appropriate command line from environment variables
# and launch the java process.

# Usage:

# 1) rename this file to the same base name as the jar file

# 2) put the jar file in /usr/bin

# 3) It will process all environment variables that begin with "ARG_",
#    convert all '_' to '.', and pass them through as name/value pairs
#    on the command line

jar=$(basename $0)

for var in $(env | cut -d= -f 1 | egrep "^ARG_"); do
    name=$(echo $var | cut -c 5- | sed 's/ARG_//g')
    value=$(eval echo \$$var)
    args=" '$name=$value' $args"
done

exec java -jar /usr/bin/${jar}.jar $args
