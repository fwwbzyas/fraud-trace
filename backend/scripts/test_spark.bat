@echo off
cd /d "%~dp0.."
echo 🚀 启动 Banks Spark 测试



echo 📋 编译项目...
call mvn compile -q

if %ERRORLEVEL% NEQ 0 (
    echo ❌ 编译失败
    pause
    exit /b 1
)

echo 🔧 运行 Spark 测试（带 JVM 参数）...
java --add-opens=java.base/java.lang=ALL-UNNAMED ^
     --add-opens=java.base/java.lang.invoke=ALL-UNNAMED ^
     --add-opens=java.base/java.lang.reflect=ALL-UNNAMED ^
     --add-opens=java.base/java.io=ALL-UNNAMED ^
     --add-opens=java.base/java.net=ALL-UNNAMED ^
     --add-opens=java.base/java.nio=ALL-UNNAMED ^
     --add-opens=java.base/java.util=ALL-UNNAMED ^
     --add-opens=java.base/java.util.concurrent=ALL-UNNAMED ^
     --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED ^
     --add-opens=java.base/sun.nio.ch=ALL-UNNAMED ^
     --add-opens=java.base/sun.nio.cs=ALL-UNNAMED ^
     --add-opens=java.base/sun.security.action=ALL-UNNAMED ^
     --add-opens=java.base/sun.util.calendar=ALL-UNNAMED ^
     --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED ^
     -cp "target/classes;%USERPROFILE%\.m2\repository\org\apache\spark\spark-core_2.12\3.4.1\*;%USERPROFILE%\.m2\repository\org\apache\spark\spark-sql_2.12\3.4.1\*;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.3\*" ^
     com.example.banks.spark.BanksSparkTest

echo.
echo ✅ 测试完成
pause
