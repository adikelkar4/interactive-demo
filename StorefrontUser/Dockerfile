FROM tomcat:7-jre8

ADD http://jenkins-build.internal.nuodb.com/job/interactive-demo-github-pipelines/job/master/lastSuccessfulBuild/artifact/StorefrontUser/target/StorefrontUser.jar /usr/bin/

COPY start.sh /
COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]