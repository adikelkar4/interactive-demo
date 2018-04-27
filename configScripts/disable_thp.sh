#!/bin/bash

if [ ! -f /etc/tuned/myprofile-nothp/tuned.conf ]; then
    mkdir -p /etc/tuned/myprofile-nothp
    touch /etc/tuned/myprofile-nothp/tuned.conf
    echo "[main]" >> /etc/tuned/myprofile-nothp/tuned.conf
    echo "include=throughput-performance" >> /etc/tuned/myprofile-nothp/tuned.conf
    echo "[vm]" >> /etc/tuned/myprofile-nothp/tuned.conf
    echo "transparent_hugepages=never" >> /etc/tuned/myprofile-nothp/tuned.conf
    chmod +x /etc/tuned/myprofile-nothp/tuned.conf
fi

tuned-adm profile myprofile-nothp
systemctl restart tuned

###### configure openebs

mkdir -p /opt/openebs
mkdir -p /var/openebs
chown -R dockerroot /opt/openebs
chown -R dockerroot /var/openebs

setenforce 0