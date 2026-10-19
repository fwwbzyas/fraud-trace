package com.example.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Banks 项目的 Spark 独立分析任务 (HDFS 适配版)
 * 读取 HDFS 数据，使用 Banks 项目的业务逻辑
 * * 使用方式：
 * 1. 独立运行：java -cp ... com.example.spark.MainJob
 * 2. 前置条件：确保 Docker Hadoop 集群已启动，且已通过 API 上传了 CSV 文件
 */
public class MainJob {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("🏦 Banks 项目 - Spark 独立分析任务 (HDFS Mode)");
        System.out.println("========================================");

        // 1. 关键：设置 Hadoop 用户，防止 "Permission denied"
        System.setProperty("HADOOP_USER_NAME", "root");

        // 2. 创建 Spark 会话 (使用我们改造过的 Factory，支持 HDFS)
        SparkSession spark = SparkSessionFactory.create("BanksFraudAnalysisJob");

        // 3. 配置 HDFS 连接参数 (替代原有的 JDBC 配置)
        String hdfsUri = "hdfs://localhost:8020";
        String hdfsBaseDir = "/user/banks/input";

        System.out.println("🔗 HDFS 连接信息:");
        System.out.println("   URI: " + hdfsUri);
        System.out.println("   目录: " + hdfsBaseDir);

        // 4. 初始化数据摄取服务 (传入 HDFS 配置)
        BanksIngestionService ingestion = new BanksIngestionService(spark, hdfsUri, hdfsBaseDir);
        BanksIndicatorCalculator calculator = new BanksIndicatorCalculator(spark);

        System.out.println("🚀 从 HDFS 加载 Banks 数据...");

        try {
            // 读取数据并触发 Action 操作以在 Spark UI 中显示任务
            Dataset<Row> accounts = ingestion.readAccounts();
            System.out.println("📊 账户数据 (总数: " + accounts.count() + " 条):");
            accounts.show(10);

            Dataset<Row> logins = ingestion.readLoginRecords();
            System.out.println("📊 登录记录数据 (总数: " + logins.count() + " 条):");
            logins.show(10);

            Dataset<Row> transfers = ingestion.readTransferRecords();
            System.out.println("📊 转账记录数据 (总数: " + transfers.count() + " 条):");
            transfers.show(10);

            // 缓存数据以便后续操作能在 UI 中显示
            accounts.cache();
            logins.cache();
            transfers.cache();

            System.out.println("💡 数据已缓存，可以在 Spark UI (http://localhost:4041) 查看任务详情");

            System.out.println("⚙️ 开始计算 Banks 指标...");

            // 创建指标参数配置
            ObjectMapper mapper = new ObjectMapper();

            // 指标A参数
            ObjectNode indicatorAParams = mapper.createObjectNode();
            indicatorAParams.put("large_amount_threshold", 50000.0);
            indicatorAParams.put("quick_transfer_minutes", 2);
            indicatorAParams.put("min_transfer_amount", 50000.0);

            // 指标B参数
            ObjectNode indicatorBParams = mapper.createObjectNode();
            indicatorBParams.put("large_amount_threshold", 50000.0);
            indicatorBParams.put("quick_operation_minutes", 5);

            // 指标C参数
            ObjectNode indicatorCParams = mapper.createObjectNode();
            indicatorCParams.put("min_single_amount", 0.01);

            int timeWindowDays = 30; // 30天时间窗口

            // 计算各个指标并触发更多 Spark 任务
            System.out.println("🔄 正在计算指标A - 大额快进快出...");
            Dataset<Row> indicatorA = calculator.calculateIndicator("INDICATOR_A", indicatorAParams, accounts, logins, transfers, timeWindowDays);
            System.out.println("📈 指标A 结果 (共 " + indicatorA.count() + " 条记录):");
            indicatorA.show();
            indicatorA.cache(); // 缓存结果

            System.out.println("🔄 正在计算指标B - 登录后快速转账...");
            Dataset<Row> indicatorB = calculator.calculateIndicator("INDICATOR_B", indicatorBParams, accounts, logins, transfers, timeWindowDays);
            System.out.println("📈 指标B 结果 (共 " + indicatorB.count() + " 条记录):");
            indicatorB.show();
            indicatorB.cache(); // 缓存结果

            System.out.println("🔄 正在计算指标C - 累计入账金额...");
            Dataset<Row> indicatorC = calculator.calculateIndicator("INDICATOR_C", indicatorCParams, accounts, logins, transfers, timeWindowDays);
            System.out.println("📈 指标C 结果 (共 " + indicatorC.count() + " 条记录):");
            indicatorC.show();
            indicatorC.cache(); // 缓存结果

            // 保持 Spark UI 一直显示
            System.out.println("✅ 分析任务全部完成！");
            System.out.println("🌐 请访问 http://localhost:4041 查看 Spark UI");
            System.out.println("💡 程序将保持运行，以便查看 UI。按 Ctrl+C 退出...");

            // 无限等待，直到用户手动停止程序
            while (true) {
                Thread.sleep(10000); // 每10秒检查一次
            }

        } catch (Exception e) {
            System.err.println("❌ 任务执行失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 正常情况下无法到达这里，除非异常退出
            if (spark != null) {
                spark.stop();
            }
        }
    }
}