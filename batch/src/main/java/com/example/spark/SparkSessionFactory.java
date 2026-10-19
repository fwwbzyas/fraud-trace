package com.example.spark;

import org.apache.spark.sql.SparkSession;

/**
 * Spark 会话工厂 (HDFS 适配版)
 * 核心改造：
 * 1. 移除强制本地文件系统的配置 (file:/// 和 LocalFileSystem)
 * 2. 增加 Docker 环境下连接 HDFS 的关键网络配置
 */
public class SparkSessionFactory {

    public static SparkSession create(String appName) {
        System.out.println("🚀 创建 Spark 会话 (HDFS模式): " + appName);

        return SparkSession.builder()
                .appName(appName)
                // ✅ 保持 local[*]，表示使用本地 CPU 资源进行计算 (Client 模式)
                // 虽然计算在本地，但数据源将指向远程 HDFS
                .master("local[*]")

                // 🔧 内存与性能配置 (保留原有的优化配置，增强稳定性)
                .config("spark.driver.memory", "4g")
                .config("spark.executor.memory", "4g")
                .config("spark.driver.maxResultSize", "2g")
                .config("spark.network.timeout", "600s")
                .config("spark.executor.heartbeatInterval", "60s")
                .config("spark.memory.fraction", "0.8")
                .config("spark.memory.storageFraction", "0.3")

                // 🔥🔥🔥 核心改造：HDFS 兼容性配置 🔥🔥🔥

                // 1. 允许使用主机名连接 DataNode (解决 Docker 网络不通问题)
                // 配合 Windows hosts 文件: 127.0.0.1 datanode
                .config("spark.hadoop.dfs.client.use.datanode.hostname", "true")

                // 2. 禁用 HDFS 客户端缓存 (提高上传/读取稳定性，减少网络报错)
                .config("spark.hadoop.fs.hdfs.impl.disable.cache", "true")

                // 3. 序列化优化 (推荐配置)
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

                // ❌ [已删除] config("spark.hadoop.fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem")
                //    原因：这行代码强制 Spark 认为所有文件都在本地磁盘，必须删除才能读 hdfs://

                // ❌ [已删除] config("spark.sql.warehouse.dir", "file:///...")
                //    原因：删除后，Spark 会自动处理临时数据的存储位置

                .getOrCreate();
    }
}