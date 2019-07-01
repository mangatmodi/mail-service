FROM openjdk:11-jre-slim

ARG JAR_NAME
ARG HOST_ADDRESS

ADD ${JAR_NAME} ${JAR_NAME}
ADD start-app.sh start-app.sh

EXPOSE 8080
ENV JAR_NAME "${JAR_NAME}"
ENV HOST_ADDRESS "${HOST_ADDRESS}"

CMD ./start-app.sh
