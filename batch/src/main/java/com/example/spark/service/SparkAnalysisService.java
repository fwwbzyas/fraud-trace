package com.example.spark.service;

import com.example.spark.dto.AnalysisRequest;
import com.example.spark.dto.AnalysisResponse;
import com.example.spark.BanksIngestionService;
import com.example.spark.BanksIndicatorCalculator;
import com.example.spark.RuleEvaluator;
import com.example.spark.DynamicRuleEvaluator;
import com.example.spark.SparkSessionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Spark 分析服务 (HDFS 完整适配版)
 */
@Service
public class SparkAnalysisService {

    private SparkSession spark;
    private BanksIngestionService ingestionService;
    private BanksIndicatorCalculator indicatorCalculator;
    private ObjectMapper objectMapper;
    private boolean initialized = false;

    // 注入 HDFS 配置
    @Value("${hadoop.fs.defaultFS:hdfs://localhost:8020}")
    private String hdfsUri;

    @Value("${spark.hdfs.base.dir:/user/banks/input}")
    private String hdfsBaseDir;

    @Value("${hadoop.user.name:root}")
    private String hadoopUser;

    public SparkAnalysisService() {
        this.objectMapper = new ObjectMapper();
        System.out.println("📋 Spark 分析服务已创建，等待分析任务...");
    }

    /**
     * 懒加载初始化 Spark 会话
     */
    private synchronized void initializeIfNeeded() {
        if (!initialized) {
            System.out.println("🚀 首次分析请求，正在初始化 Spark 会话...");
            // 设置 Hadoop 用户，防止权限问题
            System.setProperty("HADOOP_USER_NAME", hadoopUser);

            this.spark = SparkSessionFactory.create("BanksSparkAnalysisAPI");

            // ✅ [HDFS 适配] 使用 HDFS 参数初始化数据摄取服务
            System.out.println("🌊 初始化数据摄取服务，HDFS地址: " + hdfsUri);
            this.ingestionService = new BanksIngestionService(spark, hdfsUri, hdfsBaseDir);

            this.indicatorCalculator = new BanksIndicatorCalculator(spark);
            this.initialized = true;
            System.out.println("✅ Spark 分析服务初始化完成");
        }
    }

    /**
     * 🔥🔥🔥 [新增] 上传文件到 HDFS 🔥🔥🔥
     */
    public String uploadFileToHdfs(MultipartFile file, String fileName) throws IOException {
        // 确保 Spark 环境已准备好
        initializeIfNeeded();

        String targetPath = hdfsBaseDir + "/" + fileName;
        System.out.println("📤 [HDFS] 正在上传文件: " + targetPath);

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);

        // ⭐ 关键配置：强制使用主机名连接 DataNode (解决 Docker 网络隔离问题)
        conf.set("dfs.client.use.datanode.hostname", "true");
        // 禁用校验和缓存
        conf.set("fs.hdfs.impl.disable.cache", "true");

        try (FileSystem fs = FileSystem.get(URI.create(hdfsUri), conf, hadoopUser)) {
            Path path = new Path(targetPath);
            Path dir = new Path(hdfsBaseDir);

            // 自动创建目录
            if (!fs.exists(dir)) {
                fs.mkdirs(dir);
                System.out.println("📁 创建 HDFS 目录: " + hdfsBaseDir);
            }

            // 覆盖已存在的文件
            if (fs.exists(path)) {
                fs.delete(path, false);
            }

            try (var out = fs.create(path); var in = file.getInputStream()) {
                in.transferTo(out);
            }
            System.out.println("✅ HDFS 上传成功！");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HDFS 上传中断", e);
        }

