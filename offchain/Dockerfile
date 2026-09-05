FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline || true
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --uid 10001 --create-home rwashift
COPY --from=build /workspace/target/rwashift-platform.jar app.jar
# Named volumes (documents-data, jwt-signing-key in docker-compose.yml) mount over these paths at
# runtime; Docker seeds a brand-new volume's ownership from whatever's baked into the image at
# that path, so this chown is what makes the non-root `rwashift` user able to write into them.
RUN mkdir -p /app/data/documents /app/data/jwt-signing-key-dir && chown -R rwashift:rwashift /app/data
USER rwashift
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
