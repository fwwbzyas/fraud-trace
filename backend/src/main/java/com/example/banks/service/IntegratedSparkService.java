package com.example.banks.service;

import com.example.banks.entity.AnalysisIndicator;
import com.example.banks.engine.SqlBasedRuleEngine;
import com.example.banks.spark.BanksSparkSessionFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.functions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 集成的 Spark 服务 (HDFS 版)
 * 核心改造：
 * 1. 增加 uploadFileToHdfs 方法
 * 2. load数据方法改为从 HDFS 读取 CSV
 * 3. 移除 JDBC 依赖，保留完整的指标计算逻辑
 */
@Service
public class IntegratedSparkService {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedSparkService.class);

    private SparkSession sparkSession;

    // ==========================================
    // 注入新配置 (源自 application.properties)
    // ==========================================
    @Value("${spark.master}")
    private String sparkMaster;

    @Value("${hadoop.fs.defaultFS}")
    private String hdfsUri;

    @Value("${spark.hdfs.base.dir}")
    private String hdfsBaseDir;

    @Value("${hadoop.user.name}")
    private String hadoopUser;

    /**
     * 初始化 Spark (支持 HDFS + 本地运行)
     */
    public void initializeSpark() {
        // 关键：设置 HDFS 用户，防止 Permission Denied
        System.setProperty("HADOOP_USER_NAME", hadoopUser);

        logger.info("========================================");
        logger.info("🚀 初始化集成 Spark 服务 (HDFS模式)");
        logger.info("🌊 HDFS 地址: {}", hdfsUri);
        logger.info("📂 数据目录: {}", hdfsBaseDir);
        logger.info("========================================");

        try {
            if (SparkSession.getActiveSession().isDefined()) {
                this.sparkSession = SparkSession.getActiveSession().get();
                logger.info("📋 复用现有 SparkSession");
                return;
            }

            // ✅ 调用 BanksSparkSessionFactory 创建会话
            this.sparkSession = BanksSparkSessionFactory.create("Banks-HDFS-Analysis", sparkMaster);

            logger.info("✅ Spark 服务启动成功！版本: {}", sparkSession.version());

        } catch (Exception e) {
            logger.error("❌ Spark 启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("Spark 服务启动失败", e);
        }
    }

    /**
     * ✨【核心新增】上传文件到 HDFS
     * @param file 前端上传的文件
     * @param fileName 目标文件名 (如 transfer_records.csv)
     * @return HDFS 完整路径
     */
    public String uploadFileToHdfs(MultipartFile file, String fileName) throws IOException {
        // 确保 Spark 已初始化 (为了加载 Hadoop 配置)
        if (sparkSession == null) {
            initializeSpark();
        }

        String targetPath = hdfsBaseDir + "/" + fileName;
        logger.info("📤 正在上传文件到 HDFS: {}", targetPath);

        Configuration conf = new Configuration();
        conf.set("dfs.client.use.datanode.hostname", "true");
        conf.set("fs.defaultFS", hdfsUri);
        // 优化：禁用校验和，加快上传速度 (可选)
        // conf.set("fs.hdfs.impl.disable.cache", "true");

        try (FileSystem fs = FileSystem.get(URI.create(hdfsUri), conf, hadoopUser)) {
            Path path = new Path(targetPath);
            Path dir = new Path(hdfsBaseDir);

            // 1. 确保目录存在
            if (!fs.exists(dir)) {
                fs.mkdirs(dir);
                logger.info("📁 创建 HDFS 目录: {}", hdfsBaseDir);
            }

            // 2. 如果文件存在先删除 (覆盖模式)
            if (fs.exists(path)) {
                fs.delete(path, false);
            }

            // 3. 流式写入
            try (var out = fs.create(path); var in = file.getInputStream()) {
                in.transferTo(out);
            }
            logger.info("✅ HDFS 上传成功！");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HDFS 上传中断", e);
        }

        return targetPath;
    }

    @PreDestroy
    public void shutdownSpark() {
        logger.info("🔄 关闭集成 Spark 服务...");
        if (sparkSession != null) {
            sparkSession.stop();
            logger.info("✅ 集成 Spark 服务已关闭");
        }
    }

    /**
     * 执行分离规则分析 - 使用 HDFS 数据源
     */
    public SqlBasedRuleEngine.CombinedRuleResult executeSeparateRules(
            String victimRule,
            String suspectRule,
            List<AnalysisIndicator> victimIndicators,
            List<AnalysisIndicator> suspectIndicators,
            Integer taskTimeRange,
            Integer indicatorTimeWindow) {

        if (sparkSession == null) initializeSpark();

        logger.info("🚀 集成 Spark 执行分离规则分析 (HDFS源)");
        logger.info("📊 受害人规则: {}, 可疑分子规则: {}", victimRule, suspectRule);

        SqlBasedRuleEngine.CombinedRuleResult combinedResult = new SqlBasedRuleEngine.CombinedRuleResult();

        try {
            // 1. 从 HDFS 加载数据
            logger.info("📡 从 HDFS 加载 CSV 数据...");
            Dataset<Row> accountsDF = loadAccountData();
            Dataset<Row> loginRecordsDF = loadLoginRecordData();
            Dataset<Row> transferRecordsDF = loadTransferRecordData();

            logger.info("✅ 数据加载完成，准备执行分析...");

            // 2. 执行受害人规则（如果有）
            if (victimRule != null && !victimRule.trim().isEmpty() &&
                    victimIndicators != null && !victimIndicators.isEmpty()) {

                logger.info("🔍 执行受害人规则分析...");
                SparkRuleResult victimResult = executeSparkRule(
                        victimRule, victimIndicators, accountsDF, loginRecordsDF, transferRecordsDF, indicatorTimeWindow);

                combinedResult.setVictimResult(convertToSqlRuleResult(victimResult));
                logger.info("✅ 受害人分析完成，匹配账户数: {}", victimResult.getMatchedAccounts().size());
            }

            // 3. 执行可疑分子规则（如果有）
            if (suspectRule != null && !suspectRule.trim().isEmpty() &&
                    suspectIndicators != null && !suspectIndicators.isEmpty()) {

                logger.info("🔍 执行可疑分子规则分析...");
                SparkRuleResult suspectResult = executeSparkRule(
                        suspectRule, suspectIndicators, accountsDF, loginRecordsDF, transferRecordsDF, indicatorTimeWindow);

                combinedResult.setSuspectResult(convertToSqlRuleResult(suspectResult));
                logger.info("✅ 可疑分子分析完成，匹配账户数: {}", suspectResult.getMatchedAccounts().size());
            }

            // 4. 清理缓存 (可选)
            // accountsDF.unpersist();

            // 5. 生成统计信息
            combinedResult.generateStatistics();

            logger.info("🎉 HDFS 分析任务完成 - 受害人: {}, 可疑分子: {}",
                    combinedResult.getVictimCount(), combinedResult.getSuspectCount());

            return combinedResult;

        } catch (Exception e) {
            logger.error("❌ 分析失败: {}", e.getMessage(), e);
            throw new RuntimeException("分析失败: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // 📂 数据加载方法改造 (JDBC -> CSV on HDFS)
    // ==========================================

    private Dataset<Row> loadAccountData() {
        // ⚠️ 注意：文件名是 account.csv
        String path = hdfsUri + hdfsBaseDir + "/account.csv";
        logger.info("📥 加载账户数据 (带表头): {}", path);

        try {
            return sparkSession.read()
                    .option("header", "true")       // ✅ 这次有表头
                    .option("inferSchema", "true")
                    .option("quote", "\"")          // 处理双引号
                    .csv(path)
                    .select(
                            col("id").alias("account_id"),
                            col("account_no"),
                            col("holder_name"),
                            col("phone")
                    );
        } catch (Exception e) {
            logger.warn("⚠️ 无法加载账户 CSV: {}", e.getMessage());
            return sparkSession.emptyDataFrame();
        }
    }

    private Dataset<Row> loadLoginRecordData() {
        // ⚠️ 注意：文件名是 login_record.csv (没有s)
        String path = hdfsUri + hdfsBaseDir + "/login_record.csv";
        logger.info("📥 加载登录数据 (清洗时间格式): {}", path);

        try {
            return sparkSession.read()
                    .option("header", "true")
                    .option("inferSchema", "true")
                    .option("quote", "\"")
                    .csv(path)
                    .withColumn("clean_time",
                            // 截掉 ".-1f"
                            substring_index(col("login_time"), ".", 1)
                    )
                    .select(
                            col("account_id").cast("string"),
                            unix_timestamp(col("clean_time"), "d/M/y H:m:s")
                                    .multiply(1000).alias("login_time"),
                            col("login_ip").alias("ip")
                    );
        } catch (Exception e) {
            logger.warn("⚠️ 无法加载登录 CSV: {}", e.getMessage());
            return sparkSession.emptyDataFrame();
        }
    }
    private Dataset<Row> loadTransferRecordData() {
        // ⚠️ 注意：文件名是 transfer_records.csv
        String path = hdfsUri + hdfsBaseDir + "/transfer_records.csv";
        logger.info("📥 加载转账数据 (清洗时间格式): {}", path);

        try {
            return sparkSession.read()
                    .option("header", "true")
                    .option("inferSchema", "true")
                    .option("quote", "\"")
                    .csv(path)
                    .withColumn("clean_time",
                            // 使用 substring_index 截掉 ".-1f" 后面的内容
                            substring_index(col("transfer_time"), ".", 1)
                    )
                    .select(
                            col("from_account_id").cast("string").alias("from_account"),
                            col("to_account_id").cast("string").alias("to_account"),
                            // 解析格式: dd/MM/yyyy HH:mm:ss
                            unix_timestamp(col("clean_time"), "d/M/y H:m:s")
                                    .multiply(1000).alias("tx_time"),
                            col("amount").cast("double")
                    );
        } catch (Exception e) {
            logger.warn("⚠️ 无法加载转账 CSV: {}", e.getMessage());
            return sparkSession.emptyDataFrame();
        }
    }
    // ==========================================
    // 📉 计算逻辑与辅助方法 (保持原样)
    // ==========================================

    /**
     * 执行单个规则的 Spark 分析
     */
    private SparkRuleResult executeSparkRule(
            String ruleExpression,
            List<AnalysisIndicator> indicators,
            Dataset<Row> accountsDF,
            Dataset<Row> loginRecordsDF,
            Dataset<Row> transferRecordsDF,
            Integer indicatorTimeWindowDays) {

        logger.info("🔍 执行规则: {}, 窗口: {}天", ruleExpression, indicatorTimeWindowDays);

        List<RuleCondition> conditions = parseRuleExpression(ruleExpression);
        Map<String, Map<Long, Double>> indicatorResultsMap = new HashMap<>();

        for (AnalysisIndicator indicator : indicators) {
            String indicatorCode = indicator.getIndicatorCode();
            logger.info("📊 计算指标: {}", indicatorCode);

            // 计算指标原始结果
            Map<Long, Double> indicatorResults = calculateIndicatorWithSpark(
                    indicator, accountsDF, loginRecordsDF, transferRecordsDF, indicatorTimeWindowDays);

            // 查找该指标对应的规则条件
            RuleCondition condition = conditions.stream()
                    .filter(c -> c.getIndicatorCode().equals(indicatorCode))
                    .findFirst()
                    .orElse(null);

            // 应用条件过滤
            Map<Long, Double> filteredResults;
            if (condition != null) {
                filteredResults = filterByCondition(indicatorResults, condition);
                logger.info("✅ 指标 {} 应用条件 {} {} - 结果数: {}",
                        indicatorCode, condition.getOperator(), condition.getThreshold(), filteredResults.size());
            } else {
                filteredResults = indicatorResults;
            }

            indicatorResultsMap.put(indicatorCode, filteredResults);
        }

        SparkRuleResult result = new SparkRuleResult();
        result.setRuleExpression(ruleExpression);

        if (ruleExpression.contains("AND")) {
            result.setMatchedAccounts(intersectResults(indicatorResultsMap));
        } else {
            result.setMatchedAccounts(unionResults(indicatorResultsMap));
        }

        return result;
    }

    /**
     * 计算单个指标
     */
    private Map<Long, Double> calculateIndicatorWithSpark(
            AnalysisIndicator indicator,
            Dataset<Row> accountsDF,
            Dataset<Row> loginRecordsDF,
            Dataset<Row> transferRecordsDF,
            Integer timeWindowDays) {

        Map<Long, Double> results = new HashMap<>();
        String indicatorCode = indicator.getIndicatorCode();
        JsonNode paramConfig = indicator.getParamConfig();

        try {
            Dataset<Row> resultDF = null;

            if ("INDICATOR_A".equals(indicatorCode)) {
                resultDF = calculateIndicatorAWithSpark(paramConfig, timeWindowDays, transferRecordsDF);
            } else if ("INDICATOR_B".equals(indicatorCode)) {
                resultDF = calculateIndicatorBWithSpark(paramConfig, timeWindowDays, loginRecordsDF, transferRecordsDF);
            } else if ("INDICATOR_C".equals(indicatorCode)) {
                resultDF = calculateIndicatorCWithSpark(paramConfig, timeWindowDays, transferRecordsDF);
            }

            if (resultDF != null) {
                List<Row> rows = resultDF.collectAsList();
                for (Row row : rows) {
                    // 兼容处理 account_id 类型
                    String accStr = row.getAs("account_id").toString();
                    Long accountId = Long.valueOf(accStr);
                    Double value = ((Number) row.getAs(1)).doubleValue();
                    if (value != null) {
                        results.put(accountId, value);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("计算指标 {} 失败", indicatorCode, e);
        }
        return results;
    }

    /**
     * 指标A：大额快进快出 (保留完整逻辑)
     */
    private Dataset<Row> calculateIndicatorAWithSpark(JsonNode paramConfig, Integer timeWindowDays, Dataset<Row> transferRecordsDF) {
        double largeAmountThreshold = paramConfig.get("large_amount_threshold").asDouble(50000.0);
        int quickTransferMinutes = paramConfig.get("quick_transfer_minutes").asInt(2);
        double minTransferAmount = paramConfig.get("min_transfer_amount").asDouble(50000.0);

        logger.info("📊 [A] 大额快进快出: 阈值={}, 分钟={}", largeAmountThreshold, quickTransferMinutes);

        // 1. 转换时间戳
        Dataset<Row> t = transferRecordsDF
                .withColumn("tx_time", col("tx_time").cast("long")) // 如果CSV是毫秒数不需要除1000，视数据而定
                .withColumn("amount", col("amount").cast("double"));

        // 2. 计算每个账户的最后交易时间
        Dataset<Row> latestTransaction = t
                .withColumn("account_id", coalesce(col("from_account"), col("to_account")))
                .groupBy("account_id")
                .agg(max("tx_time").alias("last_transaction_time"));

        // 3. 计算时间窗口
        Dataset<Row> timeWindow = latestTransaction
                .withColumn("window_start_time",
                        col("last_transaction_time").minus(timeWindowDays * 24 * 3600 * 1000L)); // 假设是毫秒

        // 4. 找到大额转入
        Dataset<Row> largeIncoming = t
                .filter(col("amount").geq(largeAmountThreshold))
                .select(
                        col("to_account").alias("account_id"),
                        col("tx_time").alias("incoming_time"),
                        col("amount").alias("incoming_amount")
                );

        // 5. 找到大额转出
        Dataset<Row> largeOutgoing = t
                .filter(col("amount").geq(minTransferAmount))
                .select(
                        col("from_account").alias("account_id"),
                        col("tx_time").alias("outgoing_time"),
                        col("amount").alias("outgoing_amount")
                );

        // 6. 关联时间窗口和转入
        Dataset<Row> incomingInWindow = timeWindow
                .join(largeIncoming, "account_id")
                .filter(col("incoming_time").geq(col("window_start_time")))
                .filter(col("incoming_time").leq(col("last_transaction_time")));

        // 7. 关联转出并计算时间差
        Dataset<Row> quickTransfers = incomingInWindow
                .join(largeOutgoing, "account_id")
                .filter(col("outgoing_time").geq(col("incoming_time")))
                .withColumn("time_diff_seconds",
                        col("outgoing_time").minus(col("incoming_time")).divide(1000))
                .filter(col("time_diff_seconds").leq(quickTransferMinutes * 60));

        // 8. 统计
        Dataset<Row> result = quickTransfers
                .groupBy("account_id")
                .agg(count(lit(1)).alias("indicator_A"))
                .filter(col("indicator_A").gt(0));

        return result;
    }

    /**
     * 指标B：登录后快速转账 (保留完整逻辑)
     */
    private Dataset<Row> calculateIndicatorBWithSpark(JsonNode paramConfig, Integer timeWindowDays,
                                                      Dataset<Row> loginRecordsDF, Dataset<Row> transferRecordsDF) {
        double LARGE_AMOUNT = 50000.0;

        logger.info("📊 [B] 登录后快速转账: 大额阈值={}", LARGE_AMOUNT);

        // 1. 转账
        Dataset<Row> tLarge = transferRecordsDF
                .withColumn("tx_time", col("tx_time").cast("long"))
                .withColumn("amount", col("amount").cast("double"))
                .filter(col("amount").geq(LARGE_AMOUNT))
                .select(col("from_account").alias("account_id"), col("tx_time"));

        // 2. 登录
        Dataset<Row> l = loginRecordsDF
                .withColumn("login_time", col("login_time").cast("long"))
                .select(col("account_id"), col("login_time"));

        // 3. 关联
        Dataset<Row> joined = tLarge
                .join(l, tLarge.col("account_id").equalTo(l.col("account_id")), "inner")
                .select(tLarge.col("account_id"));

        // 4. 统计
        Dataset<Row> result = joined
                .groupBy("account_id")
                .agg(count(lit(1)).alias("indicator_B"))
                .filter(col("indicator_B").gt(0));

        return result;
    }

    /**
     * 指标C：累计入账金额 (保留完整逻辑)
     */
    private Dataset<Row> calculateIndicatorCWithSpark(JsonNode paramConfig, Integer timeWindowDays, Dataset<Row> transferRecordsDF) {
        logger.info("📊 [C] 累计入账金额分析");

        // 1. 聚合
        Dataset<Row> c = transferRecordsDF
                .withColumn("amount", col("amount").cast("double"))
                .groupBy(col("to_account").alias("account_id"))
                .agg(sum("amount").alias("total_incoming"))
                .withColumn("indicator_C", when(col("total_incoming").gt(50000), lit(1)).otherwise(lit(0)));

        // 2. 结果
        Dataset<Row> result = c.select("account_id", "indicator_C")
                .filter(col("indicator_C").gt(0));

        return result;
    }

    // --- 辅助方法：集合操作 ---

    private Map<Long, Map<String, Double>> intersectResults(Map<String, Map<Long, Double>> indicatorResultsMap) {
        Map<Long, Map<String, Double>> result = new HashMap<>();
        if (indicatorResultsMap.isEmpty()) return result;

        Set<Long> accountIds = null;
        for (Map.Entry<String, Map<Long, Double>> entry : indicatorResultsMap.entrySet()) {
            if (accountIds == null) {
                accountIds = new HashSet<>(entry.getValue().keySet());
            } else {
                accountIds.retainAll(entry.getValue().keySet());
            }
        }

        if (accountIds != null) {
            for (Long accountId : accountIds) {
                Map<String, Double> indicatorValues = new HashMap<>();
                for (Map.Entry<String, Map<Long, Double>> entry : indicatorResultsMap.entrySet()) {
                    Double value = entry.getValue().get(accountId);
                    if (value != null) indicatorValues.put(entry.getKey(), value);
                }
                result.put(accountId, indicatorValues);
            }
        }
        return result;
    }

    private Map<Long, Map<String, Double>> unionResults(Map<String, Map<Long, Double>> indicatorResultsMap) {
        Map<Long, Map<String, Double>> result = new HashMap<>();
        for (Map.Entry<String, Map<Long, Double>> entry : indicatorResultsMap.entrySet()) {
            String indicatorCode = entry.getKey();
            Map<Long, Double> indicatorResult = entry.getValue();
            for (Map.Entry<Long, Double> accountEntry : indicatorResult.entrySet()) {
                result.computeIfAbsent(accountEntry.getKey(), k -> new HashMap<>())
                        .put(indicatorCode, accountEntry.getValue());
            }
        }
        return result;
    }

    private com.example.banks.engine.SqlRuleResult convertToSqlRuleResult(SparkRuleResult sparkResult) {
        com.example.banks.engine.SqlRuleResult sqlResult = new com.example.banks.engine.SqlRuleResult();
        sqlResult.setMatchedAccounts(sparkResult.getMatchedAccounts());
        return sqlResult;
    }

    // --- 内部类 ---

    private static class SparkRuleResult {
        private String ruleExpression;
        private Map<Long, Map<String, Double>> matchedAccounts = new HashMap<>();
        public String getRuleExpression() { return ruleExpression; }
        public void setRuleExpression(String ruleExpression) { this.ruleExpression = ruleExpression; }
        public Map<Long, Map<String, Double>> getMatchedAccounts() { return matchedAccounts; }
        public void setMatchedAccounts(Map<Long, Map<String, Double>> matchedAccounts) { this.matchedAccounts = matchedAccounts; }
    }

    private static class RuleCondition {
        private String indicatorCode;
        private String operator;
        private double threshold;

        public RuleCondition(String indicatorCode, String operator, double threshold) {
            this.indicatorCode = indicatorCode;
            this.operator = operator;
            this.threshold = threshold;
        }
        public String getIndicatorCode() { return indicatorCode; }
        public String getOperator() { return operator; }
        public double getThreshold() { return threshold; }

        public boolean evaluate(double value) {
            switch (operator) {
                case ">": return value > threshold;
                case ">=": return value >= threshold;
                case "<": return value < threshold;
                case "<=": return value <= threshold;
                case "=": return Math.abs(value - threshold) < 0.0001;
                case "!=": return Math.abs(value - threshold) >= 0.0001;
                default: return false;
            }
        }
    }

    private List<RuleCondition> parseRuleExpression(String ruleExpression) {
        List<RuleCondition> conditions = new ArrayList<>();
        Pattern pattern = Pattern.compile("(INDICATOR_[A-Z0-9_]+)\\s*(>=|<=|>|<|=|!=)\\s*([0-9.]+)");
        Matcher matcher = pattern.matcher(ruleExpression);
        while (matcher.find()) {
            conditions.add(new RuleCondition(matcher.group(1), matcher.group(2), Double.parseDouble(matcher.group(3))));
        }
        return conditions;
    }

    private Map<Long, Double> filterByCondition(Map<Long, Double> indicatorResults, RuleCondition condition) {
        Map<Long, Double> filtered = new HashMap<>();
        for (Map.Entry<Long, Double> entry : indicatorResults.entrySet()) {
            if (condition.evaluate(entry.getValue())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }
}