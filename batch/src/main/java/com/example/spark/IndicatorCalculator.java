package com.example.spark;

import org.apache.spark.sql.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import static org.apache.spark.sql.functions.*;

public class IndicatorCalculator {
//    private static final double LARGE_AMOUNT = 50000.0;
//    // 统一用“秒”为比较单位（分钟转为秒）
//    private static final long SEC_2MIN = 2 * 60L;
    private static final long SEC_5MIN = 90 * 60L;
    private static final double LARGE_AMOUNT = 50000.0;
    private static final int SEC_2MIN = 120; // 2分钟秒数
    private static final int DAYS_30_SEC = 30 * 24 * 3600; // 30天秒数


    private final SparkSession spark;

    public IndicatorCalculator(SparkSession spark) {
        this.spark = spark;
    }

     //指标A：账户近30天内大额转入后一个半小时内发起大额转出的次数
    public Dataset<Row> calcIndicatorA(Dataset<Row> transfers) {
        // 1️ 将 tx_time 转换为秒级 long
        Dataset<Row> t = transfers
                .withColumn("tx_time", col("tx_time").divide(1000).cast("long"))
                .withColumn("amount", col("amount").cast("double"));

        System.out.println("=== Step 1: Converted transfers ===");
        t.show();

        long currentTimeSec = System.currentTimeMillis() / 1000;
        long SEC_2MIN = 90 * 60 ;        // 2分钟
        long DAYS_30_SEC = 60 * 24 * 3600; // 30天   60天

        // 2️ 只考虑近30天交易
        Dataset<Row> recentTransfers = t
                .filter(col("tx_time").geq(currentTimeSec - DAYS_30_SEC));

        System.out.println("=== Step 2: Recent 30-day transfers ===");
        recentTransfers.show();
        System.out.println("Count: " + recentTransfers.count());

        // 3️ 入账大额
        Dataset<Row> incoming = recentTransfers
                .filter(col("amount").geq(LARGE_AMOUNT))
                .filter(col("to_account").gt(0))
                .select(
                        col("to_account").alias("account_id"),
                        col("tx_time").alias("incoming_time")
                );

        System.out.println("=== Step 3: Incoming large transfers ===");
        incoming.show();
        System.out.println("Count: " + incoming.count());

        // 4️ 转出大额
        Dataset<Row> outgoing = recentTransfers
                .filter(col("amount").geq(LARGE_AMOUNT))
                .filter(col("from_account").gt(0))
                .select(
                        col("from_account").alias("account_id"),
                        col("tx_time").alias("outgoing_time")
                );

        System.out.println("=== Step 4: Outgoing large transfers ===");
        outgoing.show();
        System.out.println("Count: " + outgoing.count());

        // 5️ 调试版 cross join，计算每笔入账和转出的时间差
        Dataset<Row> cross = incoming.crossJoin(outgoing)
                .withColumn("same_account", incoming.col("account_id").equalTo(outgoing.col("account_id")))
                .withColumn("time_diff", outgoing.col("outgoing_time").minus(incoming.col("incoming_time")));

        System.out.println("=== Step 5: Cross join debug ===");
        cross.show(50, false); // 显示前50条，完整时间差
        System.out.println("Count: " + cross.count());

        // 只保留同一账户，且时间差在 0~120 秒内
        Dataset<Row> suspicious = cross.filter(col("same_account").equalTo(true)
                        .and(col("time_diff").geq(0))
                        .and(col("time_diff").leq(SEC_2MIN)))
                .select(incoming.col("account_id"));

        System.out.println("=== Step 5: Suspicious transfers (filtered) ===");
        suspicious.show();
        System.out.println("Count: " + suspicious.count());

        // 6️ 按账户统计次数
        Dataset<Row> result = suspicious.groupBy("account_id")
                .agg(count(lit(1)).alias("indicator_A"))
                .filter(col("indicator_A").gt(0));

        System.out.println("=== Step 6: Result (indicator_A) ===");
        result.show();
        System.out.println("Count: " + result.count());

        return result;
    }




     //指标B：登录后一个半小时内是否发生大额转账
    public Dataset<Row> calcIndicatorB(Dataset<Row> logins, Dataset<Row> transfers) {
        Dataset<Row> tLarge = transfers
                .withColumn("tx_time", col("tx_time").divide(1000).cast("long"))
                .withColumn("amount", col("amount").cast("double"))
                .filter(col("amount").geq(LARGE_AMOUNT))
                .select(col("from_account").alias("account_id"), col("tx_time"));

        Dataset<Row> l = logins
                .withColumn("login_time", col("login_time").divide(1000).cast("long"))
                .select(col("account_id"), col("login_time"));

        Column cond = tLarge.col("account_id").equalTo(l.col("account_id"))
                .and(tLarge.col("tx_time").geq(l.col("login_time")))
                .and(tLarge.col("tx_time").leq(l.col("login_time").plus(SEC_5MIN)));

        Dataset<Row> joined = tLarge.join(l, cond, "inner").select(tLarge.col("account_id"));
        joined.groupBy("account_id").agg(count(lit(1)).alias("indicator_B")).show();
        return joined.groupBy("account_id").agg(count(lit(1)).alias("indicator_B"));
    }


    /**
     * 指标C：收款方近30天累计入账金额大于50000
     * 输出 indicator_C = 1 表示满足条件
     */
    public Dataset<Row> calcIndicatorC(Dataset<Row> transfers) {
        Dataset<Row> c = transfers
                .withColumn("amount", col("amount").cast("double"))
                .groupBy(col("to_account").alias("account_id"))
                .agg(sum("amount").alias("total_incoming"))
                .withColumn("indicator_C", when(col("total_incoming").gt(50000), lit(1)).otherwise(lit(0)));

        System.out.println("=== Indicator C ===");
        c.show(false);

        return c.select("account_id", "indicator_C");
    }

}
