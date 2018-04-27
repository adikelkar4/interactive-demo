#!/usr/bin/env bash
set -x


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


#### allow root login
sed -i -- "s/#PermitRootLogin yes/PermitRootLogin yes/g" /etc/ssh/sshd_config
sed -i -- "s/PasswordAuthentication no/#PasswordAuthentication no/g" /etc/ssh/sshd_config

#### disable strict key checking
echo "StrictHostKeyChecking no" >> /etc/ssh/ssh_config

systemctl restart sshd

### run on master
if [ "${NODE_TYPE}" == "MASTER" ]; then

    #### grab the IP addresses of deployed nodes
    function getInstanceIp {
        count=1
        while [ "$instanceIp" == "" ]; do
            instanceIp="$( aws autoscaling describe-auto-scaling-instances --region ${REGION} --output text \
              --query """AutoScalingInstances[?AutoScalingGroupName=='${NODEASGID}'].InstanceId""" )"
            sleep 5
            if [ "$count" == 30 ]; then
                echo "timed out getting node list"
                exit 1
            fi
            ((count++))
        done

        aws autoscaling describe-auto-scaling-instances --region ${REGION} --output text \
              --query """AutoScalingInstances[?AutoScalingGroupName=='${NODEASGID}'].InstanceId""" \
               | xargs -n1 aws ec2 describe-instances --instance-ids $ID --region ${REGION} \
                --query 'Reservations[].Instances[].[PrivateDnsName, Placement.AvailabilityZone]' --output text >>/tmp/hostlist.txt

        cat /tmp/hostlist.txt
    }

    while [ -z "$isPopulated" ]; do
        isPopulated=$( getInstanceIp )
    done

    MASTERIP="$( curl http://169.254.169.254/latest/meta-data/local-hostname/ )"
    sed -i -- "s/@@master@@/$MASTERIP/g" /local/ansible/inventory/hosts.cluster

    #populate node list
    while read p; do
        echo $p | awk '{ print $1 " openshift_schedulable=true openshift_node_labels=\"{`region`: `infra`, `zone`: `" $2 "`}\"" }' >>/local/ansible/inventory/hosts.cluster
    done </tmp/hostlist.txt

#    #populate glusterfs list
#    echo "" >>/local/ansible/inventory/hosts.cluster
#    echo "[glusterfs]" >>/local/ansible/inventory/hosts.cluster
#    echo $MASTER " glusterfs_devices=\"[ `/dev/xvdf` ]\"" >>/local/ansible/inventory/hosts.cluster
#    while read p; do
#        echo $p | awk '{ print $1 " glusterfs_devices=\"[ `/dev/xvdf` ]\"" }' >>/local/ansible/inventory/hosts.cluster
#    done </tmp/hostlist.txt

    #fix quotes
    sed -i -- "s/\`/\'/g" /local/ansible/inventory/hosts.cluster

    while read p; do

        echo $p | awk '{ print "ssh " $1 " /local/scripts/disable_thp.sh" }' >>/local/scripts/ssh.sh

    done </tmp/hostlist.txt

    chmod +x /local/scripts/ssh.sh
fi