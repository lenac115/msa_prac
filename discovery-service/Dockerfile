FROM openjdk:17-jdk-slim
VOLUME /tmp
ARG JAR_FILE=build/libs/discovery-service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-jar", "/app.jar"]
