# Etapa de Compilación
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom-root.xml .
COPY gateway/pom.xml gateway/
COPY gateway/src gateway/src
WORKDIR /app/gateway
RUN mvn clean package -DskipTests

# Etapa de Ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el jar generado
COPY --from=build /app/gateway/target/*.jar app.jar

# Exponer el puerto del Gateway (estándar 8080)
EXPOSE 8080

# Variables de entorno
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Ejecutar
ENTRYPOINT ["java", "-jar", "app.jar"]