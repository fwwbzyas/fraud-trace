# 银行欺诈分析系统数据库设计说明

## 一、引言

### 1.1 编写目的
本文档旨在为银行欺诈分析系统的数据库设计提供详细说明，明确数据库的结构、功能和设计原则，为系统开发、测试和维护提供技术依据。

### 1.2 数据库设计的目的和预期效果

我们设计的数据库为银行欺诈风险分析和监控提供详细说明，以支持银行的反欺诈业务运营，包括账户风险分析、交易监控、异常行为识别、风险评估等功能。

**预期效果：**
- 能够让业务人员**自定义分析指标**，如：转账次数、转账金额、登录频率、跨地域登录次数、单日最大转账金额等，满足不同业务场景的风险识别需求。
- 能够让业务人员**灵活配置风险识别规则**，通过可视化规则构建器组合多个指标条件，形成复杂的规则表达式（支持AND/OR/NOT逻辑组合）。
- 能够**区分受害人和嫌疑人**两类分析场景，分别配置不同的识别规则：
  - **受害人规则**：识别可能被骗、被盗刷的账户（如异常大额支出、频繁小额转账等）
  - **嫌疑人规则**：识别可能从事欺诈、洗钱的账户（如异常大额收入、快速资金转移等）
- 支持**海量历史数据存储和快速查询**，通过HDFS存储PB级历史交易记录，使用ClickHouse实现秒级OLAP分析。
- 提供**实时任务监控和进度跟踪**，记录每个分析任务的执行状态、进度、结果统计等信息。
- 实现**双时间维度灵活分析**，支持独立配置任务筛选时间范围（决定分析哪些账户）和指标计算时间窗口（决定如何计算指标），确保分析结果的准确性和公平性。

---

## 二、需求分析

基于让业务人员能够灵活配置分析指标和识别规则的预期效果，我们的数据库以支持动态指标配置、规则引擎驱动、大数据处理为前提设计。除了指标和任务的创建功能，我们还支持业务人员对分析指标、风险识别规则、分析任务进行删、查、改的一系列操作，并且对于交易数据的导入不仅支持单条信息录入，也支持以CSV文件格式批量导入，同时支持将分析结果导出为Excel或CSV格式便于报送。在功能实现上，系统采用创新的双时间维度设计（task_time_range决定分析哪些账户，indicator_time_window决定如何计算指标），支持受害人规则和嫌疑人规则分离配置，使用PostgreSQL的JSONB字段存储动态配置以实现灵活的规则定义，并通过Spark集群实现百万级甚至千万级账户的大规模并行分析。另外，系统也满足一系列非功能需求，包括：保证数据的安全性和完整性（RBAC权限控制、数据加密、主外键约束、事务支持）、提供快速的查询响应时间（单表查询不超过3秒，支持ClickHouse加速OLAP查询）、支持系统的可扩展性（通过增加Spark Worker和HDFS DataNode实现水平扩展）、确保系统的可维护性（统一命名规范、完整注释、版本管理）以及向后兼容性（保留旧版本字段，支持平滑升级）等。

---

## 三、数据库架构设计

### 3.1 数据库选型

本系统采用**多数据库混合架构**，根据不同数据特点选择最合适的存储方案：

| 数据库 | 版本 | 用途 | 数据类型 |
|--------|------|------|---------|
| **PostgreSQL** | 14+ | 主业务数据库 | 账户信息、任务配置、指标配置、最近30天的交易数据 |
| **ClickHouse** | 23+ | 历史数据分析 | 历史交易记录（30天前）、风险记录归档、统计报表 |
| **HDFS** | 3.3+ | 超大规模存储 | 超过1年的历史数据、原始日志、备份数据 |
| **Redis** | 6+ | 缓存加速 | 热点数据缓存、任务状态缓存、会话信息 |

### 3.2 分库分表策略

#### 3.2.1 时间维度分区
- **transfer_records** 和 **login_record** 表按月分区
- **risk_records** 表按季度分区
- 自动清理超过保留期限的历史分区

#### 3.2.2 数据归档策略
- PostgreSQL保留最近30天的热数据
- ClickHouse保留最近1年的温数据
- HDFS保留1年以上的冷数据
- 定期自动归档和清理

---

## 四、核心表设计概览

### 4.1 业务配置表（PostgreSQL）
- `analysis_indicators`：分析指标配置表
- `analysis_tasks`：分析任务表
- `account`：账户信息表

