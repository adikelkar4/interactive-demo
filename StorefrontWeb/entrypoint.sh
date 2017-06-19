#!/usr/bin/env bash

#populate catalina.properties
/start.sh

tail -f /var/log/dmesg &

exec "$@"