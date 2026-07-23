FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY lunchorder-bootstrap/target/lunchorder-bootstrap-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-Dspring.profiles.active=prod", "-jar", "app.jar"]