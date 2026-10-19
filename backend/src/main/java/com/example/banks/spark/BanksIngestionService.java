package com.example.banks.spark;

import com.example.banks.util.DatabaseEnvironment;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import java.util.Properties;

/**
 * 完全模仿 fraudanalysis 的 IngestionService
 * 但连接 PostgreSQL 而不是 ClickHouse
 */
public class BanksIngestionService {
    private final SparkSession spark;
    private final String jdbcUrl;
    private final Properties connectionProps;

    public BanksIngestionService(SparkSession spark) {
        this.spark = spark;

        // ✅ 配置 PostgreSQL 连接参数
        this.jdbcUrl = DatabaseEnvironment.value("DB_URL", "jdbc:postgresql://localhost:5432/banksys");
        this.connectionProps = new Properties();
        connectionProps.put("user", DatabaseEnvironment.value("DB_USER", "postgres"));
        connectionProps.put("password", DatabaseEnvironment.required("DB_PASSWORD"));
        connectionProps.put("driver", "org.postgresql.Driver");
    }

    /** 读取账户表 **/
    public Dataset<Row> readAccounts() {
        return spark.read()
                .jdbc(jdbcUrl, "account", connectionProps)
                .selectExpr(
                        "account_id",
                        "account_no",
                        "holder_name",
                        "phone"
                );
    }

    /** 读取登录记录表 **/
    public Dataset<Row> readLogins() {
        return spark.read()
                .jdbc(jdbcUrl, "login_record", connectionProps)
                .selectExpr(
                        "CAST(account_id AS STRING) AS account_id",
                        "login_time",
                        "login_ip AS ip",
                        "device_info"
                );
    }

    /** 读取转账记录表 **/
    public Dataset<Row> readTransfers() {
        return spark.read()
                .jdbc(jdbcUrl, "transfer_records", connectionProps)
                .selectExpr(
                        "transfer_id",
                        "CAST(from_account_id AS STRING) AS from_account",
                        "CAST(to_account_id AS STRING) AS to_account",
                        "transfer_time AS tx_time",
                        "CAST(amount AS DOUBLE) AS amount",
                        "transfer_type",
                        "status"
                );
    }
}
