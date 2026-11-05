# Compila dentro de la imagen: la VM de Oracle no necesita Maven ni JDK.
# Ambas imágenes son multiarquitectura (sirven para ARM64 de Oracle y x86).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --create-home app
USER app
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
