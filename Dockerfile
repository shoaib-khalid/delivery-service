FROM openjdk:8-alpine
RUN apk update && apk add curl bash

RUN mkdir -p /home/docker/Software
COPY target/*.jar /home/docker/Software/
ADD src/main/resources/application.properties /home/docker/Software/application.properties
COPY target/libs /home/docker/Software/libs
RUN chmod +x /home/docker/Software/*.jar 

WORKDIR /home/docker/Software
EXPOSE 3001
CMD java \
    -Dspring.config.location=/home/docker/Software/ \
    -jar /home/docker/Software/*.jar