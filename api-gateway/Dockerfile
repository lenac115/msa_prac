FROM openjdk:17-jdk-slim
VOLUME /tmp
ARG JAR_FILE=build/libs/api-gateway-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
ENV SPRING_PROFILES_ACTIVE=docker