package com.example.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import static org.apache.spark.sql.functions.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 动态规则评估器 - 支持从规则表达式中解析条件
 * 替代硬编码的RuleEvaluator
 */
public class DynamicRuleEvaluator {

    /**
     * 规则条件
     */
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

        @Override
        public String toString() {
            return indicatorCode + " " + operator + " " + threshold;
        }
    }

    /**
     * 解析规则表达式
     * 例如: "INDICATOR_A > 0 AND INDICATOR_B > 0 AND INDICATOR_C >= 50000"
     */
    private List<RuleCondition> parseRule(String ruleExpression) {
        List<RuleCondition> conditions = new ArrayList<>();
        
        // 正则表达式匹配: INDICATOR_XXX >= 50000
        Pattern pattern = Pattern.compile("(INDICATOR_[A-Z0-9_]+)\\s*(>=|<=|>|<|=|!=)\\s*([0-9.]+)");
        Matcher matcher = pattern.matcher(ruleExpression);
        
        while (matcher.find()) {
            String indicatorCode = matcher.group(1);
            String operator = matcher.group(2);
            double threshold = Double.parseDouble(matcher.group(3));
            
            conditions.add(new RuleCondition(indicatorCode, operator, threshold));
            System.out.println("解析规则条件: " + indicatorCode + " " + operator + " " + threshold);
        }
        
        System.out.println("从规则 '" + ruleExpression + "' 中解析出 " + conditions.size() + " 个条件");
        return conditions;
    }

    /**
     * 动态评估规则
     * @param ruleExpression 规则表达式
     * @param indicatorResults 指标计算结果的Map，key为指标代码
     * @return 符合条件的账户及其指标值
     */
    public Dataset<Row> evaluate(String ruleExpression, Map<String, Dataset<Row>> indicatorResults) {
        System.out.println("=== 动态规则评估 ===");
        System.out.println("规则表达式: " + ruleExpression);
        
        // 1. 解析规则表达式
        List<RuleCondition> conditions = parseRule(ruleExpression);
        
        // 2. 为每个指标应用条件过滤
        List<Dataset<Row>> filteredDatasets = new ArrayList<>();
        
        for (RuleCondition condition : conditions) {
            String indicatorCode = condition.getIndicatorCode();
            Dataset<Row> indicatorDF = indicatorResults.get(indicatorCode);
            
            if (indicatorDF == null) {
                System.out.println("⚠️ 警告: 未找到指标 " + indicatorCode + " 的计算结果");
                continue;
            }
            
            // 统一列名为 account_id
            String accountIdCol = indicatorDF.columns()[0];
            Dataset<Row> renamedDF = indicatorDF.withColumnRenamed(accountIdCol, "account_id");
            
            // 应用条件过滤
            Dataset<Row> filtered = applyCondition(renamedDF, indicatorCode, condition);
            
            System.out.println("指标 " + condition + " 过滤后账户数: " + filtered.count());
            filteredDatasets.add(filtered);
        }
        
        // 3. 根据逻辑操作符合并结果
        if (filteredDatasets.isEmpty()) {
            throw new RuntimeException("没有任何指标通过条件过滤");
        }
        
        Dataset<Row> result = filteredDatasets.get(0);
        
        if (ruleExpression.contains("AND")) {
            // AND逻辑：取交集
            System.out.println("使用 AND 逻辑合并结果");
            for (int i = 1; i < filteredDatasets.size(); i++) {
                result = result.join(filteredDatasets.get(i), "account_id");
            }
        } else if (ruleExpression.contains("OR")) {
            // OR逻辑：取并集
            System.out.println("使用 OR 逻辑合并结果");
            for (int i = 1; i < filteredDatasets.size(); i++) {
                result = result.union(filteredDatasets.get(i)).distinct();
            }
        }
        
        System.out.println("=== 最终匹配账户数: " + result.count() + " ===");
        result.show(false);
        
        return result;
    }

    /**
     * 应用条件过滤
     */
    private Dataset<Row> applyCondition(Dataset<Row> df, String indicatorCode, RuleCondition condition) {
        String indicatorCol = "indicator_" + indicatorCode.substring(10); // INDICATOR_A -> indicator_A
        
        // 查找实际的列名（可能是 indicator_A 或 INDICATOR_A）
        String actualCol = null;
        for (String col : df.columns()) {
            if (col.equalsIgnoreCase(indicatorCol)) {
                actualCol = col;
                break;
            }
        }
        
        if (actualCol == null) {
            System.out.println("⚠️ 警告: 未找到列 " + indicatorCol);
            return df;
        }
        
        // 根据操作符应用过滤
        Dataset<Row> filtered;
        switch (condition.getOperator()) {
            case ">":
                filtered = df.filter(col(actualCol).gt(condition.getThreshold()));
                break;
            case ">=":
                filtered = df.filter(col(actualCol).geq(condition.getThreshold()));
                break;
            case "<":
                filtered = df.filter(col(actualCol).lt(condition.getThreshold()));
                break;
            case "<=":
                filtered = df.filter(col(actualCol).leq(condition.getThreshold()));
                break;
            case "=":
                filtered = df.filter(col(actualCol).equalTo(condition.getThreshold()));
                break;
            case "!=":
                filtered = df.filter(col(actualCol).notEqual(condition.getThreshold()));
                break;
            default:
                System.out.println("⚠️ 不支持的操作符: " + condition.getOperator());
                filtered = df;
        }
        
        return filtered.select("account_id", actualCol);
    }
}
