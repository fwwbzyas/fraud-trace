package com.example.banks.controller;

import com.example.banks.util.DatabaseEnvironment;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 测试 Spark UI 是否正常工作
 */
@RestController
@RequestMapping("/spark-ui-test")
public class SparkUITestController {
    
    private static final Logger logger = LoggerFactory.getLogger(SparkUITestController.class);
    
    @Autowired
    private SparkSession sparkSession;
    
    @GetMapping("/simple")
    public String testSimpleSparkJob() {
        logger.info("🚀 开始简单 Spark 任务测试");
        
        try {
            // 创建简单的测试数据
            List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            Dataset<Row> df = sparkSession.createDataFrame(data, Integer.class).toDF("number");
            
            logger.info("📊 创建测试数据集");
            df.show();
            
            // 执行一些计算 - 这会在 Spark UI 中显示
            Dataset<Row> result = df.filter(df.col("number").gt(5));
            logger.info("📊 过滤结果 (number > 5):");
            result.show();
            
            long count = result.count();
            logger.info("✅ 过滤后记录数: {}", count);
            
            // 执行聚合操作
            Dataset<Row> sum = df.agg(org.apache.spark.sql.functions.sum("number").alias("total"));
            logger.info("📊 求和结果:");
            sum.show();
            
            logger.info("🎉 Spark 任务执行完成！");
            logger.info("🌐 请访问 http://localhost:4040 查看 Spark UI");
            
            return "Spark 任务执行成功！记录数: " + count + "，请访问 http://localhost:4040 查看 Spark UI";
            
        } catch (Exception e) {
            logger.error("❌ Spark 任务执行失败", e);
            return "Spark 任务执行失败: " + e.getMessage();
        }
    }
    
    @GetMapping("/database")
    public String testDatabaseSparkJob() {
        logger.info("🚀 开始数据库 Spark 任务测试");
        
        try {
            // 测试数据库连接
            java.util.Properties connectionProps = new java.util.Properties();
            connectionProps.put("user", DatabaseEnvironment.value("DB_USER", "postgres"));
            connectionProps.put("password", DatabaseEnvironment.required("DB_PASSWORD"));
            connectionProps.put("driver", "org.postgresql.Driver");
            
            String jdbcUrl = DatabaseEnvironment.value("DB_URL", "jdbc:postgresql://localhost:5432/banksys");
            
            logger.info("🔗 连接数据库: {}", jdbcUrl);
            
            // 读取账户数据
            Dataset<Row> accountsDF = sparkSession.read()
                    .jdbc(jdbcUrl, "account", connectionProps)
                    .limit(10); // 只取前10条
            
            logger.info("📊 账户数据 (前10条):");
            accountsDF.show();
            
            // 执行计算
            long totalAccounts = accountsDF.count();
            logger.info("✅ 总账户数: {}", totalAccounts);
            
            logger.info("🎉 数据库 Spark 任务执行完成！");
            logger.info("🌐 请访问 http://localhost:4040 查看 Spark UI");
            
            return "数据库 Spark 任务执行成功！总账户数: " + totalAccounts + "，请访问 http://localhost:4040 查看 Spark UI";
            
        } catch (Exception e) {
            logger.error("❌ 数据库 Spark 任务执行失败", e);
            return "数据库 Spark 任务执行失败: " + e.getMessage();
        }
    }
    
    @GetMapping("/info")
    public String getSparkInfo() {
        try {
            String info = String.format(
                "Spark 版本: %s\n" +
                "应用名称: %s\n" +
                "Spark UI: http://localhost:4040\n" +
                "Master: %s",
                sparkSession.version(),
                sparkSession.sparkContext().appName(),
                sparkSession.sparkContext().master()
            );
            
            logger.info("📋 Spark 信息:\n{}", info);
            return info;
            
        } catch (Exception e) {
            logger.error("❌ 获取 Spark 信息失败", e);
            return "获取 Spark 信息失败: " + e.getMessage();
        }
    }
}
