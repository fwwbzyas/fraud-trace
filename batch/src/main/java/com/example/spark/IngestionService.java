package com.example.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import java.util.Properties;

public class IngestionService {
    private final SparkSession spark;
    private final String jdbcUrl;
    private final Properties connectionProps;

    public IngestionService(SparkSession spark) {
        this.spark = spark;

        // ✅ 配置 ClickHouse 连接参数
        this.jdbcUrl = DatabaseEnvironment.value("CLICKHOUSE_URL", "jdbc:clickhouse://localhost:8123/default");
        this.connectionProps = new Properties();
        connectionProps.put("user", "default");
        connectionProps.put("password", DatabaseEnvironment.required("CLICKHOUSE_PASSWORD"));
        connectionProps.put("driver", "com.clickhouse.jdbc.ClickHouseDriver");
    }

    /** 读取登录记录表 **/
    public Dataset<Row> readLogin() {
        return spark.read()
                .jdbc(jdbcUrl, "login_record", connectionProps)
                .selectExpr(
                        "CAST(account_id AS STRING) AS account_id",
                        "unix_timestamp(login_time) * 1000 AS login_time",
                        "login_ip AS ip"
                );
    }

    /** 读取转账记录表 **/
    public Dataset<Row> readTransfer() {
        return spark.read()
                .jdbc(jdbcUrl, "transfer_records", connectionProps)
                .selectExpr(
                        "CAST(from_account_id AS STRING) AS from_account",
                        "CAST(to_account_id AS STRING) AS to_account",
                        "unix_timestamp(transfer_time) * 1000 AS tx_time",
                        "CAST(amount AS DOUBLE) AS amount"
                );
    }
}
