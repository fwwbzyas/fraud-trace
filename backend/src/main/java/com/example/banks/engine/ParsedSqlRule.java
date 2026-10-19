package com.example.banks.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析后的SQL规则
 */
public class ParsedSqlRule {
    private String originalExpression;
    private String logicOperator = "AND"; // AND 或 OR
    private List<SqlRuleCondition> conditions = new ArrayList<>();
    
    // Getters and Setters
    public String getOriginalExpression() { return originalExpression; }
    public void setOriginalExpression(String originalExpression) { this.originalExpression = originalExpression; }
    
    public String getLogicOperator() { return logicOperator; }
    public void setLogicOperator(String logicOperator) { this.logicOperator = logicOperator; }
    
    public List<SqlRuleCondition> getConditions() { return conditions; }
    public void addCondition(SqlRuleCondition condition) { this.conditions.add(condition); }
}

