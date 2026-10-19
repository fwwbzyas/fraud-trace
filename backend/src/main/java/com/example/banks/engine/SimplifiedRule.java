package com.example.banks.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 简化后的规则（用于SQL生成）
 */
public class SimplifiedRule {
    private List<RuleCondition> conditions = new ArrayList<>();
    private String logicOperator; // AND 或 OR
    
    public List<RuleCondition> getConditions() { return conditions; }
    public void setConditions(List<RuleCondition> conditions) { this.conditions = conditions; }
    
    public String getLogicOperator() { return logicOperator; }
    public void setLogicOperator(String logicOperator) { this.logicOperator = logicOperator; }
}



