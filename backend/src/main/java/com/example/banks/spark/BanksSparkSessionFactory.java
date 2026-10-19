package com.example.banks.spark;

import org.apache.spark.sql.SparkSession;

/**
 * 改造后的 SparkSessionFactory
 * 支持连接远程 Hadoop 集群 (HDFS)
 */
public class BanksSparkSessionFactory {

    /**
     * 创建 SparkSession
     * @param appName 应用名称
     * @param masterUrl 运行模式 (从配置文件读取，例如 "local[*]")
     * @return SparkSession 实例
     */
    public static SparkSession create(String appName, String masterUrl) {
        // 构建器
        SparkSession.Builder builder = SparkSession.builder()
                .appName(appName)
                .master(masterUrl);

        // ====================================================
        // 配置优化与 HDFS 支持
        // ====================================================

        // 1. 序列化优化 (提升大数据量下的性能)
        builder.config("spark.serializer", "org.apache.spark.serializer.KryoSerializer");

        // 2. Catalog 配置 (保持 in-memory，因为我们不使用 Hive Metastore)
        builder.config("spark.sql.catalogImplementation", "in-memory");

        // 3. 关键网络配置：允许客户端通过主机名访问 DataNode
        // (解决在本地开发机访问远程 HDFS 时常见的通信问题)
        builder.config("spark.hadoop.dfs.client.use.datanode.hostname", "true");

        // 4. 关闭不需要的 Shuffle 服务 (本地模式不需要)
        builder.config("spark.shuffle.service.enabled", "false");

        // 5. 开启 Spark UI (便于通过 localhost:4040 监控任务)
        builder.config("spark.ui.enabled", "true");
        builder.config("spark.ui.port", "4040");

        // ❌ 已删除：spark.sql.warehouse.dir (现在由 application.properties 控制)
        // ❌ 已删除：spark.hadoop.fs.file.impl (删除后才能支持 hdfs:// 协议)
        // ❌ 已删除：spark.hadoop.fs.permissions.umask-mode (交由 Hadoop 默认配置)

        return builder.getOrCreate();
    }
}