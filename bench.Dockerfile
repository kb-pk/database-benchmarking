FROM maven:3-amazoncorretto-21 AS build
WORKDIR /app
COPY bench/ /app
# cache dependencies
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

FROM maven:3-amazoncorretto-21 AS runner
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
HEALTHCHECK --retries=5 CMD curl localhost:8080/healthcheck
CMD java -jar /app/app.jar
