# 大数据集群完整部署指南（HDFS + Spark + ZooKeeper）

## 1. 完整集群架构

```
┌──────────────── ZooKeeper集群（高可用协调）────────────────┐
│  ZK1(192.168.1.10)  ZK2(192.168.1.11)  ZK3(192.168.1.12)  │
└───────────────────────────┬────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼───────┐  ┌────────▼────────┐
│ HDFS NameNode  │  │Spark Master  │  │Spark Master     │
│   (Active)     │  │   (Active)   │  │   (Standby)     │
│ 192.168.1.20   │  │192.168.1.30  │  │192.168.1.31     │
└────────────────┘  └──────────────┘  └─────────────────┘
        │
┌───────▼────────┐
│ HDFS NameNode  │
│   (Standby)    │
│ 192.168.1.21   │
└────────────────┘
        │
  ┌─────┴─────┬────────┬────────┬────────┐
  │           │        │        │        │
┌─▼──┐    ┌──▼─┐   ┌──▼─┐   ┌──▼─┐   ┌──▼─┐
│DN1 │    │DN2 │   │DN3 │   │DN4 │   │DN5 │
│WK1 │    │WK2 │   │WK3 │   │WK4 │   │WK5 │
└────┘    └────┘   └────┘   └────┘   └────┘
 .40        .41      .42      .43      .44

DN = HDFS DataNode
WK = Spark Worker（与DataNode共部署）
```

---

## 2. 服务器清单（生产环境完整配置）

| 角色 | IP地址 | 主机名 | 配置 | 部署服务 |
|-----|--------|--------|------|---------|
| **ZooKeeper集群** |||||
| ZK1 | 192.168.1.10 | zk1 | 4核/8GB/100GB | ZooKeeper |
| ZK2 | 192.168.1.11 | zk2 | 4核/8GB/100GB | ZooKeeper |
| ZK3 | 192.168.1.12 | zk3 | 4核/8GB/100GB | ZooKeeper |
| **HDFS集群** |||||
| NameNode1 | 192.168.1.20 | nn1 | 16核/32GB/1TB SSD | NameNode(Active) + JournalNode |
| NameNode2 | 192.168.1.21 | nn2 | 16核/32GB/1TB SSD | NameNode(Standby) + JournalNode |
| JournalNode | 192.168.1.22 | jn1 | 8核/16GB/500GB | JournalNode |
| **Spark集群** |||||
| Spark Master1 | 192.168.1.30 | sm1 | 8核/16GB/500GB | Spark Master(Active) |
| Spark Master2 | 192.168.1.31 | sm2 | 8核/16GB/500GB | Spark Master(Standby) |
| **计算存储节点（混合部署）** |||||
| Node1 | 192.168.1.40 | node1 | 16核/32GB/4TB | DataNode + Spark Worker |
| Node2 | 192.168.1.41 | node2 | 16核/32GB/4TB | DataNode + Spark Worker |
| Node3 | 192.168.1.42 | node3 | 16核/32GB/4TB | DataNode + Spark Worker |
| Node4 | 192.168.1.43 | node4 | 16核/32GB/4TB | DataNode + Spark Worker |
| Node5 | 192.168.1.44 | node5 | 16核/32GB/4TB | DataNode + Spark Worker |

**总计：** 13台服务器
- ZooKeeper: 3台
- HDFS管理节点: 3台（2 NameNode + 1 额外JournalNode）
- Spark管理节点: 2台
- 计算存储混合节点: 5台

---

## 3. 环境准备（所有节点）

### 3.1 配置主机名和hosts

**所有节点执行：**
```bash
# 编辑/etc/hosts，添加：
192.168.1.100 spark-master
192.168.1.101 spark-worker1
192.168.1.102 spark-worker2
192.168.1.103 spark-worker3
```

### 3.2 安装Java

**所有节点执行：**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk -y

# CentOS/RHEL
sudo yum install java-11-openjdk java-11-openjdk-devel -y

# 验证
java -version
```

### 3.3 配置SSH免密登录

**Master节点执行：**
```bash
# 生成SSH密钥
ssh-keygen -t rsa -P "" -f ~/.ssh/id_rsa

# 复制公钥到所有Worker节点
ssh-copy-id spark-worker1
ssh-copy-id spark-worker2
ssh-copy-id spark-worker3

