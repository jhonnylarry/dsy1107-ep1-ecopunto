FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S ecopunto && adduser -S ecopunto -G ecopunto
COPY --from=build /app/target/*.jar app.jar
USER ecopunto

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
