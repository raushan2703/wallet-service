# Stage 1: Build the application
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Install tools needed for the maven wrapper
RUN apt-get update && apt-get install -y unzip dos2unix

# Copy only the files we definitely have
COPY mvnw pom.xml ./

# Fix permissions and line endings
RUN dos2unix mvnw && chmod +x mvnw

# Download dependencies (this will now work without the .mvn folder)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Stage 2: Run the application
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]