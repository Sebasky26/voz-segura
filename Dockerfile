# Etapa de Compilación
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom-root.xml .
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de Ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app


# Copiar el fat jar generado por Spring Boot
COPY --from=build /app/target/voz-segura-core-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto del Core (según DIDIT_INTEGRATION.md es 8082)
EXPOSE 8082


# Variables de entorno por defecto (Render sobreescribe estas si están definidas en el panel)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8082

# Ejecutar
ENTRYPOINT ["java", "-jar", "app.jar"]