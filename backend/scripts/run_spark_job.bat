@echo off
cd /d "%~dp0.."
echo 🚀 启动 Banks Spark 任务



echo 📋 编译项目...
call mvn compile -q

if %ERRORLEVEL% NEQ 0 (
    echo ❌ 编译失败
    pause
    exit /b 1
)

echo 🔧 运行 Banks Spark 任务（带完整 JVM 参数）...

java ^
--add-opens=java.base/java.lang=ALL-UNNAMED ^
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
--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED ^
--add-opens=java.base/java.lang.ref=ALL-UNNAMED ^
--add-opens=java.base/java.nio.charset=ALL-UNNAMED ^
--add-opens=java.base/java.security=ALL-UNNAMED ^
--add-opens=java.base/javax.crypto=ALL-UNNAMED ^
-Djava.net.useSystemProxies=false ^
-Dfile.encoding=UTF-8 ^
-cp "target/classes;%USERPROFILE%\.m2\repository\org\apache\spark\spark-core_2.12\3.4.1\spark-core_2.12-3.4.1.jar;%USERPROFILE%\.m2\repository\org\apache\spark\spark-sql_2.12\3.4.1\spark-sql_2.12-3.4.1.jar;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.3\postgresql-42.7.3.jar;%USERPROFILE%\.m2\repository\org\scala-lang\scala-library\2.12.17\scala-library-2.12.17.jar" ^
com.example.banks.spark.BanksMainJob

echo.
echo ✅ 任务完成
pause
