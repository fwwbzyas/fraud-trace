package com.example.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

/**
 * Banks 项目的结果导出器
 * 支持写入 PostgreSQL 数据库的 risk_result 表
 */
public class ResultExporter {

    /**
     * 写入 Banks 项目的 PostgreSQL 数据库
     */
    public void writeToPostgreSQL(Dataset<Row> result, SparkSession spark, Long taskId) {
        System.out.println("💾 准备写入 Banks PostgreSQL 数据库...");
        
        // 格式化 DataFrame，映射到 Banks 项目的 risk_result 表字段
        Dataset<Row> formatted = result
                .withColumn("task_id", functions.lit(taskId))  // 任务ID
                .withColumn("account_role", functions.lit(2))  // 可疑账户角色
                .withColumn("risk_score", functions.lit(85.0))  // 风险评分
                .withColumn(
                        "matched_indicators",
                        functions.to_json(functions.struct(
                                functions.col("indicator_A").alias("A"),
                                functions.col("indicator_B").alias("B"), 
                                functions.col("indicator_C").alias("C")
                        ))
                )
                .withColumn("matched_rules", functions.lit("Banks规则: 大额快进快出 + 登录后快速转账"))
                .withColumn("create_time", functions.current_timestamp())
                .select(
                        "task_id",
                        "account_id", 
                        "account_role",
                        "risk_score",
                        "matched_indicators",
                        "matched_rules",
                        "create_time"
                );

        // 输出检查
        System.out.println("📊 格式化后的结果数据:");
        formatted.show(false);
        formatted.printSchema();

        // 写入 Banks PostgreSQL risk_result 表
        formatted.write()
                .mode("append")
                .format("jdbc")
                .option("url", DatabaseEnvironment.value("DB_URL", "jdbc:postgresql://localhost:5432/banksys"))
                .option("dbtable", "risk_result")
                .option("user", DatabaseEnvironment.value("DB_USER", "postgres"))
                .option("password", DatabaseEnvironment.required("DB_PASSWORD"))
                .option("driver", "org.postgresql.Driver")
                .save();

        System.out.println("✅ 风险结果已成功写入 Banks PostgreSQL 表: risk_result");
    }

    /**
     * 保留原有的 ClickHouse 写入方法（兼容性）
     */
    public void writeToClickHouse(Dataset<Row> result, SparkSession spark) {
        // 格式化 DataFrame，映射到 ClickHouse risk_results 表字段
        Dataset<Row> formatted = result
                .withColumn("risk_id", functions.monotonically_increasing_id())  // 生成唯一ID
                .withColumn("task_id", functions.lit(1))  // 当前任务ID
                .withColumn("account_role", functions.lit(2))  // 可疑账户
                .withColumn(
                        "matched_indicators",
                        functions.concat_ws(", ",
                                functions.concat(functions.lit("A="), result.col("indicator_A")),
                                functions.concat(functions.lit("B="), result.col("indicator_B")),
                                functions.concat(functions.lit("C="), result.col("indicator_C"))
                        )
                )
                .withColumn("matched_rules", functions.lit("Rule: A&B triggered, C=0"))
                .select(
                        "risk_id",
                        "task_id",
                        "account_id",
                        "account_role",
                        "matched_indicators",
                        "matched_rules"
                );

        // 输出检查
        formatted.show(false);
        formatted.printSchema();

        // 写入 ClickHouse risk_results 表
        formatted.write()
                .mode("append")
                .format("jdbc")
                .option("url", DatabaseEnvironment.value("CLICKHOUSE_URL", "jdbc:clickhouse://localhost:8123/default"))
                .option("dbtable", "risk_results")
                .option("user", "default")
                .option("password", DatabaseEnvironment.required("CLICKHOUSE_PASSWORD"))
                .option("driver", "com.clickhouse.jdbc.ClickHouseDriver")
                .save();

        System.out.println("✅ Risk results successfully written to ClickHouse table: risk_results");
    }
}
