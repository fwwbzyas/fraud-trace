package com.example.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.spark.sql.functions.*;

/**
 * 适配 banks 后端的数据摄取服务 (HDFS 版)
 * 核心改造：从读取 PostgreSQL 改为读取 HDFS 上的 CSV 文件
 */
public class BanksIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(BanksIngestionService.class);
    private final SparkSession spark;

    // HDFS 配置
    private final String hdfsUri;
    private final String hdfsBaseDir;

    /**
     * 构造函数
     * @param spark Spark会话
     * @param hdfsUri HDFS URI (例如: hdfs://localhost:8020)
     * @param hdfsBaseDir 数据存储基础目录 (例如: /user/banks/input)
     */
    public BanksIngestionService(SparkSession spark, String hdfsUri, String hdfsBaseDir) {
        this.spark = spark;
        this.hdfsUri = hdfsUri;
        this.hdfsBaseDir = hdfsBaseDir;
    }

    /** * 读取账户表 (account.csv)
     * 映射逻辑：id -> account_id
     **/
    public Dataset<Row> readAccounts() {
        String path = hdfsUri + hdfsBaseDir + "/account.csv";
        logger.info("📥 加载账户数据: {}", path);

        try {
            return spark.read()
                    .option("header", "true")       // 有表头
                    .option("inferSchema", "true")  // 自动推断类型
                    .option("quote", "\"")          // 处理双引号
                    .csv(path)
                    .select(
                            col("id").alias("account_id"),
                            col("account_no"),
                            col("holder_name"),
                            col("phone")
                    );
        } catch (Exception e) {
            logger.warn("⚠️ 无法加载账户 CSV (可能未上传): {}", e.getMessage());
            return spark.emptyDataFrame();
        }
    }

    /** * 读取登录记录表 (login_record.csv)
     * 清洗逻辑：处理 "28/10/2025 17:29:04.-1f" 这种特殊时间格式
     **/
    public Dataset<Row> readLoginRecords() {
        String path = hdfsUri + hdfsBaseDir + "/login_record.csv";
        logger.info("📥 加载登录数据: {}", path);

        try {
            return spark.read()
                    .option("header", "true")
                    .option("inferSchema", "true")
                    .option("quote", "\"")
                    .csv(path)
                    .withColumn("clean_time",
                            // 截掉 ".-1f" 后缀
                            substring_index(col("login_time"), ".", 1)
                    )
                    .select(
                            col("account_id").cast("string"),
                            // 解析格式: d/M/y H:m:s (例如 28/10/2025 17:29:04)
                            unix_timestamp(col("clean_time"), "d/M/y H:m:s")
                                    .multiply(1000).alias("login_time"),
                            col("login_ip").alias("ip")
                    );
        } catch (Exception e) {
            logger.warn("⚠️ 无法加载登录 CSV: {}", e.getMessage());
            return spark.emptyDataFrame();
        }
    }

    /** * 读取转账记录表 (transfer_records.csv)
     * 修复说明：移除 .alias("to_account")，保留原始列名 to_account_id，
     * 解决 UNRESOLVED_COLUMN 错误。
     **/
    public Dataset<Row> readTransferRecords() {
        String path = hdfsUri + hdfsBaseDir + "/transfer_records.csv";
        logger.info("📥 加载转账数据: {}", path);

        try {
            return spark.read()
                    .option("header", "true")
                    .option("inferSchema", "true")
                    .option("quote", "\"")
                    .csv(path)
                    .withColumn("clean_time",
                            substring_index(col("transfer_time"), ".", 1)
                    )
                    .select(
                            // ✅ 修复点：不再重命名为 from_account，保持 from_account_id
                            col("from_account_id").cast("string"),
                            // ✅ 修复点：不再重命名为 to_account，保持 to_account_id (解决报错的关键)
                            col("to_account_id").cast("string"),

                            unix_timestamp(col("clean_time"), "d/M/y H:m:s")
                                    .multiply(1000).alias("tx_time"),
                            col("amount").cast("double")
                    );
        } catch (Exception e) {
            logger.warn("⚠️ 无法加载转账 CSV: {}", e.getMessage());
            return spark.emptyDataFrame();
        }
    }
}