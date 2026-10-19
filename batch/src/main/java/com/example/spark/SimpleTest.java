package com.example.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * Banks 项目的简单测试 (HDFS 版)
 * 测试 Spark 是否能成功连接 Docker 中的 Hadoop 并读取 CSV
 */
public class SimpleTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("🧪 Banks 项目 - Spark & HDFS 连接测试");
        System.out.println("========================================");

        // 1. 设置 Hadoop 用户，防止 Permission denied
        System.setProperty("HADOOP_USER_NAME", "root");

        // 2. 创建 Spark 会话
        SparkSession spark = SparkSessionFactory.create("BanksSimpleTest");

        // 3. 测试基本 Spark 功能 (确保 Spark 引擎正常)
        System.out.println("🔢 测试基本 Spark 计算 (Range 1-10):");
        Dataset<Row> data = spark.range(1, 11).toDF("number");
        Dataset<Row> result = data.filter("number % 2 = 0");
        result.show(5);

        // 4. 测试 HDFS 连接与数据读取
        String hdfsUri = "hdfs://localhost:8020";
        String hdfsBaseDir = "/user/banks/input";

        System.out.println("🔗 测试 HDFS 连接:");
        System.out.println("   URI: " + hdfsUri);
        System.out.println("   Base Dir: " + hdfsBaseDir);

        try {
            // 初始化数据摄取服务 (传入 HDFS 配置)
            BanksIngestionService ingestion = new BanksIngestionService(
                    spark,
                    hdfsUri,
                    hdfsBaseDir
            );

            System.out.println("🚀 尝试读取 account.csv ...");

            // 测试读取账户表
            Dataset<Row> accounts = ingestion.readAccounts();

            if (accounts.isEmpty()) {
                System.out.println("⚠️ 读取结果为空！请检查：");
                System.out.println("   1. Docker Hadoop 是否已启动？");
                System.out.println("   2. 是否已通过 API 上传了 account.csv？");
            } else {
                System.out.println("📊 账户表数据预览 (前5条):");
                accounts.show(5);
                System.out.println("✅ HDFS 读取测试成功!");
            }

        } catch (Exception e) {
            System.err.println("❌ HDFS 测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            spark.stop();
            System.out.println("✅ 测试流程结束");
        }
    }
}