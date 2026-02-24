# Paso 1: Usar Maven para compilar el proyecto
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Paso 2: Usar WildFly para ejecutar la aplicación
FROM quay.io/wildfly/wildfly:latest
# Copiamos el archivo .war generado por Maven y lo RENOMBRAMOS a Gimnasio.war
COPY --from=build target/*.war /opt/jboss/wildfly/standalone/deployments/Gimnasio.war

# Exponemos el puerto 8080 que es el que usa WildFly por defecto
EXPOSE 8080

# Comando para iniciar WildFly
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]