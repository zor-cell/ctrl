FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /backend

# download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# rebuild source code
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /backend

COPY --from=build /backend/target/*.jar backend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "backend.jar"]