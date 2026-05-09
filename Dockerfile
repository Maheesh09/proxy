# Simple runtime-only image — JAR is pre-built locally via 'mvn package -DskipTests'
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/proxymaze-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx512m", \
  "-XX:+UseContainerSupport", \
  "-jar", "app.jar"]
