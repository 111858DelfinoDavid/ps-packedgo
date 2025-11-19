# Dockerfile para Stripe API
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY src ./src

# Construir la aplicación
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el JAR desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto
EXPOSE 8081

# Variables de entorno (se pueden sobrescribir)
ENV SERVER_PORT=8081

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
