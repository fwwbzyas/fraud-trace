# ========================================
# 银行反欺诈系统 - Windows自动部署脚本
# ========================================

param(
    [string]$Mode = "dev",  # dev/prod
    [switch]$SkipBuild = $false,
    [switch]$OnlyBackend = $false,
    [switch]$OnlyFrontend = $false
)

$ErrorActionPreference = "Stop"

# 颜色输出函数
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Write-Success { Write-ColorOutput Green $args }
function Write-Info { Write-ColorOutput Cyan $args }
function Write-Warning { Write-ColorOutput Yellow $args }
function Write-Error { Write-ColorOutput Red $args }

Write-Info "================================================"
Write-Info "  银行反欺诈系统部署脚本"
Write-Info "  部署模式: $Mode"
Write-Info "================================================"
Write-Info ""

# 配置
$BACKEND_DIR = Join-Path $PSScriptRoot ".."
$FRONTEND_DIR = Join-Path $PSScriptRoot "../../frontend"
$BACKEND_PORT = 8080
$FRONTEND_PORT = if ($Mode -eq "prod") { 3000 } else { 5173 }

# ========================================
# 1. 环境检查
# ========================================
Write-Info "步骤1: 检查部署环境..."

# 检查Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_ -replace '.*"(.*)".*', '$1' }
    if ($javaVersion -match "^(1[7-9]|[2-9]\d)") {
        Write-Success "✓ Java版本: $javaVersion"
    } else {
        throw "Java版本过低，需要17+"
    }
} catch {
    Write-Error "✗ Java未安装或版本不符合要求（需要Java 17+）"
    exit 1
}

# 检查Maven
try {
    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven" | ForEach-Object { $_ -replace 'Apache Maven ([\d.]+).*', '$1' }
    Write-Success "✓ Maven版本: $mavenVersion"
} catch {
    Write-Error "✗ Maven未安装"
    exit 1
}

# 检查Node.js（仅前端）
if (-not $OnlyBackend) {
    try {
        $nodeVersion = node -v
        $npmVersion = npm -v
        Write-Success "✓ Node.js版本: $nodeVersion"
        Write-Success "✓ npm版本: $npmVersion"
    } catch {
        Write-Error "✗ Node.js未安装"
        exit 1
    }
}

# 检查PostgreSQL
try {
    $pgService = Get-Service postgresql* -ErrorAction SilentlyContinue
    if ($pgService -and $pgService.Status -eq "Running") {
        Write-Success "✓ PostgreSQL服务运行中"
    } else {
        Write-Warning "⚠ PostgreSQL服务未运行"
    }
} catch {
    Write-Warning "⚠ 无法检查PostgreSQL状态"
}

Write-Info ""

# ========================================
# 2. 停止旧服务
# ========================================
Write-Info "步骤2: 停止旧服务..."

if (-not $OnlyFrontend) {
    # 停止后端Java进程
    $javaProcesses = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*banks*" }
    if ($javaProcesses) {
        $javaProcesses | ForEach-Object {
            Stop-Process -Id $_.Id -Force
            Write-Success "✓ 已停止后端进程 (PID: $($_.Id))"
        }
    }
}

if (-not $OnlyBackend) {
    # 停止前端Node进程
    $nodeProcesses = Get-Process node -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*vite*" -or $_.CommandLine -like "*serve*" }
    if ($nodeProcesses) {
        $nodeProcesses | ForEach-Object {
            Stop-Process -Id $_.Id -Force
            Write-Success "✓ 已停止前端进程 (PID: $($_.Id))"
        }
    }
}

Start-Sleep -Seconds 2
Write-Info ""

# ========================================
# 3. 构建后端
# ========================================
if (-not $OnlyFrontend) {
    Write-Info "步骤3: 构建后端应用..."
    
    cd $BACKEND_DIR
    
    if (-not $SkipBuild) {
        Write-Info "执行Maven构建（跳过测试）..."
        mvn clean package -DskipTests
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "✓ 后端构建成功"
        } else {
            Write-Error "✗ 后端构建失败"
            exit 1
        }
    } else {
        Write-Warning "⊙ 跳过后端构建"
    }
    
    Write-Info ""
}

# ========================================
# 4. 构建前端
# ========================================
if (-not $OnlyBackend) {
    Write-Info "步骤4: 构建前端应用..."
    
    cd $FRONTEND_DIR
    
    if (-not $SkipBuild) {
        # 检查node_modules
        if (-not (Test-Path "node_modules")) {
            Write-Info "首次部署，安装依赖..."
            npm install
        }
        
        if ($Mode -eq "prod") {
            Write-Info "执行生产构建..."
            npm run build
            
            if ($LASTEXITCODE -eq 0) {
                Write-Success "✓ 前端构建成功"
            } else {
                Write-Error "✗ 前端构建失败"
                exit 1
            }
        } else {
            Write-Info "开发模式，跳过构建"
        }
    } else {
        Write-Warning "⊙ 跳过前端构建"
    }
    
    Write-Info ""
}

