FROM openjdk:11-jre-slim

ARG JAR_NAME

ADD ${JAR_NAME} ${JAR_NAME}

ENV JAR_NAME "${JAR_NAME}"

CMD java -jar $JAR_NAME
