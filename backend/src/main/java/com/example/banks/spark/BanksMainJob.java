package com.example.banks.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * 完全模仿 fraudanalysis 的 MainJob
 * 纯 Java 应用，不依赖 Spring
 */
public class BanksMainJob {

    public static void main(String[] args) {
        System.out.println("🚀 开始 Banks 欺诈分析任务（模仿 fraudanalysis）");

        // ✅ 修复点：这里补充了第二个参数 "local[*]"
        // 因为 BanksSparkSessionFactory.create 现在需要 (appName, masterUrl) 两个参数
        SparkSession spark = BanksSparkSessionFactory.create("BanksFraudAnalysisJob", "local[*]");

        try {
            // 2. 创建服务 - 模仿 fraudanalysis 的结构
            // 注意：如果 BanksIngestionService 也依赖 HDFS，这里可能需要进一步改造
            // 但目前先保证 MainJob 能跑通
            BanksIngestionService ingestion = new BanksIngestionService(spark);

            System.out.println("🚀 从数据源加载数据...");

            // 3. 加载数据并立即显示 - 这会触发 Spark UI 显示内容
            Dataset<Row> accounts = ingestion.readAccounts();
            System.out.println("📊 账户数据:");
            accounts.show(5); // 显示前5条，这会在 Spark UI 中显示任务

            Dataset<Row> logins = ingestion.readLogins();
            System.out.println("📊 登录数据:");
            logins.show(5);

            Dataset<Row> transfers = ingestion.readTransfers();
            System.out.println("📊 转账数据:");
            transfers.show(5);

            System.out.println("⚙️ 开始计算指标...");

            // 4. 执行一些计算任务 - 让 Spark UI 有内容显示
            Dataset<Row> largeTransfers = transfers.filter("amount > 10000");
            System.out.println("💰 大额转账统计:");
            largeTransfers.show(10);

            // 5. 统计分析
            long totalAccounts = accounts.count();
            long totalLogins = logins.count();
            long totalTransfers = transfers.count();
            long largeTransferCount = largeTransfers.count();

            System.out.println("📈 统计结果:");
            System.out.println("总账户数: " + totalAccounts);
            System.out.println("总登录数: " + totalLogins);
            System.out.println("总转账数: " + totalTransfers);
            System.out.println("大额转账数: " + largeTransferCount);

            System.out.println("🌐 Spark UI 访问地址: http://localhost:4040");
            System.out.println("💡 任务正在运行，现在可以访问 Spark UI 查看详情");

            // 6. 保持运行一段时间，让你能看到 Spark UI
            System.out.println("⏰ 保持运行 30 秒，请访问 Spark UI...");
            Thread.sleep(30000);

        } catch (Exception e) {
            System.err.println("❌ 任务执行失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 7. 关闭 SparkSession
            spark.stop();
            System.out.println("✅ 分析任务完成");
        }
    }
}