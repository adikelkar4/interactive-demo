FROM tomcat:7-jre8

ADD target/StorefrontUser.jar /usr/bin/StorefrontUser.jar

COPY StorefrontUser.sh /
COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]