# 测试免密登录
ssh spark-worker1 "hostname"
```

---

## 4. ZooKeeper集群部署

### 4.1 下载ZooKeeper

**在所有ZooKeeper节点执行：**
```bash
cd /opt
wget https://archive.apache.org/dist/zookeeper/zookeeper-3.8.3/apache-zookeeper-3.8.3-bin.tar.gz
tar -xzf apache-zookeeper-3.8.3-bin.tar.gz
ln -s apache-zookeeper-3.8.3-bin zookeeper
```

### 4.2 配置ZooKeeper

**在所有ZooKeeper节点执行：**
```bash
# 创建数据目录
mkdir -p /data/zookeeper
mkdir -p /data/zookeeper/logs

# 创建配置文件
cd /opt/zookeeper/conf
cp zoo_sample.cfg zoo.cfg

# 编辑zoo.cfg
cat > zoo.cfg << 'EOF'
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/data/zookeeper
dataLogDir=/data/zookeeper/logs
clientPort=2181
maxClientCnxns=60

# ZooKeeper集群配置
server.1=192.168.1.10:2888:3888
server.2=192.168.1.11:2888:3888
server.3=192.168.1.12:2888:3888

# 性能优化
autopurge.snapRetainCount=3
autopurge.purgeInterval=1
EOF
```

**配置myid文件（每个节点不同）：**
```bash
# ZK1节点执行
echo "1" > /data/zookeeper/myid

# ZK2节点执行
echo "2" > /data/zookeeper/myid

# ZK3节点执行
echo "3" > /data/zookeeper/myid
```

### 4.3 启动ZooKeeper集群

```bash
# 在所有ZooKeeper节点执行
/opt/zookeeper/bin/zkServer.sh start

# 检查状态
/opt/zookeeper/bin/zkServer.sh status

# 应该看到1个Leader和2个Follower
```

### 4.4 验证ZooKeeper

```bash
# 测试连接
/opt/zookeeper/bin/zkCli.sh -server 192.168.1.10:2181

# 在ZK命令行中执行
ls /
quit
```

---

## 5. HDFS集群部署

### 5.1 下载Hadoop

**在所有Hadoop节点执行：**
```bash
cd /opt
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
tar -xzf hadoop-3.3.6.tar.gz
ln -s hadoop-3.3.6 hadoop
```

### 5.2 配置环境变量

**在所有Hadoop节点执行：**
```bash
cat >> ~/.bashrc << 'EOF'
export HADOOP_HOME=/opt/hadoop
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
EOF

source ~/.bashrc
```

### 5.3 配置Hadoop（NameNode节点）

**编辑 core-site.xml：**
```xml
<configuration>
  <property>
    <name>fs.defaultFS</name>
    <value>hdfs://bankcluster</value>
  </property>
  <property>
    <name>ha.zookeeper.quorum</name>
    <value>192.168.1.10:2181,192.168.1.11:2181,192.168.1.12:2181</value>
  </property>
  <property>
    <name>hadoop.tmp.dir</name>
    <value>/data/hadoop/tmp</value>
  </property>
</configuration>
```

**编辑 hdfs-site.xml（NameNode HA配置）：**
```xml
<configuration>
  <!-- NameService配置 -->
  <property>
    <name>dfs.nameservices</name>
    <value>bankcluster</value>
  </property>
  
  <!-- NameNode列表 -->
  <property>
    <name>dfs.ha.namenodes.bankcluster</name>
    <value>nn1,nn2</value>
  </property>
  
  <!-- NameNode RPC地址 -->
  <property>
    <name>dfs.namenode.rpc-address.bankcluster.nn1</name>
    <value>192.168.1.20:8020</value>
  </property>
  <property>
    <name>dfs.namenode.rpc-address.bankcluster.nn2</name>
    <value>192.168.1.21:8020</value>
  </property>
  
  <!-- NameNode HTTP地址 -->
  <property>
    <name>dfs.namenode.http-address.bankcluster.nn1</name>
    <value>192.168.1.20:9870</value>
  </property>
  <property>
    <name>dfs.namenode.http-address.bankcluster.nn2</name>
    <value>192.168.1.21:9870</value>
  </property>
  
  <!-- JournalNode配置 -->
  <property>
    <name>dfs.namenode.shared.edits.dir</name>
    <value>qjournal://192.168.1.20:8485;192.168.1.21:8485;192.168.1.22:8485/bankcluster</value>
  </property>
  
  <!-- 故障转移配置 -->
  <property>
    <name>dfs.client.failover.proxy.provider.bankcluster</name>
    <value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>
  </property>
  <property>
    <name>dfs.ha.fencing.methods</name>
    <value>sshfence</value>
  </property>
  <property>
    <name>dfs.ha.fencing.ssh.private-key-files</name>
    <value>/root/.ssh/id_rsa</value>
  </property>
  
  <!-- 自动故障转移 -->
  <property>
    <name>dfs.ha.automatic-failover.enabled</name>
    <value>true</value>
  </property>
  
  <!-- 数据副本数 -->
  <property>
    <name>dfs.replication</name>
    <value>3</value>
  </property>
  
  <!-- 数据目录 -->
  <property>
    <name>dfs.datanode.data.dir</name>
    <value>/data/hadoop/hdfs/data</value>
  </property>
  <property>
    <name>dfs.namenode.name.dir</name>
    <value>/data/hadoop/hdfs/name</value>
  </property>
