FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/psoft-g1-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
