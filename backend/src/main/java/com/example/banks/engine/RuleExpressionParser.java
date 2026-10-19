package com.example.banks.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则表达式解析器
 * 负责智能化解析复杂的规则表达式，支持括号、多种逻辑连接符和条件语句
 * 
 * 支持的语法：
 * 1. 指标编码：INDICATOR_A, INDICATOR_B, INDICATOR_C
 * 2. 比较操作符：>, >=, <, <=, =, !=
 * 3. 逻辑操作符：AND, OR
 * 4. 括号：( ) 改变优先级
 * 5. 数值：整数和小数
 * 
 * 示例规则：
 * - 简单规则：INDICATOR_A > 0 AND INDICATOR_B > 0 AND INDICATOR_C = 0
 * - 复杂规则：(INDICATOR_A >= 2 OR INDICATOR_B >= 1) AND INDICATOR_C <= 1000
 * - 嵌套规则：((INDICATOR_A > 0 AND INDICATOR_B > 0) OR INDICATOR_C = 0) AND INDICATOR_D != 5
 */
@Component
public class RuleExpressionParser {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleExpressionParser.class);
    
    // 正则表达式模式
    private static final Pattern INDICATOR_PATTERN = Pattern.compile("INDICATOR_[A-Z_]+");
    private static final Pattern CONDITION_PATTERN = Pattern.compile(
        "(INDICATOR_[A-Z_]+)\\s*(>=|<=|>|<|=|!=)\\s*(\\d+(?:\\.\\d+)?)"
    );
    private static final Pattern LOGIC_OPERATOR_PATTERN = Pattern.compile("\\s+(AND|OR)\\s+");
    private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\([^()]+\\)");
    
    /**
     * 解析规则表达式，返回结构化的解析结果
     * @param ruleExpression 原始规则表达式
     * @return 解析后的规则对象
     */
    public ParsedRuleExpression parse(String ruleExpression) {
        logger.info("开始解析规则表达式: {}", ruleExpression);
        
        try {
            // 1. 预处理：标准化表达式
            String normalizedExpression = normalizeExpression(ruleExpression);
            logger.debug("标准化后的表达式: {}", normalizedExpression);
            
            // 2. 提取所有条件
            List<RuleCondition> conditions = extractConditions(normalizedExpression);
            logger.debug("提取到 {} 个条件", conditions.size());
            
            // 3. 提取逻辑操作符
            List<String> logicOperators = extractLogicOperators(normalizedExpression);
            logger.debug("提取到 {} 个逻辑操作符: {}", logicOperators.size(), logicOperators);
            
            // 4. 分析表达式结构
            ExpressionStructure structure = analyzeStructure(normalizedExpression);
            logger.debug("表达式结构: {}", structure);
            
            // 5. 验证表达式合法性
            validateExpression(conditions, logicOperators, structure);
            
            // 6. 构建解析结果
            ParsedRuleExpression result = new ParsedRuleExpression();
            result.setOriginalExpression(ruleExpression);
            result.setNormalizedExpression(normalizedExpression);
            result.setConditions(conditions);
            result.setLogicOperators(logicOperators);
            result.setStructure(structure);
            
            logger.info("规则表达式解析成功，条件数: {}, 逻辑操作符数: {}", 
                conditions.size(), logicOperators.size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("解析规则表达式失败: {}", e.getMessage(), e);
            throw new IllegalArgumentException("规则表达式解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 标准化表达式：统一空格、大小写等
     */
    private String normalizeExpression(String expression) {
        String normalized = expression.trim();
        
        // 统一逻辑操作符大小写
        normalized = normalized.replaceAll("(?i)\\s+and\\s+", " AND ");
        normalized = normalized.replaceAll("(?i)\\s+or\\s+", " OR ");
        
        // 统一括号周围的空格
        normalized = normalized.replaceAll("\\s*\\(\\s*", " ( ");
        normalized = normalized.replaceAll("\\s*\\)\\s*", " ) ");
        
        // 统一比较操作符周围的空格
        normalized = normalized.replaceAll("\\s*(>=|<=|>|<|=|!=)\\s*", " $1 ");
        
        // 清理多余空格
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }
    
    /**
     * 提取所有条件语句
     */
    private List<RuleCondition> extractConditions(String expression) {
        List<RuleCondition> conditions = new ArrayList<>();
        Matcher matcher = CONDITION_PATTERN.matcher(expression);
        
        int conditionIndex = 0;
        while (matcher.find()) {
            String indicatorCode = matcher.group(1);
            String operator = matcher.group(2);
            double value = Double.parseDouble(matcher.group(3));
            
            RuleCondition condition = new RuleCondition();
            condition.setId("COND_" + (++conditionIndex));
            condition.setIndicatorCode(indicatorCode);
            condition.setOperator(operator);
            condition.setValue(value);
            condition.setPosition(matcher.start());
            condition.setOriginalText(matcher.group());
            
            conditions.add(condition);
            
            logger.debug("提取条件: {} {} {} (位置: {})", 
                indicatorCode, operator, value, matcher.start());
        }
        
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("规则表达式中没有找到有效的条件语句");
        }
        
        return conditions;
    }
    
    /**
     * 提取逻辑操作符
     */
    private List<String> extractLogicOperators(String expression) {
        List<String> operators = new ArrayList<>();
        Matcher matcher = LOGIC_OPERATOR_PATTERN.matcher(expression);
        
        while (matcher.find()) {
            String operator = matcher.group(1);
            operators.add(operator);
            logger.debug("提取逻辑操作符: {} (位置: {})", operator, matcher.start());
        }
        
        return operators;
    }
    
    /**
     * 分析表达式结构（括号嵌套层次）
     */
    private ExpressionStructure analyzeStructure(String expression) {
        ExpressionStructure structure = new ExpressionStructure();
        structure.setOriginalExpression(expression);
        
        // 检查是否有括号
        boolean hasParentheses = expression.contains("(") && expression.contains(")");
        structure.setHasParentheses(hasParentheses);
        
        if (hasParentheses) {
            // 分析括号嵌套层次
            int maxDepth = 0;
            int currentDepth = 0;
            
            for (char c : expression.toCharArray()) {
                if (c == '(') {
                    currentDepth++;
                    maxDepth = Math.max(maxDepth, currentDepth);
                } else if (c == ')') {
                    currentDepth--;
                }
            }
            
            structure.setMaxNestingDepth(maxDepth);
            
            // 提取括号组
            List<String> parenthesesGroups = extractParenthesesGroups(expression);
            structure.setParenthesesGroups(parenthesesGroups);
        }
        
        // 判断主要逻辑类型
        String mainLogicType = determineMainLogicType(expression);
        structure.setMainLogicType(mainLogicType);
        
        return structure;
    }
    
    /**
     * 提取括号组
     */
    private List<String> extractParenthesesGroups(String expression) {
        List<String> groups = new ArrayList<>();
        Matcher matcher = PARENTHESES_PATTERN.matcher(expression);
        
        while (matcher.find()) {
            String group = matcher.group();
            // 去掉外层括号
            group = group.substring(1, group.length() - 1).trim();
            groups.add(group);
            logger.debug("提取括号组: {}", group);
        }
        
        return groups;
    }
    
    /**
     * 确定主要逻辑类型
     */
    private String determineMainLogicType(String expression) {
        // 简单判断：如果包含OR，则为OR类型，否则为AND类型
        if (expression.contains(" OR ")) {
            return "OR";
        } else if (expression.contains(" AND ")) {
            return "AND";
        } else {
            return "SINGLE"; // 单个条件
        }
    }
    
    /**
     * 验证表达式的合法性
     */
    private void validateExpression(List<RuleCondition> conditions, 
                                  List<String> logicOperators, 
                                  ExpressionStructure structure) {
        
        // 检查条件数量和逻辑操作符数量的关系
        if (conditions.size() > 1 && logicOperators.size() != conditions.size() - 1) {
            throw new IllegalArgumentException(
                String.format("逻辑操作符数量不匹配：条件数 %d，操作符数 %d", 
                    conditions.size(), logicOperators.size()));
        }
        
        // 检查指标编码的有效性
        for (RuleCondition condition : conditions) {
            if (!INDICATOR_PATTERN.matcher(condition.getIndicatorCode()).matches()) {
                throw new IllegalArgumentException("无效的指标编码: " + condition.getIndicatorCode());
            }
        }
        
        // 检查括号匹配
        if (structure.isHasParentheses()) {
            String expr = structure.getOriginalExpression();
            int openCount = (int) expr.chars().filter(ch -> ch == '(').count();
            int closeCount = (int) expr.chars().filter(ch -> ch == ')').count();
            
            if (openCount != closeCount) {
                throw new IllegalArgumentException("括号不匹配：开括号 " + openCount + "，闭括号 " + closeCount);
            }
        }
        
        logger.debug("表达式验证通过");
    }
    
    /**
     * 获取表达式中所有唯一的指标编码
     */
    public Set<String> extractIndicatorCodes(String ruleExpression) {
        Set<String> indicatorCodes = new HashSet<>();
        Matcher matcher = INDICATOR_PATTERN.matcher(ruleExpression);
        
        while (matcher.find()) {
            indicatorCodes.add(matcher.group());
        }
        
        logger.debug("提取到指标编码: {}", indicatorCodes);
        return indicatorCodes;
    }
    
    /**
     * 将复杂表达式转换为简单的AND或OR连接
     * 用于SQL生成的简化处理
     */
    public SimplifiedRule simplifyForSql(ParsedRuleExpression parsedRule) {
        SimplifiedRule simplified = new SimplifiedRule();
        simplified.setConditions(parsedRule.getConditions());
        
        // 简化逻辑：如果包含OR，则使用OR；否则使用AND
        String mainLogic = parsedRule.getStructure().getMainLogicType();
        if ("OR".equals(mainLogic)) {
            simplified.setLogicOperator("OR");
        } else {
            simplified.setLogicOperator("AND");
        }
        
        logger.debug("简化规则为: {} 逻辑，条件数: {}", 
            simplified.getLogicOperator(), simplified.getConditions().size());
        
        return simplified;
    }
}