# ========================================
# 5. 启动后端服务
# ========================================
if (-not $OnlyFrontend) {
    Write-Info "步骤5: 启动后端服务..."
    
    cd $BACKEND_DIR
    
    $jarFile = "target\banks-0.0.1-SNAPSHOT.jar"
    
    if (Test-Path $jarFile) {
        # 创建日志目录
        if (-not (Test-Path "logs")) {
            New-Item -ItemType Directory -Path "logs" | Out-Null
        }
        
        # 启动后端（后台运行）
        $processInfo = New-Object System.Diagnostics.ProcessStartInfo
        $processInfo.FileName = "java"
        $processInfo.Arguments = "-jar $jarFile"
        $processInfo.RedirectStandardOutput = $true
        $processInfo.RedirectStandardError = $true
        $processInfo.UseShellExecute = $false
        $processInfo.CreateNoWindow = $true
        $processInfo.WorkingDirectory = $BACKEND_DIR
        
        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $processInfo
        $process.Start() | Out-Null
        
        Write-Success "✓ 后端服务已启动 (PID: $($process.Id))"
        Write-Info "  日志文件: logs\spring.log"
        Write-Info "  访问地址: http://localhost:$BACKEND_PORT/api"
        
        # 等待服务启动
        Write-Info "  等待服务启动..."
        Start-Sleep -Seconds 5
        
        # 健康检查
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$BACKEND_PORT/api/actuator/health" -UseBasicParsing -TimeoutSec 10
            if ($response.StatusCode -eq 200) {
                Write-Success "  ✓ 后端服务健康检查通过"
            }
        } catch {
            Write-Warning "  ⚠ 后端服务可能未完全启动，请稍后检查"
        }
    } else {
        Write-Error "✗ JAR文件不存在: $jarFile"
        exit 1
    }
    
    Write-Info ""
}

# ========================================
# 6. 启动前端服务
# ========================================
if (-not $OnlyBackend) {
    Write-Info "步骤6: 启动前端服务..."
    
    cd $FRONTEND_DIR
    
    if ($Mode -eq "prod") {
        # 生产模式：使用serve
        Write-Info "使用serve启动生产环境..."
        
        $processInfo = New-Object System.Diagnostics.ProcessStartInfo
        $processInfo.FileName = "npx"
        $processInfo.Arguments = "serve -s dist -p $FRONTEND_PORT"
        $processInfo.RedirectStandardOutput = $true
        $processInfo.RedirectStandardError = $true
        $processInfo.UseShellExecute = $false
        $processInfo.CreateNoWindow = $true
        $processInfo.WorkingDirectory = $FRONTEND_DIR
        
        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $processInfo
        $process.Start() | Out-Null
        
        Write-Success "✓ 前端服务已启动 (PID: $($process.Id))"
    } else {
        # 开发模式：使用vite
        Write-Info "使用Vite启动开发环境..."
        
        $processInfo = New-Object System.Diagnostics.ProcessStartInfo
        $processInfo.FileName = "npm"
        $processInfo.Arguments = "run dev"
        $processInfo.RedirectStandardOutput = $true
        $processInfo.RedirectStandardError = $true
        $processInfo.UseShellExecute = $false
        $processInfo.CreateNoWindow = $true
        $processInfo.WorkingDirectory = $FRONTEND_DIR
        
        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $processInfo
        $process.Start() | Out-Null
        
        Write-Success "✓ 前端服务已启动 (PID: $($process.Id))"
    }
    
    Write-Info "  访问地址: http://localhost:$FRONTEND_PORT"
    Write-Info ""
}

# ========================================
# 7. 部署完成
# ========================================
Write-Success "================================================"
Write-Success "  🎉 部署完成！"
Write-Success "================================================"
Write-Info ""
Write-Info "服务访问地址："

if (-not $OnlyFrontend) {
    Write-Info "  后端API:    http://localhost:$BACKEND_PORT/api"
    Write-Info "  健康检查:   http://localhost:$BACKEND_PORT/api/actuator/health"
    Write-Info "  压测接口:   http://localhost:$BACKEND_PORT/api/mock/tasks/health"
}

if (-not $OnlyBackend) {
    Write-Info "  前端页面:   http://localhost:$FRONTEND_PORT"
}

Write-Info ""
Write-Info "查看运行进程："
Write-Info "  Get-Process java,node | Where-Object {`$_.CommandLine -like '*banks*' -or `$_.CommandLine -like '*vite*'}"
Write-Info ""
Write-Info "停止所有服务："
Write-Info "  .\deploy_windows.ps1 -Mode stop"
Write-Info ""

# 保存进程信息
$processInfo = @{
    Backend = if (-not $OnlyFrontend) { Get-Process java | Where-Object { $_.CommandLine -like "*banks*" } | Select-Object Id, ProcessName } else { $null }
    Frontend = if (-not $OnlyBackend) { Get-Process node | Where-Object { $_.CommandLine -like "*vite*" -or $_.CommandLine -like "*serve*" } | Select-Object Id, ProcessName } else { $null }
    DeployTime = Get-Date
    Mode = $Mode
}

$processInfo | ConvertTo-Json | Out-File -FilePath "$BACKEND_DIR\deploy_info.json" -Encoding UTF8

Write-Success "部署信息已保存到: deploy_info.json"





