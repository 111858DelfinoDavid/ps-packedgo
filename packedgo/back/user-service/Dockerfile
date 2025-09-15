# Usa una imagen base de Java con el JDK 21. Eclipse Temurin es una opción popular y confiable.
FROM openjdk:21

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo JAR ejecutable de tu aplicación al contenedor
# Asegúrate de que el nombre del archivo JAR coincida con el que genera Maven (por ejemplo, user-service-0.0.1-SNAPSHOT.jar)
COPY target/user-service-0.0.1-SNAPSHOT.jar /app/app.jar

# Define el punto de entrada para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]