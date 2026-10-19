package com.example.banks.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析后的规则表达式
 */
public class ParsedRuleExpression {
    private String originalExpression;
    private String normalizedExpression;
    private List<RuleCondition> conditions = new ArrayList<>();
    private List<String> logicOperators = new ArrayList<>();
    private ExpressionStructure structure;
    
    // Getters and Setters
    public String getOriginalExpression() { return originalExpression; }
    public void setOriginalExpression(String originalExpression) { this.originalExpression = originalExpression; }
    
    public String getNormalizedExpression() { return normalizedExpression; }
    public void setNormalizedExpression(String normalizedExpression) { this.normalizedExpression = normalizedExpression; }
    
    public List<RuleCondition> getConditions() { return conditions; }
    public void setConditions(List<RuleCondition> conditions) { this.conditions = conditions; }
    
    public List<String> getLogicOperators() { return logicOperators; }
    public void setLogicOperators(List<String> logicOperators) { this.logicOperators = logicOperators; }
    
    public ExpressionStructure getStructure() { return structure; }
    public void setStructure(ExpressionStructure structure) { this.structure = structure; }
}


