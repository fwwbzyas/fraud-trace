# 清理 Maven 缓存脚本
# 用于清理失败的依赖下载缓存

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "清理 Maven 缓存" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$mavenRepo = "$env:USERPROFILE\.m2\repository"

# 清理 Spark 相关缓存
Write-Host "`n清理 Spark 依赖缓存..." -ForegroundColor Yellow
$sparkPaths = @(
    "$mavenRepo\org\apache\spark\spark-core_2.12\3.4.1",
    "$mavenRepo\org\apache\spark\spark-sql_2.12\3.4.1",
    "$mavenRepo\org\apache\spark\spark-core_2.12\3.4.1.pom.lastUpdated",
    "$mavenRepo\org\apache\spark\spark-sql_2.12\3.4.1.pom.lastUpdated"
)

foreach ($path in $sparkPaths) {
    if (Test-Path $path) {
        Remove-Item -Recurse -Force $path -ErrorAction SilentlyContinue
        Write-Host "已删除: $path" -ForegroundColor Green
    }
}

# 清理所有 .lastUpdated 文件（失败的下载标记）
Write-Host "`n清理所有失败的下载标记文件..." -ForegroundColor Yellow
$lastUpdatedFiles = Get-ChildItem -Path $mavenRepo -Recurse -Filter "*.lastUpdated" -ErrorAction SilentlyContinue
if ($lastUpdatedFiles) {
    $lastUpdatedFiles | Remove-Item -Force -ErrorAction SilentlyContinue
    Write-Host "已清理 $($lastUpdatedFiles.Count) 个 .lastUpdated 文件" -ForegroundColor Green
} else {
    Write-Host "未找到 .lastUpdated 文件" -ForegroundColor Gray
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "缓存清理完成！" -ForegroundColor Green
Write-Host "现在可以运行: mvn clean install -U" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan


