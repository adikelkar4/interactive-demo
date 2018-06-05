#!/usr/bin/env bash

oc adm policy add-cluster-role-to-user cluster-admin admin --as=system:admin
htpasswd -cb /etc/origin/htpasswd admin admin

oc adm policy add-scc-to-user hostaccess admin --as=system:admin
oc adm policy add-scc-to-user anyuid -z default --as=system:admin

cd /local/

git clone https://github.com/openebs/openebs.git

cd /local/openebs

git checkout a15744945d023cbdb9b99b856c13ac29d2a1ae1f

oc create -f /local/openebs/k8s/openebs-operator.yaml
oc create -f /local/openebs/k8s/openebs-storageclasses.yaml

/local/scripts/ssh.sh