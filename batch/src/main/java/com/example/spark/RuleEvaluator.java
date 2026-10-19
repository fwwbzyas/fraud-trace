package com.example.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import static org.apache.spark.sql.functions.*;

public class RuleEvaluator {

    public Dataset<Row> evaluate(Dataset<Row> a, Dataset<Row> b, Dataset<Row> c, double largeAmountThreshold) {
        System.out.println("=== Dataset Column Info ===");
        System.out.println("Dataset A columns: " + java.util.Arrays.toString(a.columns()));
        System.out.println("Dataset B columns: " + java.util.Arrays.toString(b.columns()));
        System.out.println("Dataset C columns: " + java.util.Arrays.toString(c.columns()));

        String accountIdColA = a.columns()[0];
        String accountIdColB = b.columns()[0];
        String accountIdColC = c.columns()[0];

        System.out.println("Account ID columns - A: " + accountIdColA + ", B: " + accountIdColB + ", C: " + accountIdColC);

        // 重命名 account_id 列，保证统一
        Dataset<Row> aRenamed = a.withColumnRenamed(accountIdColA, "account_id");
        Dataset<Row> bRenamed = b.withColumnRenamed(accountIdColB, "account_id");
        Dataset<Row> cRenamed = c.withColumnRenamed(accountIdColC, "account_id");

        // -----------------------------
        // 1️⃣ 每个指标先筛选出满足条件的账户
        // -----------------------------
        Dataset<Row> aFiltered = aRenamed.filter(col("indicator_A").gt(0)).select("account_id");
        System.out.println("Accounts passing indicator A:");
        aFiltered.show(false);
        System.out.println("Count: " + aFiltered.count());

        Dataset<Row> bFiltered = bRenamed.filter(col("indicator_B").gt(0)).select("account_id");
        System.out.println("Accounts passing indicator B:");
        bFiltered.show(false);
        System.out.println("Count: " + bFiltered.count());

        // ✅ C 现在 indicator_C >= largeAmountThreshold 表示满足累计入账金额条件
        Dataset<Row> cFiltered = cRenamed.filter(col("indicator_C").geq(largeAmountThreshold)).select("account_id");
        System.out.println("Accounts passing indicator C (>= " + largeAmountThreshold + "):");
        cFiltered.show(false);
        System.out.println("Count: " + cFiltered.count());

        // -----------------------------
        // 2️⃣ 取交集：A & B
        // -----------------------------
        Dataset<Row> abIntersect = aFiltered.join(bFiltered, "account_id");
        System.out.println("Accounts passing A & B:");
        abIntersect.show(false);
        System.out.println("Count: " + abIntersect.count());

        // -----------------------------
        // 3️⃣ 再和 C 条件取交集
        // -----------------------------
        Dataset<Row> abcIntersect = abIntersect.join(cFiltered, "account_id");
        System.out.println("Accounts passing A & B & C:");
        abcIntersect.show(false);
        System.out.println("Count: " + abcIntersect.count());

        // -----------------------------
        // 4️⃣ 回补指标值
        // -----------------------------
        Dataset<Row> result = abcIntersect
                .join(aRenamed, "account_id", "left")
                .join(bRenamed, "account_id", "left")
                .join(cRenamed, "account_id", "left")
                .select("account_id", "indicator_A", "indicator_B", "indicator_C");

        System.out.println("=== Final suspicious accounts ===");
        result.show(false);

        return result;
    }
}