        return targetPath;
    }

    @PreDestroy
    public void cleanup() {
        if (spark != null) {
            System.out.println("🔄 关闭 Spark 会话...");
            spark.stop();
        }
    }

    /**
     * 执行单个指标分析
     */
    public AnalysisResponse executeAnalysis(AnalysisRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            initializeIfNeeded();

            System.out.println("📊 开始执行指标分析: " + request.getIndicatorCode());

            ObjectNode paramConfig = buildParamConfig(request);

            // 从 HDFS 读取数据
            Dataset<Row> accounts = ingestionService.readAccounts();
            Dataset<Row> logins = ingestionService.readLoginRecords();
            Dataset<Row> transfers = ingestionService.readTransferRecords();

            Dataset<Row> result = indicatorCalculator.calculateIndicator(
                    request.getIndicatorCode(),
                    paramConfig,
                    accounts,
                    logins,
                    transfers,
                    request.getTimeWindowDays()
            );

            result.cache();
            long totalRecords = result.count();
            System.out.println("📊 指标 " + request.getIndicatorCode() + " 计算完成，共 " + totalRecords + " 条结果");

            List<Map<String, Object>> resultList = convertDatasetToList(result);
            Map<String, Object> statistics = calculateStatistics(result, request.getIndicatorCode());
            long executionTime = System.currentTimeMillis() - startTime;

            AnalysisResponse response = new AnalysisResponse(true, "分析执行成功");
            response.setIndicatorCode(request.getIndicatorCode());
            response.setExecutionTimeMs(executionTime);
            response.setTotalRecords(totalRecords);
            response.setResults(resultList);
            response.setStatistics(statistics);

            if (totalRecords > 0) {
                System.out.println("📈 结果预览（前5条）:");
                result.show(5);
            } else {
                System.out.println("⚠️ 指标 " + request.getIndicatorCode() + " 没有返回结果");
            }

            return response;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            System.err.println("❌ 指标分析失败: " + e.getMessage());
            e.printStackTrace();

            AnalysisResponse response = new AnalysisResponse(false, "分析执行失败: " + e.getMessage());
            response.setIndicatorCode(request.getIndicatorCode());
            response.setExecutionTimeMs(executionTime);
            return response;
        }
    }

    /**
     * 🔥 执行分离规则分析（受害人 + 可疑分子）- 完整逻辑
     */
    public AnalysisResponse executeSeparateRulesAnalysis(AnalysisRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            initializeIfNeeded();

            System.out.println("🎯 开始执行分离规则分析...");
            System.out.println("👥 受害人规则: " + request.getVictimRule());
            System.out.println("🕵️ 可疑分子规则: " + request.getSuspectRule());

            ObjectNode paramConfig = buildParamConfig(request);

            // 从 HDFS 读取数据
            Dataset<Row> accounts = ingestionService.readAccounts();
            Dataset<Row> logins = ingestionService.readLoginRecords();
            Dataset<Row> transfers = ingestionService.readTransferRecords();

            Set<String> requiredIndicators = extractIndicatorsFromRules(
                    request.getVictimRule(),
                    request.getSuspectRule()
            );

            System.out.println("📊 需要计算的指标: " + requiredIndicators);

            Map<String, Dataset<Row>> indicatorResults = new HashMap<>();

            for (String indicatorCode : requiredIndicators) {
                System.out.println("📊 计算指标 " + indicatorCode + "...");
                try {
                    Dataset<Row> indicatorResult = indicatorCalculator.calculateIndicator(
                            indicatorCode, paramConfig, accounts, logins, transfers, request.getTimeWindowDays()
                    );
                    indicatorResults.put(indicatorCode, indicatorResult);
                    System.out.println("✅ 指标 " + indicatorCode + " 计算完成，结果数: " + indicatorResult.count());
                } catch (Exception e) {
                    System.err.println("❌ 计算指标 " + indicatorCode + " 失败: " + e.getMessage());
                    throw e;
                }
            }

            DynamicRuleEvaluator dynamicEvaluator = new DynamicRuleEvaluator();

            // 评估受害人规则
            Dataset<Row> victimAccounts = null;
            long victimCount = 0;
            if (request.getVictimRule() != null && !request.getVictimRule().trim().isEmpty()) {
                System.out.println("⚖️ 评估受害人规则: " + request.getVictimRule());
                victimAccounts = dynamicEvaluator.evaluate(request.getVictimRule(), indicatorResults);
                victimAccounts.cache();
                victimCount = victimAccounts.count();
                System.out.println("👥 识别出 " + victimCount + " 个受害人账户");
            }

            // 评估可疑分子规则
            Dataset<Row> suspectAccounts = null;
            long suspectCount = 0;
            if (request.getSuspectRule() != null && !request.getSuspectRule().trim().isEmpty()) {
                System.out.println("⚖️ 评估可疑分子规则: " + request.getSuspectRule());
                suspectAccounts = dynamicEvaluator.evaluate(request.getSuspectRule(), indicatorResults);
                suspectAccounts.cache();
                suspectCount = suspectAccounts.count();
                System.out.println("🕵️ 识别出 " + suspectCount + " 个可疑分子账户");
            }

            // 合并结果
            List<Map<String, Object>> resultList = new ArrayList<>();

            if (victimAccounts != null && victimCount > 0) {
                List<Map<String, Object>> victimList = convertDatasetToList(victimAccounts);
                for (Map<String, Object> victim : victimList) {
                    victim.put("account_role", "VICTIM");
                    victim.put("role_name", "受害人");
                    resultList.add(victim);
                }
                System.out.println("👥 受害人账户预览（前5个）:");
                victimAccounts.show(5);
            }

            if (suspectAccounts != null && suspectCount > 0) {
                List<Map<String, Object>> suspectList = convertDatasetToList(suspectAccounts);
                for (Map<String, Object> suspect : suspectList) {
                    suspect.put("account_role", "SUSPECT");
                    suspect.put("role_name", "可疑分子");
                    resultList.add(suspect);
                }
                System.out.println("🕵️ 可疑分子账户预览（前5个）:");
                suspectAccounts.show(5);
            }

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("victim_count", victimCount);
            statistics.put("suspect_count", suspectCount);
            statistics.put("total_risk_accounts", victimCount + suspectCount);
            statistics.put("victim_rule", request.getVictimRule());
            statistics.put("suspect_rule", request.getSuspectRule());
            statistics.put("time_window_days", request.getTimeWindowDays());

            long executionTime = System.currentTimeMillis() - startTime;

            AnalysisResponse response = new AnalysisResponse(true, "分离规则分析执行成功");
            response.setIndicatorCode("SEPARATE_RULES_ANALYSIS");
            response.setExecutionTimeMs(executionTime);
            response.setTotalRecords(victimCount + suspectCount);
            response.setResults(resultList);
            response.setStatistics(statistics);

            System.out.println("✅ 分离规则分析完成，耗时: " + executionTime + "ms");

            return response;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            System.err.println("❌ 分离规则分析失败: " + e.getMessage());
            e.printStackTrace();

            AnalysisResponse response = new AnalysisResponse(false, "分离规则分析执行失败: " + e.getMessage());
            response.setIndicatorCode("SEPARATE_RULES_ANALYSIS");
            response.setExecutionTimeMs(executionTime);
            return response;
        }
    }

    /**
     * 🔥 执行完整的风险分析 - 完整逻辑
     */
    public AnalysisResponse executeRiskAnalysis(AnalysisRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            initializeIfNeeded();

            System.out.println("🎯 开始执行完整风险分析...");

            ObjectNode paramConfig = buildParamConfig(request);
            double largeAmountThreshold = paramConfig.get("large_amount_threshold").asDouble();

            // 从 HDFS 读取数据
            Dataset<Row> accounts = ingestionService.readAccounts();
            Dataset<Row> logins = ingestionService.readLoginRecords();
            Dataset<Row> transfers = ingestionService.readTransferRecords();

            Set<String> requiredIndicators = extractIndicatorsFromRules(request.getRuleExpression());
            System.out.println("📊 需要计算的指标: " + requiredIndicators);

            Map<String, Dataset<Row>> indicatorResults = new HashMap<>();

            for (String indicatorCode : requiredIndicators) {
                System.out.println("📊 计算指标 " + indicatorCode + "...");
                try {
                    Dataset<Row> indicatorResult = indicatorCalculator.calculateIndicator(
                            indicatorCode, paramConfig, accounts, logins, transfers, request.getTimeWindowDays()
                    );
                    indicatorResults.put(indicatorCode, indicatorResult);
                    System.out.println("✅ 指标 " + indicatorCode + " 计算完成，结果数: " + indicatorResult.count());
                } catch (Exception e) {
                    System.err.println("❌ 计算指标 " + indicatorCode + " 失败: " + e.getMessage());
                    throw e;
                }
            }

            Dataset<Row> riskAccounts;

            if (request.getRuleExpression() != null && !request.getRuleExpression().trim().isEmpty()) {
                System.out.println("⚖️ 使用动态规则评估: " + request.getRuleExpression());
                DynamicRuleEvaluator dynamicEvaluator = new DynamicRuleEvaluator();
                riskAccounts = dynamicEvaluator.evaluate(request.getRuleExpression(), indicatorResults);
            } else {
                System.out.println("⚖️ 使用硬编码规则评估，阈值: " + largeAmountThreshold);
                RuleEvaluator ruleEvaluator = new RuleEvaluator();
                Dataset<Row> indicatorA = indicatorResults.getOrDefault("INDICATOR_A", null);
                Dataset<Row> indicatorB = indicatorResults.getOrDefault("INDICATOR_B", null);
                Dataset<Row> indicatorC = indicatorResults.getOrDefault("INDICATOR_C", null);
                riskAccounts = ruleEvaluator.evaluate(indicatorA, indicatorB, indicatorC, largeAmountThreshold);
            }

            riskAccounts.cache();
            long totalRiskAccounts = riskAccounts.count();

            System.out.println("🎯 风险分析完成，识别出 " + totalRiskAccounts + " 个风险账户");

            List<Map<String, Object>> resultList = convertDatasetToList(riskAccounts);

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total_risk_accounts", totalRiskAccounts);
            statistics.put("large_amount_threshold", largeAmountThreshold);
            statistics.put("time_window_days", request.getTimeWindowDays());

            long executionTime = System.currentTimeMillis() - startTime;

            AnalysisResponse response = new AnalysisResponse(true, "风险分析执行成功");
            response.setIndicatorCode("RISK_ANALYSIS");
            response.setExecutionTimeMs(executionTime);
            response.setTotalRecords(totalRiskAccounts);
            response.setResults(resultList);
            response.setStatistics(statistics);

            if (totalRiskAccounts > 0) {
                System.out.println("🚨 风险账户预览（前10个）:");
                riskAccounts.show(10);
            } else {
                System.out.println("✅ 未发现风险账户");
            }

            System.out.println("✅ 风险分析完成，耗时: " + executionTime + "ms");
            return response;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            System.err.println("❌ 风险分析失败: " + e.getMessage());
            e.printStackTrace();

            AnalysisResponse response = new AnalysisResponse(false, "风险分析执行失败: " + e.getMessage());
            response.setIndicatorCode("RISK_ANALYSIS");
            response.setExecutionTimeMs(executionTime);
            return response;
        }
    }

    // --- 辅助方法 ---

    private ObjectNode buildParamConfig(AnalysisRequest request) {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("large_amount_threshold", request.getLargeAmountThreshold() != null ? request.getLargeAmountThreshold() : 50000.0);
        config.put("quick_transfer_minutes", request.getQuickTransferMinutes() != null ? request.getQuickTransferMinutes() : 2);
        config.put("min_transfer_amount", request.getMinTransferAmount() != null ? request.getMinTransferAmount() : 50000.0);
        config.put("quick_operation_minutes", request.getQuickOperationMinutes() != null ? request.getQuickOperationMinutes() : 5);
        config.put("min_single_amount", request.getMinSingleAmount() != null ? request.getMinSingleAmount() : 0.01);
        // 其他默认参数
        config.put("night_start_hour", 22);
        config.put("night_end_hour", 6);
        config.put("gang_member_threshold", 3);
        config.put("time_window_minutes", 30);
        config.put("distinct_location_threshold", 3);
        return config;
    }

    private List<Map<String, Object>> convertDatasetToList(Dataset<Row> dataset) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // 限制结果数量
        List<Row> rows = dataset.limit(1000).collectAsList();
        for (Row row : rows) {
            Map<String, Object> rowMap = new HashMap<>();
            String[] fieldNames = row.schema().fieldNames();
            for (int i = 0; i < fieldNames.length; i++) {
                rowMap.put(fieldNames[i], row.get(i));
            }
            resultList.add(rowMap);
        }
        return resultList;
    }

    private Map<String, Object> calculateStatistics(Dataset<Row> result, String indicatorCode) {
        Map<String, Object> stats = new HashMap<>();
        try {
            long totalCount = result.count();
            stats.put("total_count", totalCount);
            if (totalCount > 0) {
                switch (indicatorCode) {
                    case "INDICATOR_A":
                        Row maxRow = result.agg(
                                org.apache.spark.sql.functions.max("indicator_A"),
                                org.apache.spark.sql.functions.avg("indicator_A"),
                                org.apache.spark.sql.functions.sum("total_in_amount")
                        ).first();
                        stats.put("max_quick_transfers", maxRow.get(0));
                        stats.put("avg_quick_transfers", maxRow.get(1));
                        stats.put("total_amount", maxRow.get(2));
                        break;
                    case "INDICATOR_B":
                        Row maxRowB = result.agg(
                                org.apache.spark.sql.functions.max("indicator_B"),
                                org.apache.spark.sql.functions.avg("indicator_B"),
                                org.apache.spark.sql.functions.sum("total_quick_transfer_amount")
                        ).first();
                        stats.put("max_quick_operations", maxRowB.get(0));
                        stats.put("avg_quick_operations", maxRowB.get(1));
                        stats.put("total_amount", maxRowB.get(2));
                        break;
                    case "INDICATOR_C":
                        Row maxRowC = result.agg(
                                org.apache.spark.sql.functions.max("indicator_C"),
                                org.apache.spark.sql.functions.avg("indicator_C"),
                                org.apache.spark.sql.functions.sum("indicator_C")
                        ).first();
                        stats.put("max_total_amount", maxRowC.get(0));
                        stats.put("avg_total_amount", maxRowC.get(1));
                        stats.put("sum_total_amount", maxRowC.get(2));
                        break;
                }
            }
        } catch (Exception e) {
            stats.put("error", "统计信息计算失败: " + e.getMessage());
        }
        return stats;
    }

    public Map<String, Object> getDataSourceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            initializeIfNeeded();
            Dataset<Row> accounts = ingestionService.readAccounts();
            Dataset<Row> logins = ingestionService.readLoginRecords();
            Dataset<Row> transfers = ingestionService.readTransferRecords();

            stats.put("accounts_count", accounts.count());
            stats.put("logins_count", logins.count());
            stats.put("transfers_count", transfers.count());
            stats.put("status", "healthy");
            stats.put("source", "HDFS");
        } catch (Exception e) {
            stats.put("status", "error");
            stats.put("error_message", e.getMessage());
        }
        return stats;
    }

    /**
     * 从规则表达式中提取所有使用的指标代码
     */
    private Set<String> extractIndicatorsFromRules(String... rules) {
        Set<String> indicators = new HashSet<>();
        for (String rule : rules) {
            if (rule == null || rule.trim().isEmpty()) continue;
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("INDICATOR_[A-Z]");
            java.util.regex.Matcher matcher = pattern.matcher(rule);
            while (matcher.find()) indicators.add(matcher.group());
        }
        return indicators;
    }
}