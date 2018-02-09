#!/usr/bin/env bash
set -x

env

systemctl start NetworkManager
systemctl enable NetworkManager

#### set ssh keys
touch /root/.ssh/public.key
echo "${PUB}" >>/root/.ssh/public.key
cat /root/.ssh/public.key >>/root/.ssh/authorized_keys
chmod 400 /root/.ssh/public.key

touch /root/.ssh/id_rsa
echo "${PEM}" >>/root/.ssh/id_rsa
chmod 400 /root/.ssh/id_rsa

#### configure docker group
groupadd docker
usermod -aG docker centos

#### configure docker network
sed -i '/OPTIONS=.*/c\\OPTIONS="--selinux-enabled --insecure-registry 172.30.0.0/16"' /etc/sysconfig/docker

systemctl start docker
systemctl enable docker

#### allow root login
sed -i -- "s/#PermitRootLogin yes/PermitRootLogin yes/g" /etc/ssh/sshd_config
sed -i -- "s/PasswordAuthentication no/#PasswordAuthentication no/g" /etc/ssh/sshd_config

#### disable strict key checking
echo "StrictHostKeyChecking no" >> /etc/ssh/ssh_config

systemctl restart sshd

### run on master
if [ "${NODE_TYPE}" == "MASTER" ]; then

    #### clone ansible repo
    mkdir -p /local/workspace && cd /local/workspace
    git clone http://github.com/openshift/openshift-ansible
    cd openshift-ansible
    git checkout release-3.7

    #### grab the IP addresses of deployed nodes
    function getInstanceIp {
        count=1
        while [ "$instanceIp" == "" ]; do
            instanceIp="$( aws autoscaling describe-auto-scaling-instances --region ${REGION} --output text \
              --query """AutoScalingInstances[?AutoScalingGroupName=='${NODEASGID}'].InstanceId""" \
               | xargs -n1 aws ec2 describe-instances --instance-ids $ID --region ${REGION} \
                --query Reservations[].Instances[].PrivateDnsName --output text )"
            sleep 5
            if [ "$count" == 10 ]; then
                echo "timed out getting node list"
                exit 1
            fi
            ((count++))
        done

        echo $instanceIp
    }

    #make sure all nodes are in list
    while [ "$listCount" != "$DESIRED" ]; do
        InstanceList=$(getInstanceIp)
        listCount="$( echo $InstanceList | wc -l )"
        sleep 5
    done

    touch /tmp/nodes.txt

    #### configure inventory
    for ip in $InstanceList
    do
        echo "$ip  openshift_schedulable=true openshift_node_labels=\"{'region': ''${REGION}'', 'zone': 'default'}\"" >>/tmp/nodes.txt
    done

    nodelist=$(</tmp/nodes.txt)

    echo $nodelist

    MASTERIP="$( curl http://169.254.169.254/latest/meta-data/public-ipv4/ )"
    sed -i -- "s/@@master@@/$MASTERIP/g" /local/scripts/openshift-inventory.erb
    sed -i -- "s/@@nodes@@/$nodeslist/g" /local/scripts/openshift-inventory.erb

    #### run ansible to deploy openshift cluster
    ansible-playbook -i /local/scripts/openshift-inventory.erb /local/workspace/openshift-ansible/playbooks/byo/config.yml -vvv >>/local/deploy-cluster.log
fi