### 4.2 交易数据表（PostgreSQL + ClickHouse）
- `transfer_records`：转账记录表
- `login_record`：登录记录表

### 4.3 结果数据表（PostgreSQL + ClickHouse）
- `risk_records`：风险账户记录表

### 4.4 系统管理表（PostgreSQL）
- `system_config`：系统配置表
- `audit_log`：审计日志表
- `user_role`：用户角色表

---

## 五、技术特色

### 5.1 JSONB动态配置
使用PostgreSQL的JSONB字段存储指标的计算逻辑和规则表达式，实现灵活的配置管理，无需频繁修改表结构。

### 5.2 双时间维度设计
创新的双时间维度设计（`task_time_range` + `indicator_time_window`），实现更精确、更公平的风险分析。

### 5.3 分离规则架构
受害人规则和嫌疑人规则完全独立配置和执行，提供全方位的风险识别能力。

### 5.4 大数据处理能力
通过Spark集群实现分布式计算，支持千万级甚至亿级数据的实时分析。

### 5.5 多级存储优化
热温冷数据分级存储，平衡性能和成本，满足不同场景的查询需求。

---

**本数据库设计充分考虑了银行欺诈分析业务的复杂性和大数据处理需求，通过合理的架构设计和技术选型，实现了高性能、高可用、易扩展的数据存储和处理能力。**

---

## 三、数据库表的联系

### 3.1 核心表关系说明

**(1) 账户表（account）是系统的核心主表**
- 账户表通过主键`id`与其他所有业务表建立关联关系
- 登录记录表（login_record）通过外键`account_id`关联账户表
- 转账记录表（transfer_records）通过外键`from_account_id`和`to_account_id`关联账户表，分别表示付款方和收款方
- 风险结果表（risk_results）通过外键`account_id`关联账户表

**(2) 分析任务表（analysis_tasks）是业务流程的核心**
- 分析任务表通过`indicator_ids`字段（存储指标ID列表如"1,2,3"）关联分析指标表（analysis_indicators）
- 风险结果表通过外键`task_id`关联分析任务表，记录每个任务识别出的风险账户
- 分析任务表的`rule_config`字段（JSONB格式）存储动态规则配置，支持灵活的规则组合

**(3) 外键约束确保数据完整性**
- 对于登录记录表和转账记录表，使用`ON DELETE RESTRICT`约束，防止删除仍有业务数据的账户
- 对于风险结果表，使用`ON DELETE CASCADE`约束，删除任务时自动清理相关的风险记录
- 风险结果表使用联合唯一约束`UNIQUE (task_id, account_id, account_role)`，确保同一任务中同一账户的同一角色只有一条记录

**(4) 主键设计**
- 所有表均使用`BIGSERIAL`类型的自增主键（如`id`、`record_id`、`transfer_id`、`task_id`、`risk_id`），确保记录唯一性
- 业务唯一性字段使用`UNIQUE`约束（如`account_no`、`task_code`、`indicator_code`），保证业务层面不重复

### 3.2 数据流转关系

```
用户创建分析任务
      ↓
analysis_tasks（存储任务配置，包括indicator_ids和rule_config）
      ↓
系统执行分析，从account表筛选账户
      ↓
根据indicator_ids查询analysis_indicators获取指标配置
      ↓
查询transfer_records和login_record计算每个账户的指标值
      ↓
使用rule_config中的规则表达式进行匹配
      ↓
将识别的风险账户写入risk_results表
      ↓
用户查询风险结果（通过task_id关联查询账户详情）
```

---

## 四、数据库实现

### 4.1 数据库的选择

该项目数据库采用**PostgreSQL 14+**作为主业务数据库，同时支持**ClickHouse 23+**用于历史数据分析，并使用**HDFS 3.3+**进行超大规模数据存储。

**PostgreSQL选择理由：**
- PostgreSQL是世界最先进的开源关系型数据库，性能卓越，服务稳定，很少出现异常宕机。
- 支持**JSONB数据类型**，可灵活存储指标配置和规则表达式，无需频繁修改表结构。
- 提供完善的**事务支持**和**外键约束**，确保数据一致性和完整性。
- 维护成本低，支持多种开发语言，对Java、Python等主流语言有很好的支持。
- 采用标准SQL语法，支持复杂的查询操作，如窗口函数、CTE（公共表表达式）等高级特性。

