FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY --from=a6-deps . /root/.m2/repository/com/angorasix
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
COPY detekt.yml .
COPY .editorconfig .
# RUN --mount=type=cache,target=/root/.m2 ./mvnw install -DskipTests
RUN ./mvnw install -DskipTests -Dcheckstyle.skip
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:21-jre
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]