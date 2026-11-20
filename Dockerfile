# Basado en el Dockerfile de cvalverd/pet-api-oracle, adaptado a auth-users (Java 17, puerto 8081)

# ---- Etapa 1: compilar con Maven Wrapper ----
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# mvnw puede venir con saltos de linea de Windows (CRLF) si se edito en Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B clean package -DskipTests

# ---- Etapa 2: imagen liviana solo con el JRE ----
FROM eclipse-temurin:17-jre

WORKDIR /app
RUN useradd --system --no-create-home appuser \
    && mkdir -p /app/wallet \
    && chown appuser:appuser /app/wallet \
    && chmod 0700 /app/wallet
COPY --from=build /app/target/auth-users-0.0.1-SNAPSHOT.jar /app/auth-users.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

# La wallet se descarga desde S3 al arrancar (AWS) o se monta en /app/wallet (docker compose)
ENV TNS_ADMIN=/app/wallet
RUN chmod 0555 /app/docker-entrypoint.sh
USER appuser
EXPOSE 8081

ENTRYPOINT ["/app/docker-entrypoint.sh"]
