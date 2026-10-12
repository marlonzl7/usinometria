# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache de dependências: só invalida quando o pom.xml muda
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Testes ficam de fora: o teste de contexto precisa de banco
RUN mvn -B -q package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]