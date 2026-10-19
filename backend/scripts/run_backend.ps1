Set-Location (Join-Path $PSScriptRoot "..")
# PowerShell 启动脚本
# 启动 Banks 应用，包含解决 Java 17 和 Spark 兼容性问题的 JVM 参数

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "启动 Banks 应用" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 设置 Java 17 模块访问参数（解决 DirectByteBuffer 问题）
$javaOpts = "--add-opens=java.base/java.nio=ALL-UNNAMED " +
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED " +
            "--add-opens=java.base/java.lang=ALL-UNNAMED " +
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED " +
            "--add-opens=java.base/java.util=ALL-UNNAMED"

# 设置 Hadoop Home（解决 Windows 上 Spark 的问题）
$hadoopHome = "$env:TEMP\hadoop-tmp"
if (-not (Test-Path $hadoopHome)) {
    New-Item -ItemType Directory -Path $hadoopHome -Force | Out-Null
}
$env:HADOOP_HOME = $hadoopHome

# 使用 Maven Wrapper 运行应用
& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=$javaOpts"


