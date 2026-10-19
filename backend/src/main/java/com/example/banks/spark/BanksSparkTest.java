package com.example.banks.spark;

import com.example.banks.util.DatabaseEnvironment;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import java.util.Properties;

/**
 * 完全模仿 fraudanalysis 项目的简单 Spark 测试
 * 不依赖 Spring，直接使用 Spark
 */
public class BanksSparkTest {
    
    public static void main(String[] args) {
        System.out.println("🚀 开始 Banks Spark 测试（模仿 fraudanalysis）");
        
        // 设置 JVM 参数解决模块访问问题
        System.setProperty("spark.driver.extraJavaOptions", 
            "--add-opens=java.base/java.lang=ALL-UNNAMED " +
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED " +
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED " +
            "--add-opens=java.base/java.io=ALL-UNNAMED " +
            "--add-opens=java.base/java.net=ALL-UNNAMED " +
            "--add-opens=java.base/java.nio=ALL-UNNAMED " +
            "--add-opens=java.base/java.util=ALL-UNNAMED " +
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED " +
            "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED " +
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED " +
            "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED " +
            "--add-opens=java.base/sun.security.action=ALL-UNNAMED " +
            "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED " +
            "--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED");
        
        // 1. 创建 SparkSession - 完全模仿 fraudanalysis
        SparkSession spark = createSparkSession();
        
        try {
            // 2. 测试数据库连接和数据加载
            testDataLoading(spark);
            
            System.out.println("✅ Banks Spark 测试完成");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 3. 关闭 SparkSession
            spark.stop();
        }
    }
    
    /**
     * 创建 SparkSession - 完全模仿 fraudanalysis 的 SparkSessionFactory
     */
    private static SparkSession createSparkSession() {
        System.out.println("📋 创建 SparkSession...");
        
        return SparkSession.builder()
                .appName("BanksFraudDetectionTest")
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
                // ✅ 启用 UI（像 fraudanalysis 一样）
                .config("spark.ui.enabled", "true")
                .config("spark.ui.port", "4042")
                .getOrCreate();
    }
    
    /**
     * 测试数据加载 - 模仿 fraudanalysis 的 IngestionService
     */
    private static void testDataLoading(SparkSession spark) {
        System.out.println("📊 测试数据加载...");
        
        // PostgreSQL 连接配置
        String jdbcUrl = DatabaseEnvironment.value("DB_URL", "jdbc:postgresql://localhost:5432/banksys");
        Properties connectionProps = new Properties();
        connectionProps.put("user", DatabaseEnvironment.value("DB_USER", "postgres"));
        connectionProps.put("password", DatabaseEnvironment.required("DB_PASSWORD"));
        connectionProps.put("driver", "org.postgresql.Driver");
        
        try {
            // 测试加载账户数据
            System.out.println("🔗 连接数据库: " + jdbcUrl);
            Dataset<Row> accountsDF = spark.read()
                    .jdbc(jdbcUrl, "account", connectionProps)
                    .selectExpr(
                            "account_id",
                            "account_no",
                            "holder_name"
                    );
            
            long accountCount = accountsDF.count();
            System.out.println("✅ 账户数据加载成功: " + accountCount + " 条记录");
            
            // 显示前5条记录
            System.out.println("📋 前5条账户记录:");
            accountsDF.show(5);
            
            // 测试加载登录记录
            Dataset<Row> loginDF = spark.read()
                    .jdbc(jdbcUrl, "login_record", connectionProps)
                    .selectExpr(
                            "CAST(account_id AS STRING) AS account_id",
                            "login_time",
                            "login_ip AS ip"
                    );
            
            long loginCount = loginDF.count();
            System.out.println("✅ 登录记录加载成功: " + loginCount + " 条记录");
            
            // 测试加载转账记录
            Dataset<Row> transferDF = spark.read()
                    .jdbc(jdbcUrl, "transfer_records", connectionProps)
                    .selectExpr(
                            "CAST(from_account_id AS STRING) AS from_account",
                            "CAST(to_account_id AS STRING) AS to_account",
                            "transfer_time AS tx_time",
                            "CAST(amount AS DOUBLE) AS amount"
                    );
            
            long transferCount = transferDF.count();
            System.out.println("✅ 转账记录加载成功: " + transferCount + " 条记录");
            
            System.out.println("🎉 所有数据加载测试通过！");
            System.out.println("🌐 Spark UI 访问地址: http://localhost:4042");
            
        } catch (Exception e) {
            System.err.println("❌ 数据加载失败: " + e.getMessage());
            throw e;
        }
    }
}
