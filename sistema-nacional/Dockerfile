# ---------- Etapa 1: compilar el jar ----------
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app

# Primero solo el pom: las dependencias quedan en una capa cacheada y no se bajan de nuevo
# cada vez que cambia el código.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagen liviana solo con el JRE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Hora argentina también para los logs (la app igual la fija sola en ZonaHorariaConfig)
ENV TZ=America/Argentina/Buenos_Aires

# Correr sin root
RUN useradd --system --no-create-home app
USER app

COPY --from=build /app/target/*.jar app.jar

# 8090 en local. Render inyecta PORT y application.yaml lo toma (server.port: ${PORT:8090})
EXPOSE 8090

# MaxRAMPercentage: usar la memoria del contenedor (el plan free de Render tiene 512 MB)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
