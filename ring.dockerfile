FROM tomcat
MAINTAINER Guilherme Oliveira Fonseca de Almeida
COPY target/ring.war /usr/local/tomcat/webapps/
WORKDIR /usr/local/tomcat/
RUN mkdir -p ~/.ring/;mkdir -p ~/tmp_files/ring/
EXPOSE 8080
