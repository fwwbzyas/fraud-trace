@echo off
REM 清理 Maven 缓存脚本

echo ========================================
echo 清理 Maven 缓存
echo ========================================

set MAVEN_REPO=%USERPROFILE%\.m2\repository

echo.
echo 清理 Spark 依赖缓存...
if exist "%MAVEN_REPO%\org\apache\spark\spark-core_2.12\3.4.1" (
    rmdir /s /q "%MAVEN_REPO%\org\apache\spark\spark-core_2.12\3.4.1"
    echo 已删除 spark-core_2.12\3.4.1
)

if exist "%MAVEN_REPO%\org\apache\spark\spark-sql_2.12\3.4.1" (
    rmdir /s /q "%MAVEN_REPO%\org\apache\spark\spark-sql_2.12\3.4.1"
    echo 已删除 spark-sql_2.12\3.4.1
)

echo.
echo 清理失败的下载标记文件...
for /r "%MAVEN_REPO%" %%f in (*.lastUpdated) do (
    del /f /q "%%f" 2>nul
    echo 已删除: %%f
)

echo.
echo ========================================
echo 缓存清理完成！
echo 现在可以运行: mvn clean install -U
echo ========================================
pause


