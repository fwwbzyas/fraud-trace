package com.example.spark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 分析请求参数
 */
public class AnalysisRequest {
    
    @JsonProperty("indicator_code")
    private String indicatorCode;
    
    @JsonProperty("time_window_days")
    private Integer timeWindowDays;
    
    // 指标A参数
    @JsonProperty("large_amount_threshold")
    private Double largeAmountThreshold;
    
    @JsonProperty("quick_transfer_minutes")
    private Integer quickTransferMinutes;
    
    @JsonProperty("min_transfer_amount")
    private Double minTransferAmount;
    
    // 指标B参数
    @JsonProperty("quick_operation_minutes")
    private Integer quickOperationMinutes;
    
    // 指标C参数
    @JsonProperty("min_single_amount")
    private Double minSingleAmount;
    
    // 规则表达式（用于规则评估）
    @JsonProperty("rule_expression")
    private String ruleExpression;
    
    @JsonProperty("victim_rule")
    private String victimRule;
    
    @JsonProperty("suspect_rule")
    private String suspectRule;
    
    // 构造函数
    public AnalysisRequest() {}
    
    // Getters and Setters
    public String getIndicatorCode() {
        return indicatorCode;
    }
    
    public void setIndicatorCode(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }
    
    public Integer getTimeWindowDays() {
        return timeWindowDays;
    }
    
    public void setTimeWindowDays(Integer timeWindowDays) {
        this.timeWindowDays = timeWindowDays;
    }
    
    public Double getLargeAmountThreshold() {
        return largeAmountThreshold;
    }
    
    public void setLargeAmountThreshold(Double largeAmountThreshold) {
        this.largeAmountThreshold = largeAmountThreshold;
    }
    
    public Integer getQuickTransferMinutes() {
        return quickTransferMinutes;
    }
    
    public void setQuickTransferMinutes(Integer quickTransferMinutes) {
        this.quickTransferMinutes = quickTransferMinutes;
    }
    
    public Double getMinTransferAmount() {
        return minTransferAmount;
    }
    
    public void setMinTransferAmount(Double minTransferAmount) {
        this.minTransferAmount = minTransferAmount;
    }
    
    public Integer getQuickOperationMinutes() {
        return quickOperationMinutes;
    }
    
    public void setQuickOperationMinutes(Integer quickOperationMinutes) {
        this.quickOperationMinutes = quickOperationMinutes;
    }
    
    public Double getMinSingleAmount() {
        return minSingleAmount;
    }
    
    public void setMinSingleAmount(Double minSingleAmount) {
        this.minSingleAmount = minSingleAmount;
    }
    
    public String getRuleExpression() {
        return ruleExpression;
    }
    
    public void setRuleExpression(String ruleExpression) {
        this.ruleExpression = ruleExpression;
    }
    
    public String getVictimRule() {
        return victimRule;
    }
    
    public void setVictimRule(String victimRule) {
        this.victimRule = victimRule;
    }
    
    public String getSuspectRule() {
        return suspectRule;
    }
    
    public void setSuspectRule(String suspectRule) {
        this.suspectRule = suspectRule;
    }
}
