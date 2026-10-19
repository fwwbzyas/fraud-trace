# ============================================================
# Spring Boot 后端 Dockerfile
# ============================================================

# 第一阶段：编译
FROM maven:3.8.8-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 和源码
COPY pom.xml .
COPY src ./src

# 编译项目（跳过测试）
RUN mvn clean package -DskipTests

# 第二阶段：运行
FROM eclipse-temurin:17-jre

WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/target/banks-*.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 参数
ENV JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 启动应用
ENTRYPOINT java $JAVA_OPTS -jar app.jar
