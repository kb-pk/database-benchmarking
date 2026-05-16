FROM maven:3-amazoncorretto-21 AS build
WORKDIR /app
# cache dependencies
COPY bench/pom.xml /app/pom.xml
RUN mvn dependency:go-offline -B
COPY bench/src /app/src
RUN mvn clean package -DskipTests

FROM maven:3-amazoncorretto-21 AS runner
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
HEALTHCHECK --retries=5 CMD curl localhost:8080/healthcheck
CMD java -jar /app/app.jar
