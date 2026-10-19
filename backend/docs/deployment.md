> 历史部署参考：当前配置、密码设置和可复现启动步骤以根目录 README.md 为准。本文含旧环境示例，不保证可以直接照抄执行。

# 银行反欺诈分析系统 - 部署文档

## 📋 目录
1. [部署环境要求](#部署环境要求)
   - 1.1 [硬件要求](#硬件要求)
   - 1.2 [软件要求](#软件要求)
2. [部署操作步骤](#部署操作步骤)
   - 2.1 [数据库部署](#数据库部署)
   - 2.2 [后端服务部署](#后端服务部署)
   - 2.3 [前端应用部署](#前端应用部署)
   - 2.4 [ClickHouse部署（可选）](#clickhouse部署)
3. [部署后环境验证](#部署后环境验证)
4. [常见问题排查](#常见问题排查)

---

## 1. 部署环境要求

### 1.1 硬件要求

#### 最低配置（测试/开发环境）
| 组件 | 配置要求 | 说明 |
|------|---------|------|
| **CPU** | 4核心 | Intel i5 或同等性能 |
| **内存** | 8GB RAM | 最低要求 |
| **硬盘** | 50GB 可用空间 | SSD 推荐 |
| **网络** | 100Mbps | 内网带宽 |

#### 推荐配置（生产环境）
| 组件 | 配置要求 | 说明 |
|------|---------|------|
| **CPU** | 8核心+ | Intel Xeon 或同等性能 |
| **内存** | 16GB+ RAM | 32GB 推荐 |
| **硬盘** | 500GB+ SSD | 数据库和日志存储 |
| **网络** | 1Gbps+ | 高速内网环境 |

#### 集群部署（高可用）
| 服务器类型 | 数量 | 配置 | 用途 |
|-----------|------|------|------|
| **应用服务器** | 2-3台 | 8核/16GB | 负载均衡 |
| **数据库服务器** | 1-2台 | 16核/32GB/SSD | PostgreSQL主从 |
| **HDFS NameNode** | 2台 | 16核/32GB/SSD | HDFS主节点（HA）|
| **HDFS DataNode** | 3-5台 | 8核/16GB/4TB | HDFS数据节点 |
| **ZooKeeper** | 3台 | 4核/8GB | 分布式协调（奇数台）|
| **Spark Master** | 2台 | 8核/16GB | Spark集群管理（HA）|
| **Spark Worker** | 3-5台 | 16核/32GB | Spark计算节点 |
| **ClickHouse服务器** | 1-3台 | 16核/32GB/SSD | 实时分析（可选）|

**大数据集群配置说明：**
- **HDFS NameNode**：2台主备模式，使用ZooKeeper实现高可用
- **HDFS DataNode**：建议至少3台，数据副本数设置为3
- **ZooKeeper**：必须是奇数台（3/5/7台），用于集群协调和选主
- **Spark Master**：2台主备模式，通过ZooKeeper实现自动故障转移
- **Spark Worker**：可与DataNode部署在同一服务器，实现数据本地性
- 数据量大时可增加DataNode和Worker节点数量，实现横向扩展

---

### 1.2 软件要求

#### 操作系统
- ✅ **Windows 10/11** (开发/测试)
- ✅ **Windows Server 2019/2022** (生产)
- ✅ **Linux** (Ubuntu 20.04+, CentOS 7+)
- ✅ **macOS** (开发环境)

#### 必需软件清单

| 软件名称 | 版本要求 | 用途 | 下载地址 |
|---------|---------|------|---------|
| **Java JDK** | 17+ | 后端运行环境 | https://www.oracle.com/java/technologies/downloads/ |
| **PostgreSQL** | 14+ | 主数据库 | https://www.postgresql.org/download/ |
| **Apache Hadoop** | 3.3+ | 分布式文件系统（HDFS）| https://hadoop.apache.org/releases.html |
| **Apache Spark** | 3.5.0 | 大数据分析引擎（集群部署）| https://spark.apache.org/downloads.html |
| **Apache ZooKeeper** | 3.8+ | 分布式协调服务（高可用）| https://zookeeper.apache.org/releases.html |
| **Node.js** | 18+ | 前端构建/运行 | https://nodejs.org/ |
| **Maven** | 3.8+ | 后端构建工具 | https://maven.apache.org/download.cgi |
| **Git** | 2.x | 版本控制 | https://git-scm.com/downloads |

**大数据组件说明：**
- **Hadoop/HDFS**：用于存储海量历史数据和分析结果，支持PB级数据存储
- **Spark**：下载预编译版本（Pre-built for Apache Hadoop 3.3），需要配置SSH免密登录
- **ZooKeeper**：实现Spark Master高可用（HA），避免单点故障，生产环境强烈推荐

#### 可选软件（生产环境推荐）

| 软件名称 | 版本要求 | 用途 | 下载地址 |
|---------|---------|------|---------|
| **ClickHouse** | 23+ | 实时分析引擎 | https://clickhouse.com/docs/en/install |
| **Redis** | 6+ | 缓存服务 | https://redis.io/download |
| **Nginx** | 1.20+ | 反向代理 | https://nginx.org/en/download.html |

#### 浏览器要求
- ✅ Chrome 90+ (推荐)
- ✅ Edge 90+
- ✅ Firefox 88+
- ✅ Safari 14+

---

## 2. 部署操作步骤

### 2.1 数据库部署

#### 步骤1：安装PostgreSQL数据库

**Windows环境操作说明：**
1. 访问PostgreSQL官网下载页面，下载PostgreSQL 14或更高版本的Windows安装包
2. 双击运行安装程序，按照安装向导进行安装
3. 在安装过程中设置以下参数：
   - 监听端口：保持默认的5432端口
   - 超级用户密码：设置一个强密码并妥善保管（后续需要使用）
   - 字符集编码：选择UTF-8编码
   - 安装组件：勾选PostgreSQL Server、pgAdmin 4、Command Line Tools
4. 完成安装后，PostgreSQL服务会自动启动

**Linux环境操作说明：**
1. 打开终端，更新系统软件包列表
2. 对于Ubuntu或Debian系统，使用apt包管理器安装postgresql和postgresql-contrib
3. 对于CentOS或RHEL系统，使用yum包管理器安装postgresql-server和postgresql-contrib
4. 初始化数据库集群
5. 启动PostgreSQL服务并设置为开机自启动
6. 验证PostgreSQL服务状态确认运行正常

#### 步骤2：创建数据库和用户

1. 使用postgres超级用户身份连接到PostgreSQL数据库服务器
2. 在数据库命令行界面中执行以下操作：
   - 创建名为"banks"的数据库
   - 创建名为"banks_user"的数据库用户，并设置安全密码
   - 将banks数据库的所有权限授予banks_user用户
3. 退出数据库命令行界面

**具体操作内容：**
- 首先创建数据库：数据库名称设为banks
- 然后创建用户：用户名设为banks_user，密码需要设置为强密码
- 最后授权：将banks数据库的完全控制权限授予banks_user用户
- 完成后退出数据库连接

#### 步骤3：导入数据库结构

1. 打开命令行工具（Windows使用PowerShell或CMD，Linux使用终端）
2. 切换到项目目录，路径为：C:\Users\Lenovo\IdeaProjects\banks（Windows）或项目所在路径（Linux）
3. 使用psql命令行工具连接数据库并导入表结构：
   - 指定用户为postgres
   - 指定数据库为banks
   - 指定导入文件为backend/sql/schema.sql
4. 等待导入完成，观察控制台输出确认无错误信息
5. 导入成功后会创建以下数据表：
   - account（账户信息表）
   - login_record（登录记录表）
   - transfer_records（转账记录表）
   - analysis_indicators（分析指标配置表）
   - analysis_tasks（分析任务表）
   - risk_results（风险分析结果表）

#### 步骤4：配置数据库连接信息

1. 使用文本编辑器打开项目配置文件，路径为：src/main/resources/application.properties
2. 找到数据库配置部分，修改以下配置项：
   - 数据库连接地址：设置为jdbc:postgresql://localhost:5432/banks
   - 数据库用户名：设置为banks_user
   - 数据库密码：设置为步骤2中创建用户时的密码
   - 数据库驱动：确认为org.postgresql.Driver
3. 找到JPA配置部分，确认以下设置：
   - DDL自动生成模式：设置为none（不自动修改表结构）
   - SQL语句显示：设置为false（生产环境不显示SQL）
   - 数据库方言：设置为PostgreSQLDialect
4. 保存配置文件

---

### 2.2 后端服务部署

#### 步骤1：检查Java运行环境

1. 打开命令行工具（PowerShell、CMD或终端）
2. 输入java -version命令检查Java版本
3. 确认Java版本为17或更高版本，版本信息会显示类似"java version 17.0.x"或"openjdk version 17.0.x"
4. 输入mvn -version命令检查Maven版本
5. 确认Maven版本为3.8或更高版本，版本信息会显示"Apache Maven 3.x.x"
6. 如果版本不符合要求，需要先升级Java和Maven到所需版本
7. 如果命令未找到，说明环境变量未配置，需要添加Java和Maven到系统PATH环境变量

#### 步骤2：构建后端应用程序

**Windows环境操作：**
1. 打开PowerShell或命令提示符
2. 使用cd命令切换到项目目录：C:\Users\Lenovo\IdeaProjects\banks
3. 执行Maven清理和打包命令：mvn clean package -DskipTests
   - clean：清理之前的构建产物
   - package：打包项目为可执行jar文件
   - -DskipTests：跳过单元测试以加快构建速度
4. 等待Maven下载依赖包并完成编译，这个过程可能需要几分钟（首次构建会下载所有依赖）
5. 观察控制台输出，确认显示"BUILD SUCCESS"表示构建成功
6. 构建成功后，在target目录下会生成可执行文件：banks-0.0.1-SNAPSHOT.jar

**Linux环境操作：**
1. 打开终端
2. 使用cd命令切换到项目目录
3. 如果项目包含mvnw脚本，执行：./mvnw clean package -DskipTests
4. 如果系统已安装Maven，也可以执行：mvn clean package -DskipTests
5. 等待构建完成并确认成功信息
6. 在target目录下查看生成的jar文件

#### 步骤3：部署ZooKeeper集群（高可用必需）

**重要说明：** ZooKeeper用于实现Spark Master和HDFS NameNode的高可用，必须先部署。

**步骤3.1：准备ZooKeeper服务器**
1. 准备3台服务器（必须是奇数台）
2. 推荐配置：每台4核CPU、8GB内存、100GB硬盘
3. 确保所有节点网络互通，开放端口2181、2888、3888
4. 所有节点安装Java 11

**步骤3.2：安装ZooKeeper**
1. 在所有ZooKeeper节点下载并解压ZooKeeper 3.8.x
2. 创建数据目录：mkdir -p /data/zookeeper
3. 编辑配置文件zoo.cfg，添加集群配置：
   ```
   tickTime=2000
   initLimit=10
   syncLimit=5
   dataDir=/data/zookeeper
   clientPort=2181
   server.1=zk1:2888:3888
   server.2=zk2:2888:3888
   server.3=zk3:2888:3888
   ```
4. 在每个节点的数据目录创建myid文件：
   - 节点1：echo "1" > /data/zookeeper/myid
   - 节点2：echo "2" > /data/zookeeper/myid
   - 节点3：echo "3" > /data/zookeeper/myid

**步骤3.3：启动ZooKeeper集群**
1. 在所有节点执行：bin/zkServer.sh start
2. 检查状态：bin/zkServer.sh status
3. 确认1个Leader和2个Follower

---

#### 步骤4：部署HDFS集群（数据存储）

**重要说明：** HDFS用于存储海量历史数据和分析结果，支持PB级数据。

**步骤4.1：准备Hadoop集群服务器**
1. 准备至少5台服务器（2台NameNode + 3台DataNode）
2. NameNode配置：16核CPU、32GB内存、1TB SSD
3. DataNode配置：8核CPU、16GB内存、4TB硬盘
4. 所有节点安装Java 11，配置SSH免密登录

**步骤4.2：安装Hadoop**
1. 下载Hadoop 3.3.x并解压到所有节点
2. 设置环境变量HADOOP_HOME
3. 编辑core-site.xml，配置ZooKeeper地址：
   ```xml
   <property>
     <name>fs.defaultFS</name>
     <value>hdfs://mycluster</value>
   </property>
   <property>
     <name>ha.zookeeper.quorum</name>
     <value>zk1:2181,zk2:2181,zk3:2181</value>
   </property>
   ```

**步骤4.3：配置HDFS高可用**
1. 编辑hdfs-site.xml，配置NameNode HA：
   - 设置nameservice ID为mycluster
   - 配置2个NameNode（nn1和nn2）
   - 启用自动故障转移
2. 在两个NameNode节点分别启动JournalNode
3. 在主NameNode执行格式化：hdfs namenode -format
4. 在主NameNode执行：hdfs zkfc -formatZK

**步骤4.4：启动HDFS集群**
1. 启动JournalNode：hadoop-daemon.sh start journalnode
2. 启动主NameNode：hadoop-daemon.sh start namenode
3. 同步备NameNode：hdfs namenode -bootstrapStandby
4. 启动所有DataNode：hadoop-daemon.sh start datanode
5. 启动ZKFC（自动故障转移）：hadoop-daemon.sh start zkfc
6. 访问Web界面验证：http://namenode1:9870

---

#### 步骤5：部署Spark集群环境

**重要说明：** 本系统使用Spark集群进行大数据分析，依赖HDFS和ZooKeeper。

**Spark集群部署步骤：**

**步骤5.1：准备Spark集群服务器**
1. 准备至少3台服务器（1个Master节点 + 2个Worker节点）
2. 推荐配置：每台8核CPU、16GB内存、500GB硬盘
3. 确保所有节点网络互通，防火墙开放相关端口（7077、8080、4040等）
4. 所有节点安装Java 8或Java 11（Spark 3.5.0推荐使用Java 11）
5. 配置所有节点之间的SSH免密登录

**步骤3.2：下载并安装Spark**
1. 访问Apache Spark官网下载页面
2. 下载Spark 3.5.0预编译版本（选择Hadoop 3.3版本）
3. 解压到所有节点的相同目录，例如：/opt/spark
4. 设置SPARK_HOME环境变量指向Spark安装目录
5. 将$SPARK_HOME/bin添加到PATH环境变量

**步骤3.3：配置Spark Master节点**
1. 在Master节点编辑配置文件：$SPARK_HOME/conf/spark-env.sh
2. 添加以下配置：
   - SPARK_MASTER_HOST：设置为Master节点的IP地址
   - SPARK_MASTER_PORT：设置为7077（默认端口）
   - SPARK_MASTER_WEBUI_PORT：设置为8080（Web界面端口）
   - SPARK_WORKER_CORES：每个Worker的CPU核心数
   - SPARK_WORKER_MEMORY：每个Worker的内存大小（如8g）
3. 创建workers配置文件：$SPARK_HOME/conf/workers
4. 在workers文件中添加所有Worker节点的主机名或IP地址（每行一个）

**步骤3.4：启动Spark集群**
1. 在Master节点执行启动命令：$SPARK_HOME/sbin/start-master.sh
2. 查看Master启动日志，确认启动成功
3. 在Master节点执行启动所有Worker命令：$SPARK_HOME/sbin/start-workers.sh
4. 或在每个Worker节点单独启动：$SPARK_HOME/sbin/start-worker.sh spark://master-ip:7077
5. 访问Spark Master Web界面：http://master-ip:8080
6. 确认所有Worker节点已成功注册并显示为Alive状态

**步骤3.5：配置应用程序连接Spark集群**
1. 打开后端项目配置文件：src/main/resources/application.properties
2. 找到Spark配置部分，修改以下配置项：
   - spark.master：设置为spark://master-ip:7077（Spark Master地址）
   - spark.app.name：设置为BankFraudAnalysis
   - spark.driver.memory：设置为4g（根据应用服务器配置调整）
   - spark.executor.memory：设置为8g（根据Worker节点配置调整）
   - spark.executor.cores：设置为4（每个Executor使用的CPU核心数）
   - spark.executor.instances：设置为2（Executor实例数量）
3. 保存配置文件

**配置示例：**
```properties
# Spark集群配置
spark.master=spark://192.168.1.100:7077
spark.app.name=BankFraudAnalysis
spark.driver.memory=4g
spark.executor.memory=8g
spark.executor.cores=4
spark.executor.instances=2
spark.sql.shuffle.partitions=200
```

**步骤3.6：测试Spark集群连接**
1. 在应用服务器上运行Spark示例程序测试连接
2. 执行命令：$SPARK_HOME/bin/spark-submit --master spark://master-ip:7077 --class org.apache.spark.examples.SparkPi $SPARK_HOME/examples/jars/spark-examples_*.jar 10
3. 如果能正常提交任务并返回结果，说明集群连接正常
4. 访问Spark Master Web界面，查看任务执行记录

#### 步骤4：启动后端服务

**开发调试模式（前台运行）：**
1. 在项目目录下打开命令行工具
2. 执行java -jar target/banks-0.0.1-SNAPSHOT.jar命令启动服务
3. 或者使用Maven命令：mvn spring-boot:run
4. 观察控制台输出，等待显示"Started BanksApplication"表示启动成功
5. 默认服务监听在8080端口，可通过http://localhost:8080访问
6. 按Ctrl+C可以停止服务

**生产环境模式（后台运行）：**

**Windows系统：**
方式一：使用NSSM创建Windows服务
1. 下载NSSM（Non-Sucking Service Manager）工具
2. 打开命令行，执行安装服务命令
3. 设置服务名称为BanksFraudAPI
4. 设置Java可执行文件路径（通常在C:\Program Files\Java\jdk-17\bin\java.exe）
5. 设置应用程序参数：-jar和jar文件的完整路径
6. 启动NSSM服务
7. 将服务设置为开机自启动

方式二：使用PowerShell后台运行
1. 打开PowerShell
2. 使用Start-Process命令启动Java进程
3. 设置参数为jar文件路径
4. 设置窗口样式为Hidden（隐藏窗口）
5. 进程将在后台运行，不阻塞终端

**Linux系统：**
1. 使用文本编辑器创建systemd服务文件：/etc/systemd/system/banks-api.service
2. 编写服务配置内容：
   - Unit部分：描述服务信息，设置在postgresql服务之后启动
   - Service部分：
     * 服务类型设为simple
     * 运行用户设为appuser（需提前创建）
     * 工作目录设为/opt/banks
     * 启动命令设为java -jar加jar文件路径
     * 设置自动重启策略，失败后10秒重启
   - Install部分：设置为多用户模式下启动
3. 保存服务文件
4. 重新加载systemd配置：sudo systemctl daemon-reload
5. 启动服务：sudo systemctl start banks-api
6. 设置开机自启：sudo systemctl enable banks-api
7. 查看服务状态：sudo systemctl status banks-api

#### 步骤5：验证后端服务运行状态

1. 等待服务完全启动（通常需要10-30秒）
2. 打开浏览器或使用curl工具访问健康检查接口
3. 访问地址：http://localhost:8080/api/actuator/health
4. 正常情况下应返回JSON格式的响应：{"status":"UP"}
5. 如果返回UP状态，说明后端服务已成功启动并运行正常
6. 如果无法访问或返回错误，需要检查：
   - 服务是否真正启动（查看进程列表）
   - 端口是否被占用（检查8080端口）
   - 数据库连接是否正常（查看日志文件）
   - 防火墙是否阻止了访问

---

### 2.3 前端应用部署

#### 步骤1：安装和检查Node.js环境

1. 打开命令行工具（PowerShell、CMD或终端）
2. 输入node -version命令检查Node.js版本
3. 确认Node.js版本为18或更高版本，版本信息会显示类似"v18.x.x"或"v20.x.x"
4. 输入npm -version命令检查npm版本
5. npm版本应随Node.js自动安装，通常为8.x或9.x版本
6. 如果Node.js版本不符合要求，需要访问Node.js官网下载并安装最新的LTS版本
7. 如果命令未找到，说明Node.js未安装或环境变量未配置

#### 步骤2：安装前端项目依赖

1. 打开命令行工具
2. 使用cd命令切换到前端项目目录：C:\Users\Lenovo\Desktop\bankvue
3. 执行npm install命令安装所有依赖包
4. 如果网络速度慢或连接超时，可以先配置使用国内镜像源：
   - 执行命令：npm config set registry https://registry.npmmirror.com
   - 然后再执行npm install
5. 等待依赖安装完成，这个过程可能需要几分钟（首次安装会下载所有依赖）
6. 安装完成后，项目目录下会生成node_modules文件夹，包含所有依赖包
7. 如果出现错误，尝试删除node_modules和package-lock.json后重新安装

#### 步骤3：配置后端API地址

1. 使用文本编辑器打开文件：bankvue/src/api/request.js
2. 找到API基础地址配置部分（通常在文件开头）
3. 对于开发环境，设置baseURL为：http://localhost:8080/api
4. 对于生产环境，需要修改为实际的服务器地址，例如：http://192.168.1.100:8080/api
5. 如果部署在同一台服务器上，可以使用相对路径：/api
6. 保存文件
7. 确认修改后的地址与后端服务地址一致

#### 步骤4：构建前端应用

**开发调试模式（支持热重载）：**
1. 在前端项目目录下打开命令行工具
2. 执行npm run dev命令启动开发服务器
3. 等待Vite开发服务器启动，通常几秒钟即可完成
4. 启动成功后，控制台会显示访问地址，通常为：http://localhost:5173
5. 开发模式支持热重载，修改代码后浏览器会自动刷新
6. 适合开发和调试使用
7. 按Ctrl+C可以停止开发服务器

**生产部署模式（构建静态文件）：**
1. 在前端项目目录下打开命令行工具
2. 执行npm run build命令进行生产构建
3. Vite会对代码进行优化、压缩和打包，这个过程通常需要1-2分钟
4. 构建成功后，控制台会显示"built in Xs"表示构建完成
5. 构建产物会生成在dist目录下，包含所有静态文件：
   - index.html（入口页面）
   - assets目录（包含所有JS、CSS和图片资源）
6. dist目录的内容即为可直接部署的前端应用
7. 可将dist目录内容部署到任何Web服务器（Nginx、Apache、IIS等）

#### 步骤5：部署前端静态文件

**方式1：使用Node.js轻量服务器（适合测试）**
1. 全局安装serve工具，执行命令：npm install -g serve
2. 在前端项目目录下，执行命令：serve -s dist -p 3000
   - -s 参数表示以单页应用模式运行
   - dist 是静态文件目录
   - -p 3000 指定端口为3000
3. 启动成功后，访问地址：http://localhost:3000
4. 这种方式适合测试和临时使用，不推荐生产环境使用
5. 服务会一直运行直到按Ctrl+C停止

**方式2：使用Nginx部署（推荐生产环境）**
1. 安装Nginx Web服务器（Windows下载exe版本，Linux使用包管理器安装）
2. 找到Nginx配置文件目录（Windows通常在nginx/conf/，Linux在/etc/nginx/）
3. 创建或编辑站点配置文件nginx.conf
4. 配置服务器块（server block）：
   - 监听端口设置为80（HTTP默认端口）
   - 服务器名称设置为域名或IP地址
   - 网站根目录设置为dist文件夹的绝对路径（例如C:/apps/bankvue/dist）
   - 默认首页设置为index.html
5. 配置URL重写规则（location /）：
   - 设置try_files规则，使所有请求都指向index.html
   - 这样可以支持Vue Router的History模式
6. 配置API代理（location /api/）：
   - 将/api/开头的请求代理到后端服务（http://localhost:8080/api/）
   - 设置必要的HTTP头部（Host、X-Real-IP）
   - 这样可以避免跨域问题
7. 保存配置文件
8. Windows下执行nginx.exe启动服务，Linux下执行sudo nginx启动
9. 访问http://localhost或配置的域名查看效果

**方式3：使用IIS部署（Windows Server环境）**
1. 打开Windows服务器管理器，添加角色和功能
2. 勾选Web服务器（IIS），安装IIS组件
3. 从IIS官网下载并安装URL Rewrite模块（用于支持前端路由）
4. 将构建好的dist目录整体复制到IIS网站根目录（通常是C:\inetpub\wwwroot\bankvue）
5. 在dist目录下创建web.config文件（IIS配置文件）
6. 编写web.config内容：
   - 配置URL重写规则，将所有请求重定向到index.html
   - 排除实际存在的文件和目录，避免静态资源无法访问
   - 设置规则为停止处理（stopProcessing="true"）
7. 保存web.config文件
8. 打开IIS管理器，添加新网站或更新现有网站
9. 设置网站物理路径为dist目录
10. 设置绑定端口（通常为80）
11. 启动网站，访问http://localhost测试

---

### 2.4 ClickHouse部署（可选）

#### 步骤1：安装ClickHouse

**Windows:**
```powershell
# 下载安装包
# https://clickhouse.com/docs/en/install

# 运行安装
.\clickhouse-server.exe install
.\clickhouse-server.exe start
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt-get install -y apt-transport-https ca-certificates dirmngr
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 8919F6BD2B48D754
echo "deb https://packages.clickhouse.com/deb stable main" | sudo tee /etc/apt/sources.list.d/clickhouse.list
sudo apt-get update
sudo apt-get install -y clickhouse-server clickhouse-client

# 启动服务
sudo systemctl start clickhouse-server
sudo systemctl enable clickhouse-server
```

#### 步骤2：创建数据库和表

```bash
# 连接ClickHouse
clickhouse-client

# 创建数据库
CREATE DATABASE banks;
USE banks;

# 导入表结构
# 执行前面生成的建表SQL
```

#### 步骤3：导入数据

```bash
# 从PostgreSQL导出数据
cd C:\Users\Lenovo\IdeaProjects\banks

# 运行导出脚本（后续创建）
.\export_to_clickhouse.ps1
```

---

## 3. 部署后环境验证

### 3.1 数据库验证

#### 验证步骤：

**步骤1：连接数据库**
1. 打开命令行工具
2. 使用psql命令连接PostgreSQL数据库
3. 指定用户名为banks_user，数据库名为banks
4. 输入密码后成功连接

**步骤2：检查数据表是否创建成功**
1. 在psql命令行中输入 \dt 命令查看所有表
2. 应该看到以下6张表：
   - account（账户信息表）
   - analysis_indicators（分析指标配置表）
   - analysis_tasks（分析任务表）
   - login_record（登录记录表）
   - risk_results（风险分析结果表）
   - transfer_records（转账记录表）
3. 确认表的所有者（Owner）为banks_user
4. 如果表不存在，需要重新执行步骤2.1的步骤3（导入数据库结构）

**步骤3：检查数据是否存在**
1. 在psql命令行中查询account表的记录数
2. 查询transfer_records表的记录数
3. 如果返回数字大于0，说明有数据
4. 如果返回0，说明表已创建但暂无数据（需要稍后导入测试数据）
5. 输入 \q 退出psql

**验证通过标准：**
- 能成功连接数据库
- 所有必需的表都已创建
- 表结构完整无错误

---

### 3.2 后端服务验证

#### 验证步骤：

**步骤1：基础健康检查**
1. 打开浏览器或命令行工具
2. 访问健康检查接口：http://localhost:8080/api/actuator/health
3. 如果使用浏览器，应该看到JSON格式的响应
4. 如果使用curl命令，执行：curl http://localhost:8080/api/actuator/health
5. 正常情况下返回：{"status":"UP"}
6. 如果返回UP状态，说明Spring Boot应用已正常运行
7. 如果无法访问，检查服务是否启动、端口是否正确

**步骤2：API功能验证**
1. 测试获取分析指标列表：
   - 访问：http://localhost:8080/api/indicators
   - 应该返回JSON格式的指标列表
   - 如果返回空数组[]，说明暂无指标数据
   
2. 测试获取分析任务列表：
   - 访问：http://localhost:8080/api/tasks
   - 应该返回JSON格式的任务列表
   - 正常返回说明API接口工作正常

3. 测试创建任务（使用Postman工具）：
   - 打开Postman软件
   - 创建POST请求，地址为：http://localhost:8080/api/tasks
   - 设置请求头Content-Type为application/json
   - 在请求体中输入测试任务JSON数据
   - 发送请求，如果返回201状态码和任务信息，说明创建成功

**步骤3：日志文件检查**
1. 打开项目的logs目录
2. 查看spring.log文件（最新的日志文件）
3. Windows系统使用记事本或编辑器打开
4. Linux系统使用tail命令查看最后50行
5. 检查日志中是否有ERROR级别的错误信息
6. 正常情况下应该只有INFO和DEBUG级别的日志
7. 如果有错误，根据错误信息进行排查

**验证通过标准：**
- 健康检查接口返回UP状态
- API接口可正常访问并返回数据
- 日志文件无严重错误信息

---

### 3.3 前端应用验证

#### 验证步骤：

**步骤1：页面访问测试**
1. 打开Chrome、Edge或Firefox浏览器
2. 开发模式访问：http://localhost:5173
3. 生产模式访问：http://localhost:3000（或配置的端口）
4. 页面应该正常加载，显示系统界面
5. 如果显示空白页面，按F12打开开发者工具查看错误信息

**步骤2：界面元素检查**
1. 检查页面顶部导航栏是否正常显示
2. 检查左侧菜单栏是否正常显示，包含以下菜单项：
   - 任务管理
   - 指标管理
   - 结果查看
   - 其他功能模块
3. 点击各菜单项，确认页面能够正常切换
4. 确认页面布局美观，无样式错乱

**步骤3：功能测试清单**（逐项测试并记录结果）

1. 创建新的分析任务：
   - 进入任务管理页面
   - 点击"新建任务"按钮
   - 填写任务名称、选择指标、配置规则
   - 点击保存，确认任务创建成功

2. 启动任务执行：
   - 在任务列表中找到刚创建的任务
   - 点击"启动"按钮
   - 观察任务状态变为"执行中"

3. 查看任务结果：
   - 等待任务执行完成（状态变为"已完成"）
   - 点击"查看结果"按钮
   - 确认能够看到分析结果数据

4. 查看风险账户列表：
   - 进入结果查看页面
   - 确认能够看到风险账户列表
   - 测试分页功能是否正常

5. 导出分析报告：
   - 在结果页面点击"导出"按钮
   - 确认能够下载CSV或Excel文件

6. 规则构建器测试：
   - 创建任务时使用规则构建器
   - 测试添加条件、组合规则等功能
   - 确认规则表达式生成正确

**步骤4：浏览器开发者工具检查**
1. 按F12键打开浏览器开发者工具
2. 切换到"Console"（控制台）标签：
   - 检查是否有红色错误信息
   - 允许有少量警告（Warning），但不应有错误（Error）
   - 如果有错误，记录错误信息用于排查

3. 切换到"Network"（网络）标签：
   - 刷新页面，观察网络请求
   - 检查所有API请求状态码是否为200或201
   - 检查是否有404（资源未找到）或500（服务器错误）
   - 点击某个API请求，查看响应数据是否正确

4. 检查跨域问题：
   - 在Console中查找包含"CORS"的错误信息
   - 如果有CORS错误，说明跨域配置有问题
   - 正常情况下不应有跨域错误

**验证通过标准：**
- 前端页面能够正常访问和显示
- 所有菜单和功能页面可以正常切换
- 核心功能测试通过（至少80%）
- 浏览器控制台无严重错误
- API请求能正常响应

### 3.4 Spark集群验证

#### 验证步骤：

**步骤1：检查Spark集群状态**
1. 打开浏览器访问Spark Master Web界面：http://master-ip:8080
2. 检查页面显示的集群状态信息：
   - Status：应该显示为ALIVE
   - Workers：显示已注册的Worker节点数量
   - Cores in use：显示正在使用的CPU核心数
   - Memory in use：显示正在使用的内存
3. 点击"Workers"链接，查看所有Worker节点详情
4. 确认每个Worker节点状态都是ALIVE
5. 检查每个Worker的资源使用情况（CPU、内存）

**步骤2：测试Spark任务提交**
1. 在应用服务器或任意一台安装了Spark客户端的机器上
2. 执行Spark自带的示例程序：
   - 命令：spark-submit --master spark://master-ip:7077 --class org.apache.spark.examples.SparkPi $SPARK_HOME/examples/jars/spark-examples_*.jar 100
3. 观察控制台输出，任务应该正常提交并执行
4. 任务执行完成后会输出计算结果（Pi的近似值）
5. 访问Spark Master Web界面的"Completed Applications"查看任务记录

**步骤3：验证应用程序与Spark集群连接**
1. 启动后端服务（确保已配置正确的Spark Master地址）
2. 观察后端服务启动日志
3. 查找包含"SparkContext"或"SparkSession"的日志信息
4. 确认日志显示成功连接到Spark集群，如：
   - "Successfully started service 'SparkUI' on port 4040"
   - "Registered with master spark://master-ip:7077"
5. 访问Spark Master Web界面，在"Running Applications"中应该能看到应用程序

**步骤4：执行分析任务测试Spark**
1. 通过前端或API创建一个分析任务
2. 启动任务执行
3. 访问Spark Master Web界面，查看任务执行情况
4. 点击应用程序链接，进入Spark Application UI（端口4040）
5. 在"Jobs"标签页查看Spark作业执行进度和状态
6. 在"Stages"标签页查看各阶段的详细信息
7. 在"Executors"标签页查看Executor资源使用情况
8. 任务完成后，确认结果数据正确

**步骤5：监控Spark集群性能**
1. 在Spark Master Web界面查看集群资源使用率
2. 检查Worker节点的CPU和内存使用情况
3. 确认没有Worker节点离线（Lost Workers应该为0）
4. 查看历史任务的执行时间和资源消耗
5. 如果发现性能问题，可能需要调整：
   - Executor数量（spark.executor.instances）
   - Executor内存（spark.executor.memory）
   - Executor核心数（spark.executor.cores）
   - 分区数（spark.sql.shuffle.partitions）

**验证通过标准：**
- Spark Master和所有Worker节点状态为ALIVE
- 能够成功提交和执行Spark任务
- 应用程序能正常连接Spark集群
- 分析任务能够在Spark上正常执行
- 集群资源使用合理，无异常

---

### 3.5 ClickHouse验证（如已部署）

#### 验证步骤：

**步骤1：连接ClickHouse数据库**
1. 打开命令行工具
2. 执行clickhouse-client命令连接ClickHouse
3. 成功连接后会显示ClickHouse的版本信息和命令提示符
4. 提示符显示为主机名和端口号

**步骤2：检查数据库和表**
1. 执行SHOW DATABASES查看所有数据库
2. 确认banks数据库存在
3. 执行USE banks切换到banks数据库
4. 执行SHOW TABLES查看所有表
5. 应该看到以下表：
   - account（账户表）
   - transfer_records（转账记录表）
   - login_record（登录记录表）
6. 如果表不存在，需要重新执行建表脚本

**步骤3：检查数据量**
1. 查询account表的记录数量
2. 查询transfer_records表的记录数量
3. 查询login_record表的记录数量
4. 如果返回数量大于0，说明数据已成功导入
5. 记录各表的数据量，用于后续对比

**步骤4：性能测试**
1. 执行聚合查询测试：按日期统计转账记录
2. 查询应该快速返回结果（通常几毫秒到几百毫秒）
3. 查看返回的数据是否准确
4. 对比PostgreSQL执行相同查询的速度
5. ClickHouse应该明显更快（特别是大数据量情况下）
6. 输入quit或exit退出ClickHouse客户端

**验证通过标准：**
- 能成功连接ClickHouse
- 数据库和表已创建
- 数据已成功导入
- 查询响应速度快

---

### 3.5 性能压测验证

#### 验证步骤：

**步骤1：Mock接口功能测试**
1. 打开浏览器或使用curl工具
2. 访问Mock接口健康检查：http://localhost:8080/api/mock/tasks/health
3. 应该返回JSON格式的健康状态信息
4. 响应时间应该在10-30毫秒之间（极快）
5. 如果正常返回，说明Mock接口已启用

**步骤2：使用Apache Bench进行压力测试**（如已安装）
1. 打开命令行工具
2. 执行压测命令：ab -n 1000 -c 10 http://localhost:8080/api/mock/tasks/health
   - -n 1000 表示总请求数为1000次
   - -c 10 表示并发数为10
3. 等待测试完成，查看测试报告：
   - Requests per second（每秒请求数）：应该>100
   - Time per request（平均响应时间）：应该<100ms
   - Failed requests（失败请求数）：应该=0
4. 如果性能指标符合预期，说明系统性能良好

**步骤3：使用JMeter进行图形化压测**（推荐）
1. 下载并安装Apache JMeter
2. 启动JMeter（运行jmeter.bat或jmeter.sh）
3. 创建测试计划：
   - 添加线程组：设置1000个虚拟用户，10秒内启动
   - 添加HTTP请求采样器：设置URL为mock接口地址
   - 添加监听器：选择"聚合报告"和"查看结果树"
4. 运行测试计划，观察实时结果
5. 查看聚合报告：
   - 样本数应该等于设定的请求数
   - 平均响应时间应该在合理范围内
   - 错误率应该为0%或接近0%
6. 分析性能瓶颈，根据需要调整系统配置

**验证通过标准：**
- Mock接口响应正常
- 压测成功完成，无报错
- 响应时间符合预期
- 系统能承受设定的并发量

---

### 3.6 集成验证（端到端测试）

#### 验证流程：

**步骤1：创建分析任务**
1. 使用Postman或其他API测试工具
2. 创建POST请求到任务创建接口：http://localhost:8080/api/tasks
3. 设置请求头Content-Type为application/json
4. 在请求体中填写完整的任务信息：
   - 任务名称：端到端测试
   - 任务编码：E2E_TEST_001
   - 指标IDS：1,2,3（选择已存在的指标）
   - 规则配置：设置为分离规则模式
   - 受害人规则：INDICATOR_A > 0
   - 可疑分子规则：INDICATOR_C >= 100000
   - 任务时间范围：365天
   - 指标时间窗口：30天
5. 发送请求，记录返回的任务ID
6. 确认返回状态码为201（创建成功）

**步骤2：启动分析任务**
1. 使用任务ID，创建POST请求到启动接口
2. 接口地址格式：http://localhost:8080/api/tasks/{taskId}/start
3. 将{taskId}替换为步骤1中获得的实际任务ID
4. 发送请求，确认返回成功信息
5. 观察返回的状态信息，任务状态应变为"运行中"（状态码1）

**步骤3：查询任务结果**
1. 等待任务执行完成（可通过查询任务详情接口获取状态）
2. 创建GET请求到结果查询接口
3. 接口地址：http://localhost:8080/api/tasks/{taskId}/results?page=0&size=20
4. 发送请求，查看返回的风险账户列表
5. 确认返回数据格式正确，包含账户信息、指标数值等
6. 记录风险账户数量

**步骤4：验证数据完整性**
1. 打开psql连接到PostgreSQL数据库
2. 查询analysis_tasks表，找到测试任务的记录
3. 检查任务状态（task_status）应该为2（已完成）
4. 检查result_count字段，确认与接口返回的数量一致
5. 检查analyzed_accounts字段，确认分析的账户数量合理
6. 查询risk_results表，统计该任务ID对应的风险结果数量
7. 确认数据库中的结果数量与API返回的一致
8. 如果数据完全一致，说明端到端流程运行正常

**验证通过标准：**
- 能成功创建任务
- 任务能正常启动和执行
- 任务能正常完成
- 结果数据准确无误
- 数据库记录与API返回一致

---

## 4. 常见问题排查

### 4.1 后端服务问题

#### 问题1：端口被占用
```bash
# 错误信息：Port 8080 was already in use

# 解决方案：
# Windows - 查找占用端口的进程
netstat -ano | findstr :8080
taskkill /PID <进程ID> /F

# 或修改端口
# application.properties: server.port=8081
```

#### 问题2：数据库连接失败
```bash
# 错误信息：Connection refused

# 排查步骤：
1. 检查PostgreSQL服务是否启动
   # Windows
   Get-Service postgresql*
   
   # Linux
   sudo systemctl status postgresql

2. 检查防火墙
   # Windows
   Test-NetConnection localhost -Port 5432
   
3. 检查配置
   # application.properties中的数据库地址、用户名、密码
```

#### 问题3：Spark初始化失败（Windows）
```bash
# 错误信息：Failed to locate the winutils binary

# 解决方案：
1. 下载winutils.exe
   https://github.com/cdarlint/winutils/tree/master/hadoop-3.0.0/bin

2. 创建目录 C:\hadoop\bin\
3. 放入winutils.exe
4. 设置环境变量 HADOOP_HOME=C:\hadoop
5. 重启服务
```

### 4.2 前端应用问题

#### 问题1：npm install失败
```bash
# 错误：网络超时

# 解决方案：使用国内镜像
npm config set registry https://registry.npmmirror.com
npm install
```

#### 问题2：跨域问题
```bash
# 错误：CORS policy

# 解决方案1：后端已配置@CrossOrigin
# 解决方案2：使用代理（vite.config.js）
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

#### 问题3：白屏问题
```bash
# 排查步骤：
1. 打开浏览器开发者工具（F12）
2. 查看Console错误信息
3. 检查Network请求状态
4. 确认后端API可访问
5. 清除浏览器缓存
```

### 4.3 数据库问题

#### 问题1：连接数耗尽
```bash
# 错误：FATAL: remaining connection slots are reserved

# 解决方案：调整PostgreSQL配置
# postgresql.conf:
max_connections = 200

# 重启PostgreSQL
sudo systemctl restart postgresql
```

#### 问题2：查询性能慢
```sql
-- 检查是否创建索引
SELECT * FROM pg_indexes WHERE tablename = 'transfer_records';

-- 分析查询计划
EXPLAIN ANALYZE SELECT * FROM transfer_records WHERE from_account_id = '123';

-- 创建缺失的索引
CREATE INDEX idx_from_account ON transfer_records(from_account_id);
```

### 4.4 ClickHouse问题

#### 问题1：启动失败
```bash
# 检查日志
tail -f /var/log/clickhouse-server/clickhouse-server.log

# 常见原因：
# 1. 端口冲突（默认9000）
# 2. 磁盘空间不足
# 3. 权限问题

# 解决：修改配置文件 /etc/clickhouse-server/config.xml
```

#### 问题2：导入数据失败
```bash
# 错误：Too many parts

# 解决方案：优化分区策略
ALTER TABLE transfer_records 
MODIFY SETTING parts_to_throw_insert = 1000;
```

---

## 5. 生产环境优化建议

### 5.1 性能优化

1. **数据库优化**
   - 定期执行VACUUM
   - 启用查询缓存
   - 配置连接池（HikariCP已配置）

2. **后端优化**
   - 启用JVM参数优化
   ```bash
   java -Xms2g -Xmx4g -XX:+UseG1GC -jar banks-0.0.1-SNAPSHOT.jar
   ```

3. **前端优化**
   - 启用Gzip压缩
   - CDN加速静态资源
   - 图片懒加载

### 5.2 安全加固

1. **修改默认端口**
2. **启用HTTPS（SSL证书）**
3. **配置防火墙规则**
4. **定期更新依赖**
5. **启用访问日志**

### 5.3 监控告警

1. **系统监控**
   - CPU、内存、磁盘使用率
   - 网络流量

2. **应用监控**
   - API响应时间
   - 错误率
   - 并发用户数

3. **数据库监控**
   - 慢查询日志
   - 连接数
   - 死锁检测

---

## 6. 快速部署脚本

### Windows一键部署脚本

创建 `deploy.ps1`:
```powershell
# 停止旧服务
Stop-Process -Name java -Force -ErrorAction SilentlyContinue

# 构建后端
cd C:\Users\Lenovo\IdeaProjects\banks
mvn clean package -DskipTests

# 启动后端
Start-Process java -ArgumentList "-jar","target/banks-0.0.1-SNAPSHOT.jar" -WindowStyle Hidden

# 构建前端
cd C:\Users\Lenovo\Desktop\bankvue
npm run build

# 启动前端
npx serve -s dist -p 3000

Write-Host "部署完成！"
Write-Host "后端API: http://localhost:8080/api"
Write-Host "前端页面: http://localhost:3000"
```

### Linux一键部署脚本

创建 `deploy.sh`:
```bash
#!/bin/bash
set -e

echo "开始部署..."

# 停止旧服务
sudo systemctl stop banks-api
sudo systemctl stop nginx

# 构建后端
cd /opt/banks
mvn clean package -DskipTests

# 复制jar文件
sudo cp target/banks-0.0.1-SNAPSHOT.jar /opt/banks/

# 构建前端
cd /opt/bankvue
npm run build

# 复制前端文件
sudo cp -r dist/* /var/www/html/bankvue/

# 启动服务
sudo systemctl start banks-api
sudo systemctl start nginx

echo "部署完成！"
echo "后端API: http://your-server:8080/api"
echo "前端页面: http://your-server"
```

---

## 📞 技术支持

如遇到部署问题，请联系技术支持团队或查看：
- 项目文档：`README.md`
- 技术栈说明：`项目技术栈.txt`
- API文档：`http://localhost:8080/api/swagger-ui.html`

---

**文档版本**: v1.0  
**最后更新**: 2024-11-02  
**维护人员**: 系统开发团队

