package com.example.banks.engine;

import com.example.banks.entity.AnalysisIndicator;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于SQL的规则引擎
 * 支持根据指标编码动态查找SQL模板，并连接多个SQL查询
 * 
 * 规则示例：
 * INDICATOR_A > 0 AND INDICATOR_B > 0 AND INDICATOR_C = 0
 * 
 * 执行逻辑：
 * 1. 解析规则，提取指标编码和条件
 * 2. 根据指标编码查找对应的SQL模板
 * 3. 构建联合SQL查询，使用JOIN或子查询连接
 * 4. 执行SQL并返回符合条件的账户
 */
@Component
public class SqlBasedRuleEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlBasedRuleEngine.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private RuleExpressionParser expressionParser;
    
    /**
     * 根据规则表达式和指标配置生成并执行SQL查询
     * @param ruleExpression 规则表达式
     * @param indicators 指标配置列表
     * @param timeRangeDays 时间范围
     * @return 符合条件的账户ID及其指标值
     */
    public SqlRuleResult executeRule(String ruleExpression, 
                                   List<AnalysisIndicator> indicators, 
                                   Integer timeRangeDays) {
        logger.info("开始执行基于SQL的规则: {}", ruleExpression);
        
        try {
            // 1. 智能解析规则表达式
            ParsedRuleExpression parsedExpression = expressionParser.parse(ruleExpression);
            logger.info("解析结果: 条件数={}, 逻辑操作符={}, 结构={}",
                parsedExpression.getConditions().size(),
                parsedExpression.getLogicOperators(),
                parsedExpression.getStructure());
            
            // 2. 构建指标映射
            Map<String, AnalysisIndicator> indicatorMap = buildIndicatorMap(indicators);
            
            // 3. 验证规则中的指标编码
            validateIndicatorCodes(parsedExpression, indicatorMap);
            
            // 4. 简化规则用于SQL生成
            SimplifiedRule simplifiedRule = expressionParser.simplifyForSql(parsedExpression);
            
            // 5. 生成联合SQL查询
            String unionSql = generateAdvancedUnionSql(simplifiedRule, indicatorMap, timeRangeDays);
            logger.info("🚀 即将执行的联合SQL: {}", unionSql);
            
            // 6. 执行SQL查询
            List<Map<String, Object>> resultRows = jdbcTemplate.queryForList(unionSql);
            
            logger.info("📊 SQL执行结果行数: {}", resultRows.size());
            
            // 7. 处理查询结果
            SqlRuleResult result = processAdvancedQueryResults(resultRows, simplifiedRule);
            
            logger.info("SQL规则执行完成，匹配账户数: {}", result.getMatchedAccounts().size());
            return result;
            
        } catch (Exception e) {
            logger.error("SQL规则执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("SQL规则执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析规则表达式，提取指标编码和条件
     */
    private ParsedSqlRule parseRuleExpression(String ruleExpression) {
        ParsedSqlRule rule = new ParsedSqlRule();
        rule.setOriginalExpression(ruleExpression);
        
        // 正则表达式匹配指标条件：INDICATOR_XXX > 0
        Pattern conditionPattern = Pattern.compile("(INDICATOR_[A-Z_]+)\\s*(>=|<=|>|<|=|!=)\\s*(\\d+(?:\\.\\d+)?)");
        Matcher matcher = conditionPattern.matcher(ruleExpression);
        
        while (matcher.find()) {
            String indicatorCode = matcher.group(1);
            String operator = matcher.group(2);
            double value = Double.parseDouble(matcher.group(3));
            
            SqlRuleCondition condition = new SqlRuleCondition(indicatorCode, operator, value);
            rule.addCondition(condition);
            
            logger.debug("解析到条件: {} {} {}", indicatorCode, operator, value);
        }
        
        // 解析逻辑连接符
        if (ruleExpression.contains(" AND ")) {
            rule.setLogicOperator("AND");
        } else if (ruleExpression.contains(" OR ")) {
            rule.setLogicOperator("OR");
        } else {
            rule.setLogicOperator("AND"); // 默认使用AND
        }
        
        return rule;
    }
    
    /**
     * 构建指标编码到指标配置的映射
     */
    private Map<String, AnalysisIndicator> buildIndicatorMap(List<AnalysisIndicator> indicators) {
        Map<String, AnalysisIndicator> map = new HashMap<>();
        for (AnalysisIndicator indicator : indicators) {
            map.put(indicator.getIndicatorCode(), indicator);
        }
        return map;
    }
    
    /**
     * 验证规则中的指标编码是否都存在（新版本）
     */
    private void validateIndicatorCodes(ParsedRuleExpression parsedExpression, Map<String, AnalysisIndicator> indicatorMap) {
        for (RuleCondition condition : parsedExpression.getConditions()) {
            if (!indicatorMap.containsKey(condition.getIndicatorCode())) {
                throw new IllegalArgumentException("未知的指标编码: " + condition.getIndicatorCode());
            }
        }
        logger.debug("指标编码验证通过");
    }
    
    /**
     * 验证规则中的指标编码是否都存在（旧版本兼容）
     */
    private void validateIndicatorCodes(ParsedSqlRule rule, Map<String, AnalysisIndicator> indicatorMap) {
        for (SqlRuleCondition condition : rule.getConditions()) {
            if (!indicatorMap.containsKey(condition.getIndicatorCode())) {
                throw new IllegalArgumentException("未知的指标编码: " + condition.getIndicatorCode());
            }
        }
    }
    
    /**
     * 生成联合SQL查询
     * 将多个指标的SQL模板连接成一个查询
     */
    private String generateUnionSql(ParsedSqlRule rule, 
                                   Map<String, AnalysisIndicator> indicatorMap, 
                                   Integer timeRangeDays) {
        StringBuilder sqlBuilder = new StringBuilder();
        
        if ("AND".equals(rule.getLogicOperator())) {
            // AND逻辑：使用JOIN连接多个指标查询
            sqlBuilder.append(generateJoinSql(rule, indicatorMap, timeRangeDays));
        } else {
            // OR逻辑：使用UNION连接多个指标查询
            sqlBuilder.append(generateUnionOrSql(rule, indicatorMap, timeRangeDays));
        }
        
        return sqlBuilder.toString();
    }
    
    /**
     * 生成JOIN连接的SQL（用于AND逻辑）
     */
    private String generateJoinSql(ParsedSqlRule rule, 
                                  Map<String, AnalysisIndicator> indicatorMap, 
                                  Integer timeRangeDays) {
        StringBuilder sql = new StringBuilder();
        sql.append("WITH ");
        
        // 为每个指标生成CTE（公共表表达式）
        List<String> cteList = new ArrayList<>();
        for (int i = 0; i < rule.getConditions().size(); i++) {
            SqlRuleCondition condition = rule.getConditions().get(i);
            AnalysisIndicator indicator = indicatorMap.get(condition.getIndicatorCode());
            
            String cteName = "indicator_" + (i + 1);
            String indicatorSql = generateSingleIndicatorSql(indicator, condition, timeRangeDays);
            
            cteList.add(cteName + " AS (\n" + indicatorSql + "\n)");
        }
        
        sql.append(String.join(",\n", cteList));
        sql.append("\n");
        
        // 主查询：JOIN所有CTE
        sql.append("SELECT ");
        List<String> selectColumns = new ArrayList<>();
        for (int i = 0; i < rule.getConditions().size(); i++) {
            String cteName = "indicator_" + (i + 1);
            String indicatorCode = rule.getConditions().get(i).getIndicatorCode();
            selectColumns.add(cteName + ".account_id");
            selectColumns.add(cteName + ".value AS " + indicatorCode.toLowerCase());
        }
        sql.append(String.join(", ", selectColumns));
        
        sql.append("\nFROM indicator_1");
        for (int i = 1; i < rule.getConditions().size(); i++) {
            String cteName = "indicator_" + (i + 1);
            sql.append("\nINNER JOIN ").append(cteName)
               .append(" ON indicator_1.account_id = ").append(cteName).append(".account_id");
        }
        
        return sql.toString();
    }
    
    /**
     * 生成高级联合SQL查询（使用新的解析结果）
     */
    private String generateAdvancedUnionSql(SimplifiedRule simplifiedRule, 
                                           Map<String, AnalysisIndicator> indicatorMap, 
                                           Integer timeRangeDays) {
        StringBuilder sqlBuilder = new StringBuilder();
        
        if ("AND".equals(simplifiedRule.getLogicOperator())) {
            // AND逻辑：使用JOIN连接多个指标查询
            sqlBuilder.append(generateAdvancedJoinSql(simplifiedRule, indicatorMap, timeRangeDays));
        } else {
            // OR逻辑：使用UNION连接多个指标查询
            sqlBuilder.append(generateAdvancedUnionOrSql(simplifiedRule, indicatorMap, timeRangeDays));
        }
        
        return sqlBuilder.toString();
    }
    
    /**
     * 生成高级JOIN连接的SQL（用于AND逻辑）
     */
    private String generateAdvancedJoinSql(SimplifiedRule simplifiedRule, 
                                          Map<String, AnalysisIndicator> indicatorMap, 
                                          Integer timeRangeDays) {
        StringBuilder sql = new StringBuilder();
        sql.append("WITH ");
        
        // 为每个指标生成CTE（公共表表达式）
        List<String> cteList = new ArrayList<>();
        for (int i = 0; i < simplifiedRule.getConditions().size(); i++) {
            RuleCondition condition = simplifiedRule.getConditions().get(i);
            AnalysisIndicator indicator = indicatorMap.get(condition.getIndicatorCode());
            
            String cteName = "indicator_" + (i + 1);
            String indicatorSql = generateAdvancedSingleIndicatorSql(indicator, condition, timeRangeDays);
            
            cteList.add(cteName + " AS (\n" + indicatorSql + "\n)");
            
            logger.debug("生成CTE {}: 指标={}, 条件={} {} {}", 
                cteName, condition.getIndicatorCode(), 
                condition.getOperator(), condition.getValue(), condition.getOriginalText());
        }
        
        sql.append(String.join(",\n", cteList));
        sql.append("\n");
        
        // 主查询：JOIN所有CTE
        sql.append("SELECT ");
        List<String> selectColumns = new ArrayList<>();
        for (int i = 0; i < simplifiedRule.getConditions().size(); i++) {
            String cteName = "indicator_" + (i + 1);
            String indicatorCode = simplifiedRule.getConditions().get(i).getIndicatorCode();
            
            if (i == 0) {
                selectColumns.add(cteName + ".account_id");
            }
            selectColumns.add(cteName + ".value AS " + indicatorCode.toLowerCase());
        }
        sql.append(String.join(", ", selectColumns));
        
        sql.append("\nFROM indicator_1");
        for (int i = 1; i < simplifiedRule.getConditions().size(); i++) {
            String cteName = "indicator_" + (i + 1);
            sql.append("\nINNER JOIN ").append(cteName)
               .append(" ON indicator_1.account_id = ").append(cteName).append(".account_id");
        }
        
        logger.debug("生成高级JOIN SQL，条件数: {}", simplifiedRule.getConditions().size());
        return sql.toString();
    }
    
    /**
     * 生成高级UNION连接的SQL（用于OR逻辑）
     */
    private String generateAdvancedUnionOrSql(SimplifiedRule simplifiedRule, 
                                             Map<String, AnalysisIndicator> indicatorMap, 
                                             Integer timeRangeDays) {
        List<String> unionParts = new ArrayList<>();
        
        for (RuleCondition condition : simplifiedRule.getConditions()) {
            AnalysisIndicator indicator = indicatorMap.get(condition.getIndicatorCode());
            String indicatorSql = generateAdvancedSingleIndicatorSql(indicator, condition, timeRangeDays);
            
            String unionPart = "SELECT account_id, value, '" + condition.getIndicatorCode() + "' as indicator_code, '" + 
                              condition.getOriginalText() + "' as matched_condition\n" +
                              "FROM (\n" + indicatorSql + "\n) sub";
            unionParts.add(unionPart);
            
            logger.debug("生成UNION部分: 指标={}, 条件={}", 
                condition.getIndicatorCode(), condition.getOriginalText());
        }
        
        return String.join("\nUNION ALL\n", unionParts);
    }
    
    /**
     * 为单个指标生成高级SQL查询
     */
    private String generateAdvancedSingleIndicatorSql(AnalysisIndicator indicator, 
                                                     RuleCondition condition, 
                                                     Integer timeRangeDays) {
        JsonNode paramConfig = indicator.getParamConfig();
        String sqlTemplate = paramConfig.get("sql_template").asText();
        
        if (sqlTemplate == null || sqlTemplate.trim().isEmpty()) {
            throw new IllegalArgumentException("指标 " + indicator.getIndicatorCode() + " 没有配置SQL模板");
        }
        
        logger.debug("处理指标 {} 的SQL模板，条件: {} {} {}", 
            indicator.getIndicatorCode(), condition.getOperator(), condition.getValue(), condition.getOriginalText());
        
        // 替换SQL模板中的参数
        String sql = replaceSqlParameters(sqlTemplate, paramConfig, timeRangeDays);
        
        // 添加智能条件过滤
        sql = addSmartConditionFilter(sql, condition, indicator);
        
        logger.info("🔍 生成的SQL: {}", sql);
        return sql;
    }
    
    /**
     * 智能条件过滤器
     * 根据指标类型和条件操作符，选择最优的SQL过滤策略
     */
    private String addSmartConditionFilter(String sql, RuleCondition condition, AnalysisIndicator indicator) {
        String operator = condition.getOperator();
        double value = condition.getValue();
        String indicatorCode = condition.getIndicatorCode();
        
        logger.debug("为指标 {} 添加智能条件过滤: {} {} {}", 
            indicatorCode, operator, value, condition.getOriginalText());
        
        // 根据指标类型采用不同策略
        if (indicatorCode.contains("_A") || indicatorCode.contains("_B") || 
            indicatorCode.contains("_D") || indicatorCode.contains("_E") || indicatorCode.contains("_F")) {
            // 计数类指标：大额快进快出次数、登录后转账次数、夜间登录次数、团伙作案次数、异地登录次数
            return addCountBasedFilter(sql, condition);
        } else if (indicatorCode.contains("_C")) {
            // 金额类指标：累计入账金额
            return addAmountBasedFilter(sql, condition);
        } else {
            // 通用指标处理
            return addGenericFilter(sql, condition);
        }
    }
    
    /**
     * 计数类指标过滤器
     */
    private String addCountBasedFilter(String sql, RuleCondition condition) {
        String operator = condition.getOperator();
        double value = condition.getValue();
        
        // 对于计数指标，通常使用HAVING COUNT(*) > 0 这样的条件
        String conditionClause = "COUNT(*) " + operator + " " + value;
        
        if (sql.toLowerCase().contains("having")) {
            sql = sql.replaceFirst("(?i)(having\\s+)", "$1" + conditionClause + " AND ");
        } else if (sql.toLowerCase().contains("group by")) {
            sql += "\nHAVING " + conditionClause;
        } else {
            // 如果没有GROUP BY，需要添加
            sql += "\nGROUP BY account_id\nHAVING " + conditionClause;
        }
        
        logger.debug("应用计数过滤器: {}", conditionClause);
        return sql;
    }
    
    /**
     * 金额类指标过滤器
     */
    private String addAmountBasedFilter(String sql, RuleCondition condition) {
        String operator = condition.getOperator();
        double value = condition.getValue();
        
        // 🔧 修复：检查SQL是否已经计算了SUM并返回为value字段
        boolean sqlAlreadyHasSum = sql.toLowerCase().contains("sum(") && sql.toLowerCase().contains("as value");
        
        String conditionClause;
        if ("=".equals(operator) && value == 0) {
            // 特殊处理：查找入账金额为0的账户（闲置账户）
            if (sqlAlreadyHasSum) {
                // PostgreSQL的HAVING不能直接用别名，需要用完整表达式
                conditionClause = "COALESCE(SUM(tr.amount), 0) = 0";
            } else {
                conditionClause = "COALESCE(SUM(amount), 0) = 0";
            }
        } else {
            if (sqlAlreadyHasSum) {
                // PostgreSQL的HAVING不能直接用别名，需要用完整表达式
                conditionClause = "COALESCE(SUM(tr.amount), 0) " + operator + " " + value;
            } else {
                conditionClause = "SUM(amount) " + operator + " " + value;
            }
        }
        
        // 检查SQL是否包含UNION ALL，需要特殊处理
        if (sql.toLowerCase().contains("union all")) {
            return addAmountFilterToUnionQuery(sql, conditionClause);
        }
        
        if (sql.toLowerCase().contains("having")) {
            sql = sql.replaceFirst("(?i)(having\\s+)", "$1" + conditionClause + " AND ");
        } else if (sql.toLowerCase().contains("group by")) {
            sql += "\nHAVING " + conditionClause;
        } else {
            sql += "\nGROUP BY account_id\nHAVING " + conditionClause;
        }
        
        logger.info("✅ 应用金额过滤器: {} (SQL已有SUM: {})", conditionClause, sqlAlreadyHasSum);
        return sql;
    }
    
    /**
     * 为包含UNION ALL的查询添加金额过滤器
     */
    private String addAmountFilterToUnionQuery(String sql, String conditionClause) {
        // 将整个UNION查询包装在子查询中，然后添加HAVING条件
        // 需要将条件中的字段引用从amount改为value，因为内层查询返回的是value字段
        String adjustedConditionClause = conditionClause.replace("SUM(amount)", "SUM(value)")
                                                        .replace("COALESCE(SUM(amount), 0)", "COALESCE(SUM(value), 0)");
        
        String wrappedSql = "SELECT account_id, SUM(value) as value FROM (\n" +
                           sql + "\n" +
                           ") combined_results\n" +
                           "GROUP BY account_id\n" +
                           "HAVING " + adjustedConditionClause;
        
        logger.debug("为UNION查询应用金额过滤器: {} -> {}", conditionClause, adjustedConditionClause);
        return wrappedSql;
    }
    
    /**
     * 通用指标过滤器
     */
    private String addGenericFilter(String sql, RuleCondition condition) {
        String operator = condition.getOperator();
        double value = condition.getValue();
        String conditionClause = "value " + operator + " " + value;
        
        if (sql.toLowerCase().contains("having")) {
            sql = sql.replaceFirst("(?i)(having\\s+)", "$1" + conditionClause + " AND ");
        } else if (sql.toLowerCase().contains("group by")) {
            sql += "\nHAVING " + conditionClause;
        } else if (sql.toLowerCase().contains("where")) {
            sql = sql.replaceFirst("(?i)(where\\s+)", "$1" + conditionClause + " AND ");
        } else {
            sql += "\nWHERE " + conditionClause;
        }
        
        logger.debug("应用通用过滤器: {}", conditionClause);
        return sql;
    }
    
    /**
     * 处理高级查询结果
     */
    private SqlRuleResult processAdvancedQueryResults(List<Map<String, Object>> resultRows, SimplifiedRule simplifiedRule) {
        SqlRuleResult result = new SqlRuleResult();
        // 使用简化规则构建一个兼容的ParsedSqlRule
        ParsedSqlRule compatibleRule = createCompatibleRule(simplifiedRule);
        result.setRule(compatibleRule);
        
        logger.info("🔍 开始处理查询结果，行数: {}", resultRows.size());
        
        for (Map<String, Object> row : resultRows) {
            logger.info("📋 SQL返回的行数据: {}", row);
            Long accountId = ((Number) row.get("account_id")).longValue();
            
            Map<String, Double> indicatorValues = new HashMap<>();
            for (RuleCondition condition : simplifiedRule.getConditions()) {
                String indicatorCode = condition.getIndicatorCode();
                String lookupKey = indicatorCode.toLowerCase();
                Object valueObj = row.get(lookupKey);
                logger.info("🔍 查找指标 {} (key: {}) -> 值: {}", indicatorCode, lookupKey, valueObj);
                if (valueObj != null) {
                    indicatorValues.put(indicatorCode, ((Number) valueObj).doubleValue());
                } else {
                    // 为缺失的指标设置默认值
                    indicatorValues.put(indicatorCode, 0.0);
                    logger.warn("⚠️ 指标 {} 未找到，设置默认值 0.0", indicatorCode);
                }
            }
            
            result.addMatchedAccount(accountId, indicatorValues);
            
            logger.debug("处理账户 {}: 指标值 {}", accountId, indicatorValues);
        }
        
        logger.info("查询结果处理完成，匹配账户数: {}", result.getMatchedAccounts().size());
        return result;
    }
    
    /**
     * 创建兼容的ParsedSqlRule对象
     */
    private ParsedSqlRule createCompatibleRule(SimplifiedRule simplifiedRule) {
        ParsedSqlRule compatibleRule = new ParsedSqlRule();
        compatibleRule.setLogicOperator(simplifiedRule.getLogicOperator());
        
        for (RuleCondition condition : simplifiedRule.getConditions()) {
            SqlRuleCondition sqlCondition = new SqlRuleCondition(
                condition.getIndicatorCode(), 
                condition.getOperator(), 
                condition.getValue()
            );
            compatibleRule.addCondition(sqlCondition);
        }
        
        return compatibleRule;
    }
    
    /**
     * 生成UNION连接的SQL（用于OR逻辑）
     */
    private String generateUnionOrSql(ParsedSqlRule rule, 
                                     Map<String, AnalysisIndicator> indicatorMap, 
                                     Integer timeRangeDays) {
        List<String> unionParts = new ArrayList<>();
        
        for (SqlRuleCondition condition : rule.getConditions()) {
            AnalysisIndicator indicator = indicatorMap.get(condition.getIndicatorCode());
            String indicatorSql = generateSingleIndicatorSql(indicator, condition, timeRangeDays);
            
            String unionPart = "SELECT account_id, value, '" + condition.getIndicatorCode() + "' as indicator_code\n" +
                              "FROM (\n" + indicatorSql + "\n) sub";
            unionParts.add(unionPart);
        }
        
        return String.join("\nUNION ALL\n", unionParts);
    }
    
    /**
     * 为单个指标生成SQL查询
     */
    private String generateSingleIndicatorSql(AnalysisIndicator indicator, 
                                             SqlRuleCondition condition, 
                                             Integer timeRangeDays) {
        JsonNode paramConfig = indicator.getParamConfig();
        String sqlTemplate = paramConfig.get("sql_template").asText();
        
        if (sqlTemplate == null || sqlTemplate.trim().isEmpty()) {
            throw new IllegalArgumentException("指标 " + indicator.getIndicatorCode() + " 没有配置SQL模板");
        }
        
        // 替换SQL模板中的参数
        String sql = replaceSqlParameters(sqlTemplate, paramConfig, timeRangeDays);
        
        // 添加条件过滤
        sql = addConditionFilter(sql, condition);
        
        return sql;
    }
    
    /**
     * 替换SQL模板中的参数占位符
     */
    private String replaceSqlParameters(String sqlTemplate, JsonNode paramConfig, Integer timeRangeDays) {
        String sql = sqlTemplate;
        
        // 替换时间窗口参数
        sql = sql.replace("{time_window_days}", String.valueOf(timeRangeDays));
        
        // 替换其他参数
        final String[] finalSql = {sql}; // 使用数组绕过final限制
        paramConfig.fieldNames().forEachRemaining(fieldName -> {
            JsonNode fieldValue = paramConfig.get(fieldName);
            if (fieldValue.isNumber() || fieldValue.isBoolean()) {
                String placeholder = "{" + fieldName + "}";
                if (finalSql[0].contains(placeholder)) {
                    finalSql[0] = finalSql[0].replace(placeholder, fieldValue.asText());
                }
            }
        });
        sql = finalSql[0];
        
        // 转换MySQL风格的间隔语法为PostgreSQL语法
        sql = convertIntervalSyntaxToPostgreSQL(sql);
        
        // 修复字段映射不一致问题
        sql = fixFieldMappingIssues(sql);
        
        // 检查并处理剩余的未替换占位符
        sql = handleRemainingPlaceholders(sql);
        
        return sql;
    }
    
    /**
     * 在SQL查询中添加条件过滤
     */
    private String addConditionFilter(String sql, SqlRuleCondition condition) {
        // 在WHERE子句或HAVING子句中添加条件
        String operator = condition.getOperator();
        double value = condition.getValue();
        
        // 假设SQL返回的列名是'value'
        String conditionClause = "value " + operator + " " + value;
        
        if (sql.toLowerCase().contains("having")) {
            // 如果已有HAVING子句，添加AND条件
            sql = sql.replaceFirst("(?i)having\\s+", "HAVING " + conditionClause + " AND ");
        } else if (sql.toLowerCase().contains("group by")) {
            // 如果有GROUP BY，添加HAVING子句
            sql += "\nHAVING " + conditionClause;
        } else {
            // 否则添加WHERE条件
            if (sql.toLowerCase().contains("where")) {
                sql = sql.replaceFirst("(?i)where\\s+", "WHERE " + conditionClause + " AND ");
            } else {
                sql += "\nWHERE " + conditionClause;
            }
        }
        
        return sql;
    }
    
    /**
     * 处理查询结果
     */
    private SqlRuleResult processQueryResults(List<Map<String, Object>> resultRows, ParsedSqlRule rule) {
        SqlRuleResult result = new SqlRuleResult();
        result.setRule(rule);
        
        for (Map<String, Object> row : resultRows) {
            Long accountId = ((Number) row.get("account_id")).longValue();
            
            Map<String, Double> indicatorValues = new HashMap<>();
            for (SqlRuleCondition condition : rule.getConditions()) {
                String indicatorCode = condition.getIndicatorCode();
                Object valueObj = row.get(indicatorCode.toLowerCase());
                if (valueObj != null) {
                    indicatorValues.put(indicatorCode, ((Number) valueObj).doubleValue());
                }
            }
            
            result.addMatchedAccount(accountId, indicatorValues);
        }
        
        return result;
    }
    
    /**
     * 将MySQL风格的间隔语法转换为PostgreSQL语法
     * MySQL: INTERVAL 30 DAY -> PostgreSQL: INTERVAL '30 days'
     * MySQL: INTERVAL 5 MINUTE -> PostgreSQL: INTERVAL '5 minutes'
     * MySQL: INTERVAL 1 HOUR -> PostgreSQL: INTERVAL '1 hours'
     */
    private String convertIntervalSyntaxToPostgreSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        // 转换 INTERVAL {number} DAY 格式
        sql = sql.replaceAll("(?i)INTERVAL\\s+(\\d+)\\s+DAY", "INTERVAL '$1 days'");
        
        // 转换 INTERVAL {number} MINUTE 格式
        sql = sql.replaceAll("(?i)INTERVAL\\s+(\\d+)\\s+MINUTE", "INTERVAL '$1 minutes'");
        
        // 转换 INTERVAL {number} HOUR 格式
        sql = sql.replaceAll("(?i)INTERVAL\\s+(\\d+)\\s+HOUR", "INTERVAL '$1 hours'");
        
        // 转换 INTERVAL {number} SECOND 格式
        sql = sql.replaceAll("(?i)INTERVAL\\s+(\\d+)\\s+SECOND", "INTERVAL '$1 seconds'");
        
        // 转换 INTERVAL {number} MONTH 格式
        sql = sql.replaceAll("(?i)INTERVAL\\s+(\\d+)\\s+MONTH", "INTERVAL '$1 months'");
        
        // 转换 INTERVAL {number} YEAR 格式
        sql = sql.replaceAll("(?i)INTERVAL\\s+(\\d+)\\s+YEAR", "INTERVAL '$1 years'");
        
        // 转换 CURRENT_DATE 为 CURRENT_DATE（PostgreSQL兼容）
        sql = sql.replaceAll("(?i)CURRENT_DATE(?!\\s*-)", "CURRENT_DATE");
        
        logger.debug("间隔语法转换完成: MySQL -> PostgreSQL");
        return sql;
    }
    
    /**
     * 修复SQL中的字段映射不一致问题
     * 主要解决GROUP BY和SELECT子句中字段名不匹配的问题
     */
    private String fixFieldMappingIssues(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        logger.debug("开始修复字段映射问题，原始SQL: {}", sql);
        
        // 修复常见的字段映射问题
        String fixedSql = sql;
        
        // 1. 修复 to_account_id 与 account_id 的映射问题
        // 当SELECT中使用account_id，但GROUP BY使用to_account_id时
        if (fixedSql.contains("GROUP BY to_account_id") && 
            fixedSql.contains("SELECT account_id")) {
            // 将GROUP BY中的to_account_id改为to_account_id，但确保SELECT中有正确的别名
            fixedSql = fixedSql.replaceAll("(?i)GROUP BY\\s+to_account_id", "GROUP BY to_account_id");
            // 确保SELECT中有正确的字段映射
            fixedSql = fixedSql.replaceAll("(?i)SELECT\\s+account_id,", "SELECT to_account_id as account_id,");
        }
        
        // 2. 修复 from_account_id 与 account_id 的映射问题
        if (fixedSql.contains("GROUP BY from_account_id") && 
            fixedSql.contains("SELECT account_id")) {
            fixedSql = fixedSql.replaceAll("(?i)GROUP BY\\s+from_account_id", "GROUP BY from_account_id");
            fixedSql = fixedSql.replaceAll("(?i)SELECT\\s+account_id,", "SELECT from_account_id as account_id,");
        }
        
        // 3. 处理更复杂的情况：在子查询中修复字段映射
        fixedSql = fixComplexFieldMapping(fixedSql);
        
        if (!fixedSql.equals(sql)) {
            logger.debug("字段映射修复完成，修复后SQL: {}", fixedSql);
        }
        
        return fixedSql;
    }
    
    /**
     * 修复复杂的字段映射问题
     */
    private String fixComplexFieldMapping(String sql) {
        // 使用正则表达式处理更复杂的字段映射情况
        
        // 情况1: SELECT account_id, ... FROM table GROUP BY to_account_id
        Pattern pattern1 = Pattern.compile(
            "(SELECT)\\s+(account_id)(,.*?FROM.*?)GROUP BY\\s+(to_account_id)", 
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher1 = pattern1.matcher(sql);
        if (matcher1.find()) {
            String replacement = matcher1.group(1) + " " + matcher1.group(4) + " as " + matcher1.group(2) + 
                               matcher1.group(3) + "GROUP BY " + matcher1.group(4);
            sql = matcher1.replaceAll(replacement);
            logger.debug("应用复杂字段映射修复规则1");
        }
        
        // 情况2: SELECT account_id, ... FROM table GROUP BY from_account_id
        Pattern pattern2 = Pattern.compile(
            "(SELECT)\\s+(account_id)(,.*?FROM.*?)GROUP BY\\s+(from_account_id)", 
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher2 = pattern2.matcher(sql);
        if (matcher2.find()) {
            String replacement = matcher2.group(1) + " " + matcher2.group(4) + " as " + matcher2.group(2) + 
                               matcher2.group(3) + "GROUP BY " + matcher2.group(4);
            sql = matcher2.replaceAll(replacement);
            logger.debug("应用复杂字段映射修复规则2");
        }
        
        return sql;
    }
    
    /**
     * 处理SQL中剩余的未替换占位符
     * 为未替换的占位符提供默认值或抛出异常
     */
    private String handleRemainingPlaceholders(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        // 查找所有剩余的占位符 {placeholder_name}
        Pattern placeholderPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = placeholderPattern.matcher(sql);
        
        Map<String, String> defaultValues = new HashMap<>();
        // 为常见占位符提供默认值
        defaultValues.put("time_window_days", "30");
        defaultValues.put("large_amount_threshold", "10000");
        defaultValues.put("min_transfer_amount", "1000");
        defaultValues.put("quick_transfer_minutes", "5");
        defaultValues.put("quick_operation_minutes", "10");
        defaultValues.put("quick_out_minutes", "5");  // 添加缺失的占位符
        defaultValues.put("min_single_amount", "0");
        defaultValues.put("include_expense", "true");
        defaultValues.put("include_income", "true");
        defaultValues.put("exclude_zero", "false");
        
        StringBuffer result = new StringBuffer();
        boolean hasUnresolvedPlaceholders = false;
        
        while (matcher.find()) {
            String placeholderName = matcher.group(1);
            String defaultValue = defaultValues.get(placeholderName);
            
            if (defaultValue != null) {
                matcher.appendReplacement(result, defaultValue);
                logger.debug("使用默认值替换占位符: {} -> {}", placeholderName, defaultValue);
            } else {
                hasUnresolvedPlaceholders = true;
                logger.warn("发现未解析的占位符: {}", placeholderName);
                // 保留原占位符，不做替换
                matcher.appendReplacement(result, "{" + placeholderName + "}");
            }
        }
        matcher.appendTail(result);
        
        if (hasUnresolvedPlaceholders) {
            logger.warn("SQL中存在未解析的占位符，可能影响查询执行");
        }
        
        return result.toString();
    }
    
    /**
     * 执行分离的受害人和可疑分子规则分析
     * @param victimRule 受害人（付款方）规则表达式
     * @param suspectRule 可疑分子（收款方）规则表达式
     * @param victimIndicators 受害人相关指标
     * @param suspectIndicators 可疑分子相关指标
     * @param taskTimeRangeDays 任务筛选时间范围（天）- 决定分析哪些账户
     * @param indicatorTimeWindowDays 指标计算时间窗口（天）- 从最后交易时间往前推多少天
     * @return 合并的分析结果
     */
    public CombinedRuleResult executeSeparateRules(String victimRule, 
                                                  String suspectRule,
                                                  List<AnalysisIndicator> victimIndicators,
                                                  List<AnalysisIndicator> suspectIndicators,
                                                  Integer taskTimeRangeDays,
                                                  Integer indicatorTimeWindowDays) {
        logger.info("开始执行分离规则分析 - 受害人规则: {}, 可疑分子规则: {}, 任务时间范围: {}天, 指标窗口: {}天", 
            victimRule, suspectRule, taskTimeRangeDays, indicatorTimeWindowDays);
        
        CombinedRuleResult combinedResult = new CombinedRuleResult();
        
        try {
            // 1. 执行受害人规则（如果有）
            SqlRuleResult victimResult = null;
            if (victimRule != null && !victimRule.trim().isEmpty() && victimIndicators != null && !victimIndicators.isEmpty()) {
                logger.info("执行受害人规则: {}", victimRule);
                victimResult = executeRule(victimRule, victimIndicators, indicatorTimeWindowDays);
                combinedResult.setVictimResult(victimResult);
                logger.info("受害人规则执行完成，匹配账户数: {}", victimResult.getMatchedAccounts().size());
            }
            
            // 2. 执行可疑分子规则（如果有）
            SqlRuleResult suspectResult = null;
            if (suspectRule != null && !suspectRule.trim().isEmpty() && suspectIndicators != null && !suspectIndicators.isEmpty()) {
                logger.info("执行可疑分子规则: {}", suspectRule);
                suspectResult = executeRule(suspectRule, suspectIndicators, indicatorTimeWindowDays);
                combinedResult.setSuspectResult(suspectResult);
                logger.info("可疑分子规则执行完成，匹配账户数: {}", suspectResult.getMatchedAccounts().size());
            }
            
            // 3. 生成统计信息
            combinedResult.generateStatistics();
            
            logger.info("分离规则分析完成 - 受害人: {}, 可疑分子: {}", 
                combinedResult.getVictimCount(), combinedResult.getSuspectCount());
                
            return combinedResult;
            
        } catch (Exception e) {
            logger.error("分离规则执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("分离规则执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 合并规则结果类
     */
    public static class CombinedRuleResult {
        private SqlRuleResult victimResult;    // 受害人结果
        private SqlRuleResult suspectResult;   // 可疑分子结果
        private int victimCount;               // 受害人数量
        private int suspectCount;              // 可疑分子数量
        private int totalCount;                // 总数量
        
        public CombinedRuleResult() {}
        
        /**
         * 生成统计信息
         */
        public void generateStatistics() {
            victimCount = (victimResult != null) ? victimResult.getMatchedAccounts().size() : 0;
            suspectCount = (suspectResult != null) ? suspectResult.getMatchedAccounts().size() : 0;
            totalCount = victimCount + suspectCount;
        }
        
        // Getters and Setters
        public SqlRuleResult getVictimResult() { return victimResult; }
        public void setVictimResult(SqlRuleResult victimResult) { this.victimResult = victimResult; }
        
        public SqlRuleResult getSuspectResult() { return suspectResult; }
        public void setSuspectResult(SqlRuleResult suspectResult) { this.suspectResult = suspectResult; }
        
        public int getVictimCount() { return victimCount; }
        public void setVictimCount(int victimCount) { this.victimCount = victimCount; }
        
        public int getSuspectCount() { return suspectCount; }
        public void setSuspectCount(int suspectCount) { this.suspectCount = suspectCount; }
        
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    }
}

