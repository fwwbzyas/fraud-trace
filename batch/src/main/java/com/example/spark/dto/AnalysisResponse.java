package com.example.spark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * 分析响应结果
 */
public class AnalysisResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("indicator_code")
    private String indicatorCode;
    
    @JsonProperty("execution_time_ms")
    private long executionTimeMs;
    
    @JsonProperty("total_records")
    private long totalRecords;
    
    @JsonProperty("results")
    private List<Map<String, Object>> results;
    
    @JsonProperty("statistics")
    private Map<String, Object> statistics;
    
    // 构造函数
    public AnalysisResponse() {}
    
    public AnalysisResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getIndicatorCode() {
        return indicatorCode;
    }
    
    public void setIndicatorCode(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public long getTotalRecords() {
        return totalRecords;
    }
    
    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    public List<Map<String, Object>> getResults() {
        return results;
    }
    
    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
    }
    
    public Map<String, Object> getStatistics() {
        return statistics;
    }
    
    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
}