</configuration>
```

### 5.4 启动HDFS集群

```bash
# 1. 在3个JournalNode节点启动JournalNode
hdfs --daemon start journalnode

# 2. 在nn1节点格式化NameNode
hdfs namenode -format

# 3. 在nn1节点启动NameNode
hdfs --daemon start namenode

# 4. 在nn2节点同步元数据
hdfs namenode -bootstrapStandby

# 5. 在nn1节点初始化ZK
hdfs zkfc -formatZK

# 6. 在两个NameNode节点启动ZKFC
hdfs --daemon start zkfc

# 7. 在nn2节点启动NameNode
hdfs --daemon start namenode

# 8. 在所有DataNode节点启动DataNode
hdfs --daemon start datanode
```

### 5.5 验证HDFS

```bash
# 检查HDFS状态
hdfs haadmin -getServiceState nn1
hdfs haadmin -getServiceState nn2
# 应该看到一个Active，一个Standby

# 访问Web界面
# http://192.168.1.20:9870（Active NameNode）
# http://192.168.1.21:9870（Standby NameNode）

# 测试文件操作
hdfs dfs -mkdir -p /test
hdfs dfs -ls /
```

---

## 6. Spark集群部署

### 6.1 下载Spark

```bash
cd /opt
wget https://archive.apache.org/dist/spark/spark-3.5.0/spark-3.5.0-bin-hadoop3.tgz
tar -xzf spark-3.5.0-bin-hadoop3.tgz
ln -s spark-3.5.0-bin-hadoop3 spark
```

### 4.2 配置环境变量

**所有节点执行：**
```bash
# 编辑 ~/.bashrc 或 /etc/profile
export SPARK_HOME=/opt/spark
export PATH=$PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin

# 使环境变量生效
source ~/.bashrc
```

---

## 5. Master节点配置

### 5.1 配置spark-env.sh

```bash
cd $SPARK_HOME/conf
cp spark-env.sh.template spark-env.sh

# 编辑spark-env.sh，添加：
cat >> spark-env.sh << 'EOF'
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export SPARK_MASTER_HOST=192.168.1.100
export SPARK_MASTER_PORT=7077
export SPARK_MASTER_WEBUI_PORT=8080
export SPARK_WORKER_CORES=8
export SPARK_WORKER_MEMORY=16g
export SPARK_DAEMON_MEMORY=2g
EOF
```

### 5.2 配置workers文件

```bash
cd $SPARK_HOME/conf
cp workers.template workers

# 编辑workers文件，添加Worker节点：
cat > workers << 'EOF'
spark-worker1
spark-worker2
spark-worker3
EOF
```

### 5.3 配置spark-defaults.conf

```bash
cp spark-defaults.conf.template spark-defaults.conf

# 编辑spark-defaults.conf，添加：
cat >> spark-defaults.conf << 'EOF'
spark.master                     spark://192.168.1.100:7077
spark.eventLog.enabled           true
spark.eventLog.dir               file:///opt/spark/logs
spark.history.fs.logDirectory    file:///opt/spark/logs
spark.serializer                 org.apache.spark.serializer.KryoSerializer
spark.driver.memory              4g
spark.executor.memory            8g
spark.executor.cores             4
spark.sql.shuffle.partitions     200
EOF
```

---

## 6. Worker节点配置

### 6.1 同步配置文件

**从Master节点执行：**
```bash
# 将配置文件同步到所有Worker节点
scp $SPARK_HOME/conf/spark-env.sh spark-worker1:$SPARK_HOME/conf/
scp $SPARK_HOME/conf/spark-env.sh spark-worker2:$SPARK_HOME/conf/
scp $SPARK_HOME/conf/spark-env.sh spark-worker3:$SPARK_HOME/conf/

