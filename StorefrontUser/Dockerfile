FROM java

RUN curl -SL http://jenkins-build.internal.nuodb.com/job/interactive-demo-placeholder/4/com.nuodb.storefront$StorefrontUser/artifact/com.nuodb.storefront/StorefrontUser/1.0-SNAPSHOT/StorefrontUser-1.0-SNAPSHOT.jar \
    -o /usr/bin/storefrontuser.jar

COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]