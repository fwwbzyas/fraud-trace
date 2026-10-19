@echo off
cd /d "%~dp0.."
REM 启动 Banks 应用的批处理脚本
REM 包含解决 Java 17 和 Spark 兼容性问题的 JVM 参数

echo ========================================
echo 启动 Banks 应用
echo ========================================

REM 设置 Java 17 模块访问参数（解决 DirectByteBuffer 问题）
set JAVA_OPTS=--add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED

REM 设置 Hadoop Home（解决 Windows 上 Spark 的问题）
set HADOOP_HOME=%TEMP%\hadoop-tmp
if not exist "%HADOOP_HOME%" mkdir "%HADOOP_HOME%"

REM 使用 Maven Wrapper 运行应用
call mvnw spring-boot:run -Dspring-boot.run.jvmArguments="%JAVA_OPTS%"

pause


