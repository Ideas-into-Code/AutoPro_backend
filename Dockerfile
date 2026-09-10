# syntax=docker/dockerfile:1

# ---- Étape 1 : build du jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Couche dépendances (change rarement) séparée de la couche code, pour le cache.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q -DskipTests clean package

# ---- Étape 2 : image d'exécution (JRE seul) ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# La JVM s'adapte à la mémoire du plan Render.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

# Render fournit le port via $PORT ; Spring lit server.port=${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
