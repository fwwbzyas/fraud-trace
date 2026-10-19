# 金融电诈数据追查系统（fraud-trace）

基于 Vue 3、Spring Boot、PostgreSQL 和 Apache Spark/Hadoop 的反欺诈分析演示项目，包含账户交易查询、登录记录查询、分析指标配置、规则构建和分析任务结果展示。

本项目中的账户、姓名、证件号码、手机号、交易和登录记录均为模拟生成，不对应真实客户或真实案件。当前登录和角色切换是前端演示逻辑，不提供生产级认证授权；不要将后端无保护地暴露在公网，也不要导入真实金融数据。

## 目录

| 目录 | 内容 |
| --- | --- |
| `frontend/` | Vue 页面、模拟数据、可选 Dify 聊天挂件 |
| `backend/` | Spring Boot 3 后端、SQL、数据生成脚本 |
| `batch/` | 独立 Spark 分析模块与 Hadoop/Spark 部署参考 |

前端源码以 `frontend/` 为准。批处理模块中旧的预编译前端副本已移除，独立 Spark 控制台保留在 `/spark_console.html`。

## 快速体验：仅运行前端

需要 Node.js 22.12 或更新的 22.x 版本（也兼容 package.json 声明的其他版本）。

```sh
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

访问终端显示的地址，通常为 `http://localhost:5173`。示例配置启用演示模式，不需要数据库、Java、Spark 或 Dify。

演示账号：`admin / 123456`（管理员）、`staff001 / staff123`（职员）。这些是刻意公开的演示账号，不是数据库凭据。

若浏览器曾使用此系统，本地保存的模式优先于 `.env.local` 的默认值；请通过页面的模式切换菜单选择“测试模式”。模拟数据加载失败时会回退到内置随机数据。

```sh
npm run build
npm run preview
```

`public/data/` 里约 43 MB 的 CSV 会随构建一起发布。不要在该目录放入真实个人信息。字段与导入说明见 `frontend/docs/csv_data.md`。

## 数据库和后端配置

需要 JDK 17、Maven、PostgreSQL。Spark/HDFS 分析还需要可达的 Hadoop 服务；编译成功不代表这些外部依赖已部署。

1. 在项目根目录复制 `.env.example` 为 `.env`，填入自己的 `DB_PASSWORD`。`.env` 已被 Git 忽略。
2. 默认数据库名 `banksys`、用户名 `postgres`。先创建数据库，再使用 `backend/sql/schema.sql` 初始化专用的空数据库。脚本内有 `owner to postgres`，使用其他数据库用户时需同步修改所有者。
3. `.env` 不是 Java/Python 自动加载的文件。macOS/Linux 可在项目根目录运行以下命令将变量导出到当前终端；含空格、美元符号等特殊字符的值请用单引号包围。只加载自己创建并信任的文件。

```sh
set -a
. ./.env
set +a
cd backend
mvn package
java -jar target/banks-0.0.1-SNAPSHOT.jar
```

Windows PowerShell 可用 `$env:DB_PASSWORD = '你设置的本地密码'` 等命令设置进程环境变量，然后启动 Java。不要把真实密码写回源码。

主要变量：

| 变量 | 用途 |
| --- | --- |
| `DB_PASSWORD` | 必填；Spring 后端、JDBC 工具和 Python 数据生成器使用 |
| `DB_URL` | Java JDBC 地址，默认 `jdbc:postgresql://localhost:5432/banksys` |
| `DB_USER` | 本地 Java/Python 默认用户名 `postgres` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | Python 生成器连接设置；DB_NAME 会覆盖脚本原来的默认库名 |
| `HDFS_URL` | 本地 Java 默认 `hdfs://localhost:8020` |
| `HADOOP_USER_NAME` | HDFS 用户，默认 `root`，按实际环境修改 |
| `CLICKHOUSE_PASSWORD` / `CLICKHOUSE_URL` | 仅旧的可选 ClickHouse 工具使用 |

Python 脚本保留各自原有生成逻辑，部分默认生成百万甚至上亿条数据，并会写数据库。**不要为了启动演示而执行它们**；使用前先调整数据量、目标库名并安装 `backend/scripts/data_generation/requirements.txt`。缺少密码时脚本会提前报错，不再使用内置密码。

连接后端时，把 `frontend/.env.local` 中的 `VITE_DEMO_MODE` 改为 `false`，页面切换到正常模式。前端使用相对 `/api` 路径；开发服务器通过 `API_PROXY_TARGET` 代理到后端。

## Docker 部署参考

以下命令均在项目根目录运行。先准备 `.env` 并设置 `DB_PASSWORD`：

```sh
docker compose --env-file .env -f backend/deploy/compose.yml config --quiet
docker compose --env-file .env -f backend/deploy/compose.yml up --build
```

Compose 部署 PostgreSQL、后端、Nginx 前端，PostgreSQL 固定用户为 `postgres`。数据库初始化仅在空数据卷首次启动时执行；修改 `.env` 不会自动修改已有数据库用户的密码。

此 Compose **不包含 Hadoop/HDFS**。分析功能需额外部署 HDFS；`batch/deploy/compose_hadoop.yml` 为本地参考。后端容器访问宿主机上的 HDFS 时，将 `.env` 中 `HDFS_URL` 改为 `hdfs://host.docker.internal:8020`，同时确保 NameNode 返回的 DataNode 地址在容器内可达。不要在容器中使用 `localhost` 指向宿主机。

`compose_full.yml` 是更高资源配额的替代配置，不要与 `compose.yml` 同时启动。历史部署文档仅作参考。本次未完成数据库/HDFS/Spark 全链路运行验证。

## 可选 Dify 挂件

默认关闭。不需要 Dify 时不加载任何 Dify 脚本。

在 `frontend/.env.local` 中填写：

```dotenv
VITE_DIFY_ENABLED=true
VITE_DIFY_BASE_URL=https://your-dify.example.com
VITE_DIFY_EMBED_TOKEN=your-public-chatbot-embed-id
```

这里的 token 是 Dify “嵌入网站”功能提供的**公开挂件应用标识**，不是服务端 API 密钥。所有 `VITE_` 配置都会进入浏览器，不能放数据库密码或私有 API key。

修改后重新启动开发服务器或重新构建。Docker 构建时将这三个变量写到根目录 `.env`，Compose 会作为构建参数传入。线上地址应使用浏览器能访问的 Dify 域名，不要写 `localhost`。如需自定义聊天 API，请由后端保存私有密钥并调用 Dify。

## 验证与项目状态

```sh
cd frontend
npm test
npm run build
cd ../backend
mvn test
cd ../batch
mvn test
```

Java 模块目前没有业务自动化测试；上述 Maven 命令主要验证编译。本项目保留前端演示认证、部分历史工具和部署参考，不应描述为已通过生产安全审计。

仓库未指定开源许可证，作者可在确认授权方式后补充 LICENSE。第三方依赖遵循各自许可证。

## 上传 GitHub

详见 `docs/github-upload.md`。本项目的 `.gitignore` 排除本地密码配置、依赖目录和构建产物，`.env.example` 应保留在仓库中。
