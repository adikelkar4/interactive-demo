#!/usr/bin/env bash

tail -f /var/log/dmesg &

exec "$@"