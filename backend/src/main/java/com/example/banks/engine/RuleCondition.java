package com.example.banks.engine;

/**
 * 单个规则条件
 */
public class RuleCondition {
    private String id;
    private String indicatorCode;
    private String operator;
    private double value;
    private int position;
    private String originalText;
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getIndicatorCode() { return indicatorCode; }
    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
    
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    
    @Override
    public String toString() {
        return String.format("%s %s %.1f", indicatorCode, operator, value);
    }
}


