# Stage 1: Build (Maven ile derle ve JAR oluştur)
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (Sadece JAR'ı çalıştır)
FROM eclipse-temurin:17-alpine
WORKDIR /app

# Alpine'da useradd yerine adduser/addgroup kullanılır
RUN addgroup -S appgroup && adduser -S -G appgroup appuser

# Builder stage'den JAR'ı kopyala
COPY --from=builder /app/target/stok-0.0.1-SNAPSHOT.jar app.jar

# Sahiplik değiştir
RUN chown appuser:appgroup app.jar

# appuser'a geç
USER appuser

# Port'u expose et
EXPOSE 8080

# Uygulama başlat
ENTRYPOINT ["java", "-jar", "app.jar"]
