FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app

# Copiamos solo lo necesario para aprovechar la cache de Docker
COPY pom.xml .
COPY src ./src

# Compilamos el proyecto y generamos el WAR
RUN mvn -q clean package -DskipTests

# Imagen final con Tomcat 10 (Jakarta / Servlet 5)
FROM tomcat:10.1-jdk17-temurin

# Limpiamos las apps por defecto de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Copiamos el WAR generado y lo desplegamos como ROOT.war
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

# Tomcat escucha en el 8080 por defecto
EXPOSE 8080

# Comando para iniciar Tomcat
CMD ["catalina.sh", "run"]