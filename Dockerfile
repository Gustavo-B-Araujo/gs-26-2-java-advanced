FROM gradle:9.9.1-jdk21 AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
RUN gradle dependencies --no-daemon -q > /dev/null
COPY src ./src
RUN gradle bootJar --no-daemon -q

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 boletim
COPY --from=build /app/build/libs/boletim-plus-*.jar app.jar
USER boletim
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
