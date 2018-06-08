#!/bin/bash

oc login https://${MASTERIP}:8443 -u system:admin

# create cluster admin to setup environment with

oc adm policy add-cluster-role-to-user cluster-admin admin --as=system:admin

htpasswd -c /etc/origin/htpasswd admin -b admin

# edit scc restricted to enable plugins and root access

#oc edit scc restricted

   #change: allowHostDirVolumePlugin: true
   #        runAsUser: type: RunAsAny

oc adm policy add-scc-to-user hostaccess admin --as:system:admin

oc adm policy add-scc-to-user anyuid -z default --as=system:admin


#downlaod yaml files to deploy openEBS
cd /local/
git clone https://github.com/openebs/openebs.git
cd openebs/
git checkout f22c2e549c366880b88d90782f16bd12dd5cedc7

# deploy

oc create -f /local/openebs/k8s/openebs-operator.yaml
oc create -f /local/openebs/k8s/openebs-storageclasses.yaml