FROM openjdk:17-jdk-slim

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar
RUN cp build/libs/*-SNAPSHOT.jar mail-sender.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "mail-sender.jar"]