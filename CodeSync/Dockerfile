FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -DskipTests

COPY src src

RUN ./mvnw clean package -DskipTests

EXPOSE 10000

ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "target/codesync-0.0.1-SNAPSHOT.jar"]