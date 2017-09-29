#!/usr/bin/env bash

T=$(date +"%T")
SNAP=$(echo $T | sed -r 's/[:]+//g')

curl -XPUT "localhost:9200/_snapshot/index_backup/snapshot_$SNAP?wait_for_completion=true"
