Set-Location $PSScriptRoot
# 安装 Python 依赖包脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "安装 Python 依赖包" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 检查 Python 是否安装
Write-Host "`n检查 Python 环境..." -ForegroundColor Yellow
try {
    $pythonVersion = python --version 2>&1
    Write-Host "✅ $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ 未找到 Python，请先安装 Python 3.7+" -ForegroundColor Red
    exit 1
}

# 检查 pip 是否安装
Write-Host "`n检查 pip..." -ForegroundColor Yellow
try {
    $pipVersion = pip --version 2>&1
    Write-Host "✅ $pipVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ 未找到 pip，请先安装 pip" -ForegroundColor Red
    exit 1
}

# 升级 pip
Write-Host "`n升级 pip..." -ForegroundColor Yellow
python -m pip install --upgrade pip

# 安装依赖包
Write-Host "`n安装依赖包..." -ForegroundColor Yellow
Write-Host "从 requirements.txt 安装..." -ForegroundColor Gray

pip install -r requirements.txt

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "✅ 依赖包安装完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "`n现在可以运行 Python 脚本了：" -ForegroundColor Yellow
Write-Host "python generate_bank_data.py" -ForegroundColor White


