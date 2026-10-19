package com.example.banks.engine;

/**
 * SQL规则条件
 */
public class SqlRuleCondition {
    private String indicatorCode;
    private String operator;
    private double value;
    
    public SqlRuleCondition(String indicatorCode, String operator, double value) {
        this.indicatorCode = indicatorCode;
        this.operator = operator;
        this.value = value;
    }
    
    // Getters
    public String getIndicatorCode() { return indicatorCode; }
    public String getOperator() { return operator; }
    public double getValue() { return value; }
}