scp $SPARK_HOME/conf/spark-defaults.conf spark-worker1:$SPARK_HOME/conf/
scp $SPARK_HOME/conf/spark-defaults.conf spark-worker2:$SPARK_HOME/conf/
scp $SPARK_HOME/conf/spark-defaults.conf spark-worker3:$SPARK_HOME/conf/
```

---

## 7. 启动Spark集群

### 7.1 启动Master

**Master节点执行：**
```bash
$SPARK_HOME/sbin/start-master.sh

# 查看日志
tail -f $SPARK_HOME/logs/spark-*-master-*.out
```

### 7.2 启动所有Worker

**Master节点执行：**
```bash
$SPARK_HOME/sbin/start-workers.sh

# 或单独启动每个Worker
# ssh spark-worker1 "$SPARK_HOME/sbin/start-worker.sh spark://192.168.1.100:7077"
```

### 7.3 验证集群状态

```bash
# 访问Master Web UI
http://192.168.1.100:8080

# 或使用命令查看
jps  # 应该看到Master进程

# 在Worker节点查看
jps  # 应该看到Worker进程
```

---

## 8. 应用程序配置

### 8.1 修改application.properties

**后端项目配置：**
```properties
# Spark集群配置
spark.master=spark://192.168.1.100:7077
spark.app.name=BankFraudAnalysis
spark.driver.memory=4g
spark.executor.memory=8g
spark.executor.cores=4
spark.executor.instances=3
spark.sql.shuffle.partitions=200

# Spark性能优化
spark.default.parallelism=24
spark.sql.adaptive.enabled=true
spark.sql.adaptive.coalescePartitions.enabled=true
spark.serializer=org.apache.spark.serializer.KryoSerializer
```

---

## 9. 测试Spark集群

### 9.1 运行示例程序

```bash
spark-submit \
  --master spark://192.168.1.100:7077 \
  --deploy-mode client \
  --driver-memory 2g \
  --executor-memory 4g \
  --executor-cores 2 \
  --num-executors 3 \
  --class org.apache.spark.examples.SparkPi \
  $SPARK_HOME/examples/jars/spark-examples_*.jar \
  1000
```

### 9.2 查看执行结果

- 访问 http://192.168.1.100:8080 查看任务
- 访问 http://192.168.1.100:4040 查看Application UI
- 查看日志输出的Pi计算结果

---

## 10. 监控和管理

### 10.1 查看集群状态

```bash
# Master状态
http://192.168.1.100:8080

# Worker状态（每个Worker节点）
http://192.168.1.101:8081
http://192.168.1.102:8081
http://192.168.1.103:8081

# 历史服务器（可选）
$SPARK_HOME/sbin/start-history-server.sh
http://192.168.1.100:18080
```

### 10.2 常用管理命令

```bash
# 停止集群
$SPARK_HOME/sbin/stop-all.sh

# 重启集群
$SPARK_HOME/sbin/stop-all.sh
$SPARK_HOME/sbin/start-all.sh

# 查看进程
jps

# 查看日志
tail -f $SPARK_HOME/logs/spark-*.out
```

---

## 11. 性能调优建议

### 11.1 内存配置

- **Driver内存**：建议4-8GB，用于收集结果和任务调度
- **Executor内存**：建议8-16GB，根据数据量调整
- **Executor核心数**：建议4-8核，不超过物理核心数

### 11.2 并行度配置

- **spark.default.parallelism**：设置为CPU总核心数的2-3倍
- **spark.sql.shuffle.partitions**：默认200，大数据集可设置为500-1000

### 11.3 资源分配策略

- 预留20%系统资源给操作系统
- 每个Worker建议运行2-3个Executor
- 避免单个Executor占用过多资源

---

## 12. 故障排查

### 12.1 Worker无法连接Master

**检查项：**
- 防火墙是否开放7077端口
- Master地址配置是否正确
- 网络是否互通

### 12.2 任务执行失败

**检查项：**
- Executor内存是否足够
- 数据倾斜问题
- 序列化错误

### 12.3 性能问题

**优化方向：**
- 增加分区数
- 调整Executor数量
- 使用Kryo序列化
- 启用自适应查询执行

---

## 13. 生产环境检查清单

- [ ] 所有节点时间同步（NTP）
- [ ] 防火墙规则配置正确
- [ ] SSH免密登录配置完成
- [ ] Java版本一致（推荐Java 11）
- [ ] Spark版本一致（3.5.0）
- [ ] 环境变量配置正确
- [ ] 日志目录权限正确
- [ ] 资源配置合理（CPU、内存）
- [ ] 监控告警配置完成
- [ ] 备份恢复方案就绪

---

**配置完成后，访问 http://192.168.1.100:8080 确认集群状态正常！**

