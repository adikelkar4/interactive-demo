FROM tomcat:7-jre8

RUN rm -rf /usr/local/tomcat/webapps/
ADD http://jenkins-build.internal.nuodb.com/job/interactive-demo-github-pipelines/job/master/lastSuccessfulBuild/artifact/StorefrontWeb/target/StorefrontWeb.war /usr/local/tomcat/webapps/
#ADD target/StorefrontWeb.war /usr/local/tomcat/webapps/
ADD setenv.sh /usr/local/tomcat/bin/

ADD entrypoint.sh /
ADD start.sh /

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
CMD ["/start.sh"]