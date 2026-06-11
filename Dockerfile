FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY gateway-0.1.0.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/health 2>/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
