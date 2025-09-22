FROM openjdk:17-jdk-slim

# JAR 파일 복사 및 실행
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} product-0.0.1.jar
ENTRYPOINT ["java","-jar","/product-0.0.1.jar"]