**ClickHouse作为补充：**
- ClickHouse是面向OLAP场景的列式存储数据库，查询性能比传统行式数据库快10-100倍。
- 适合存储历史交易记录和风险记录归档数据，支持亿级数据的秒级查询。
- 与PostgreSQL形成互补，PostgreSQL处理实时业务，ClickHouse处理历史分析。

**HDFS用于超大规模存储：**
- Apache Hadoop HDFS提供分布式存储能力，支持PB级数据存储。
- 数据副本机制（默认3副本）确保数据高可用性和容错性。
- 配合Spark进行分布式计算，实现千万级甚至亿级数据的实时分析。

### 4.2 数据库创建表语句（核心表）

#### PostgreSQL数据库：

**创建账户表（account）**
```sql
CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    account_no VARCHAR(50) NOT NULL UNIQUE,
    holder_name VARCHAR(100) NOT NULL,
    id_card_no VARCHAR(20) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    account_type SMALLINT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**创建转账记录表（transfer_records）**
```sql
CREATE TABLE transfer_records (
    transfer_id BIGSERIAL PRIMARY KEY,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    transfer_no VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(20,2) NOT NULL,
    transfer_time TIMESTAMP NOT NULL,
    FOREIGN KEY (from_account_id) REFERENCES account(id) ON DELETE RESTRICT,
    FOREIGN KEY (to_account_id) REFERENCES account(id) ON DELETE RESTRICT
);
```

**创建登录记录表（login_record）**
```sql
CREATE TABLE login_record (
    record_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    login_time TIMESTAMP NOT NULL,
    login_ip VARCHAR(50) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE RESTRICT
);
```

**创建分析指标配置表（analysis_indicators）**
```sql
CREATE TABLE analysis_indicators (
    indicator_id BIGSERIAL PRIMARY KEY,
    indicator_code VARCHAR(50) NOT NULL UNIQUE,
    indicator_name VARCHAR(100) NOT NULL,
    indicator_desc TEXT NOT NULL,
    param_config JSONB NOT NULL,
    is_core SMALLINT NOT NULL DEFAULT 1,
    is_active SMALLINT NOT NULL DEFAULT 1,
    user_type VARCHAR(20),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**创建分析任务表（analysis_tasks）**
```sql
CREATE TABLE analysis_tasks (
    task_id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL,
    task_code VARCHAR(50) NOT NULL UNIQUE,
    indicator_ids VARCHAR(200) NOT NULL,
    rule_config JSONB NOT NULL,
    time_range INTEGER NOT NULL,
    task_time_range INTEGER NOT NULL,
    indicator_time_window INTEGER NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    task_status SMALLINT NOT NULL DEFAULT 0,
    progress INTEGER NOT NULL DEFAULT 0,
    analyzed_accounts INTEGER DEFAULT 0,
    result_count INTEGER DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**创建风险结果表（risk_results）**
```sql
CREATE TABLE risk_results (
    risk_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    account_role SMALLINT NOT NULL,
    matched_indicators JSONB NOT NULL,
    matched_rules TEXT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES analysis_tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE RESTRICT,
    UNIQUE (task_id, account_id, account_role)
);
```

#### ClickHouse数据库（用于历史数据存储）：

**创建转账记录归档表**
```sql
CREATE TABLE transfer_records (
    transfer_id Int64,
    from_account_id Int64,
    to_account_id Int64,
    transfer_no String,
    amount Decimal(20,2),
    transfer_time DateTime
) ENGINE = MergeTree()
PARTITION BY toYear(transfer_time)
ORDER BY (from_account_id, transfer_time);
```

**创建风险记录归档表**
```sql
CREATE TABLE risk_records (
    risk_id Int64,
    task_id Int64,
    account_id Int64,
    account_role Int8,
    matched_indicators String,
    detection_time DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(detection_time)
ORDER BY (task_id, detection_time);
```

### 4.3 典型查询示例

#### 示例1：查询某个任务的所有风险账户（受害人）

```sql
SELECT 
    a.account_no,
    a.holder_name,
    a.id_card_no,
    r.matched_indicators,
    r.matched_rules,
    r.create_time
FROM risk_results r
JOIN account a ON r.account_id = a.id
WHERE r.task_id = 1
  AND r.account_role = 1  -- 1=付款方/受害人
ORDER BY r.create_time DESC;
```

#### 示例2：统计某个账户的转账情况

```sql
SELECT 
    COUNT(*) as transfer_count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount,
    MAX(amount) as max_amount
FROM transfer_records
WHERE from_account_id = 12345
  AND transfer_time >= NOW() - INTERVAL '30 days';
```

#### 示例3：查找大额快进快出的可疑账户

```sql
-- 查找30天内收款超过50万且在2小时内全部转出的账户
WITH income AS (
    SELECT to_account_id, SUM(amount) as total_in
    FROM transfer_records
    WHERE transfer_time >= NOW() - INTERVAL '30 days'
      AND amount >= 50000
    GROUP BY to_account_id
    HAVING SUM(amount) >= 500000
),
quick_out AS (
    SELECT DISTINCT t1.to_account_id
    FROM transfer_records t1
    JOIN transfer_records t2 ON t1.to_account_id = t2.from_account_id
    WHERE t2.transfer_time <= t1.transfer_time + INTERVAL '2 hours'
      AND t1.amount >= 50000
)
SELECT DISTINCT a.account_no, a.holder_name, i.total_in
FROM income i
JOIN quick_out q ON i.to_account_id = q.to_account_id
JOIN account a ON i.to_account_id = a.id;
```

#### 示例4：查询使用某个指标的所有任务

```sql
SELECT 
    task_id,
    task_name,
    task_code,
    indicator_ids,
    task_status
FROM analysis_tasks
WHERE ',' || indicator_ids || ',' LIKE '%,3,%'  -- 查找使用指标ID=3的任务
ORDER BY create_time DESC;
```

#### 示例5：按账户角色统计风险账户分布

```sql
SELECT 
    CASE account_role
        WHEN 1 THEN '受害人（付款方）'
        WHEN 2 THEN '嫌疑人（收款方）'
    END as role_name,
    COUNT(*) as count
FROM risk_results
WHERE task_id = 1
GROUP BY account_role;
```

---

## 五、总结

业务人员可以根据不同的风险识别需求，在分析指标配置表（analysis_indicators）中创建自定义的分析指标，如"大额快进快出次数"、"异地登录次数"、"单日转账金额"等，并为每个指标设置灵活的参数配置（存储在param_config字段的JSONB格式中）。在创建分析任务时，业务人员可以选择多个指标组合，通过可视化规则构建器创建复杂的规则表达式（存储在analysis_tasks表的rule_config字段中），系统支持受害人规则和嫌疑人规则分离配置，实现更精准的风险识别。

在数据库设计中，我们通过主键和外键建立了完善的表间关联关系。账户表（account）作为核心主表，其主键id被登录记录表、转账记录表和风险结果表通过外键引用，确保数据的引用完整性。转账记录表通过from_account_id和to_account_id两个外键字段分别关联付款方和收款方账户，实现了账户间资金流转的完整记录。风险结果表通过task_id外键关联分析任务表，通过account_id外键关联账户表，使得我们能够快速查询每个任务识别出的风险账户，以及每个账户在哪些任务中被识别为风险。

系统采用创新的双时间维度设计，在analysis_tasks表中同时存储task_time_range（任务筛选时间范围，决定分析哪些账户）和indicator_time_window（指标计算时间窗口，决定从账户最后交易时间往前推多少天计算指标），确保分析结果的准确性和公平性。系统还保留了time_range字段用于向后兼容，确保旧版本API仍能正常工作。

为了支持海量数据的高效处理，系统采用多数据库混合架构：PostgreSQL存储最近30天的热数据用于实时业务查询，ClickHouse存储最近1年的温数据用于历史分析和统计报表，HDFS存储1年以上的冷数据用于长期归档和深度分析。这种分级存储策略既保证了查询性能，又控制了存储成本。配合Spark集群的分布式计算能力，系统可以实现千万级甚至亿级账户的大规模并行分析，单次任务执行时间大幅缩短。

未来，我们将在以下方面进一步优化系统：一是增强数据安全性，实现更细粒度的权限控制和数据脱敏；二是优化查询性能，通过增加合理的索引和分区策略提升查询响应速度；三是扩展规则引擎能力，支持更复杂的规则表达式和机器学习模型集成；四是完善监控告警机制，实现系统运行状态的实时监控和异常自动处理。

