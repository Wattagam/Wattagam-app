FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY --chmod=0755 gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew --no-daemon test bootJar

# Tests also produce a plain JAR; copy only the executable JAR to the runtime image.
RUN rm -f build/libs/*-plain.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar ./app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
