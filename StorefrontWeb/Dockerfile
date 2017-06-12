FROM tomcat:7-jre7

RUN rm -rf /usr/local/tomcat/webapps/
ADD src/main/webapp /usr/local/tomcat/webapps
ADD entrypoint.sh /

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]