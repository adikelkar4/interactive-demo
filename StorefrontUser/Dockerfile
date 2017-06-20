FROM tomcat:7-jre8

#ADD http://jenkins-build.internal.nuodb.com/job/interactive-demo-github-pipelines/job/master/lastSuccessfulBuild/artifact/StorefrontUser/target/StorefrontUser.jar /usr/bin/
ADD target/StorefrontUser.jar /usr/bin/StoreFrontUser.sh.jar

COPY StorefrontUser.sh /
COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]