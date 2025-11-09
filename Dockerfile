#
# Dependency build stage
# common-library dependency will be built and installed into the local repo in the container
#
FROM maven:3.9.9-eclipse-temurin-21 AS common_builder
WORKDIR /app/common-library
COPY common-library/pom.xml .
COPY common-library/src ./src
RUN mvn clean install -DskipTests



#
# Build stage
#
FROM maven:3.9.9-eclipse-temurin-21 AS user_builder
WORKDIR /app/user-service
COPY user-service/pom.xml .
COPY user-service/src ./src
COPY --from=common_builder /root/.m2 /root/.m2
RUN mvn clean package -DskipTests



#
# Package stage
#
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=user_builder /app/user-service/target/*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]