# ---- build stage: full JDK + Maven wrapper ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Dependencies first: this layer is cached until pom.xml changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package

# ---- runtime stage: JRE only ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/*.jar app.jar
USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
