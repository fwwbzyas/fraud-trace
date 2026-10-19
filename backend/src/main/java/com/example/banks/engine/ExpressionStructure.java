package com.example.banks.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 表达式结构分析结果
 */
public class ExpressionStructure {
    private String originalExpression;
    private boolean hasParentheses;
    private int maxNestingDepth;
    private List<String> parenthesesGroups = new ArrayList<>();
    private String mainLogicType;
    
    // Getters and Setters
    public String getOriginalExpression() { return originalExpression; }
    public void setOriginalExpression(String originalExpression) { this.originalExpression = originalExpression; }
    
    public boolean isHasParentheses() { return hasParentheses; }
    public void setHasParentheses(boolean hasParentheses) { this.hasParentheses = hasParentheses; }
    
    public int getMaxNestingDepth() { return maxNestingDepth; }
    public void setMaxNestingDepth(int maxNestingDepth) { this.maxNestingDepth = maxNestingDepth; }
    
    public List<String> getParenthesesGroups() { return parenthesesGroups; }
    public void setParenthesesGroups(List<String> parenthesesGroups) { this.parenthesesGroups = parenthesesGroups; }
    
    public String getMainLogicType() { return mainLogicType; }
    public void setMainLogicType(String mainLogicType) { this.mainLogicType = mainLogicType; }
    
    @Override
    public String toString() {
        return String.format("结构[括号:%s, 嵌套深度:%d, 主逻辑:%s]", 
            hasParentheses, maxNestingDepth, mainLogicType);
    }
}


