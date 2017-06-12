FROM java

COPY target/StorefrontUser-1.0-SNAPSHOT.jar /usr/bin/storefrontuser.jar

COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]