FROM eclipse-temurin:22-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:22-jre-jammy
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/build/libs/*SNAPSHOT.jar /app/app.jar
USER app
ENV TZ=Asia/Seoul
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55 -XX:InitialRAMPercentage=20 -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Seoul"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
