package com.example.banks.config;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spark配置类
 * 用于初始化Spark环境，支撑大数据分析任务
 */
@Configuration
public class SparkConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SparkConfig.class);

    @Value("${spark.app.name:FraudDetectionAnalysis}")
    private String appName;

    @Value("${spark.master:local[*]}")
    private String master;

    @Value("${spark.sql.warehouse.dir:./spark-warehouse}")
    private String warehouseDir;

    /**
     * 创建SparkSession Bean
     * SparkSession是Spark 2.0+的统一入口点
     */
    @Bean
    public SparkSession sparkSession() {
        logger.info("========================================");
        logger.info("🚀 初始化Spark连接");
        logger.info("========================================");
        logger.info("应用名称: {}", appName);
        logger.info("Spark Master: {}", master);
        logger.info("数据仓库目录: {}", warehouseDir);
        
        // 🔧 完全模仿 fraudanalysis 的 SparkSessionFactory
        try {
            SparkSession spark = SparkSession.builder()
                    .appName(appName)
                    // ✅ 本地运行模式
                    .master("local[*]")
                    // ✅ 关闭 Hive 支持，禁用 Hadoop catalog
                    .config("spark.sql.catalogImplementation", "in-memory")
                    // ✅ warehouse 设为一个纯本地路径（避免触发 HDFS）
                    .config("spark.sql.warehouse.dir", "file:///C:/temp/banks-spark-warehouse")
                    // ✅ 禁用权限检查和目录创建权限（绕开 Hadoop）
                    .config("spark.hadoop.fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem")
                    .config("spark.hadoop.validateOutputSpecs", "false")
                    // ✅ 关闭 Hadoop 日志与目录操作
                    .config("spark.hadoop.fs.permissions.umask-mode", "000")
                    // ✅ 不启用 checkpoint / shuffle service
                    .config("spark.shuffle.service.enabled", "false")
                    .getOrCreate();
            
            logger.info("========================================");
            logger.info("✅ Spark连接成功！");
            logger.info("========================================");
            logger.info("Spark版本: {}", spark.version());
            logger.info("📊 Spark UI 访问地址: http://localhost:4040");
            logger.info("💡 提示: 启动后即可访问 Spark UI 查看任务状态");
            logger.info("🔧 配置: 本地模式，内存目录，简化配置");
            
            return spark;
            
        } catch (Exception e) {
            logger.error("❌ Spark 初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Spark 初始化失败", e);
        }
    }
}
