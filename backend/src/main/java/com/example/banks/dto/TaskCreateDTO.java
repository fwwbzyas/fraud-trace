package com.example.banks.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 创建分析任务的DTO
 */
public class TaskCreateDTO {
    
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    
    @NotBlank(message = "指标ID列表不能为空")
    private String indicatorIds; // 如 "1,2,3"
    
    @NotNull(message = "规则配置不能为空")
    private JsonNode ruleConfig; // 规则配置JSON（旧版本兼容）
    
    // 新增：分别的受害人和可疑分子规则
    private String victimRule; // 受害人（付款方）规则表达式
    private String suspectRule; // 可疑分子（收款方）规则表达式
    private String victimIndicatorIds; // 受害人指标ID列表
    private String suspectIndicatorIds; // 可疑分子指标ID列表
    
    @NotNull(message = "任务筛选时间范围不能为空")
    @Min(value = 1, message = "任务筛选时间范围最少为1天")
    @Max(value = 3650, message = "任务筛选时间范围最多为10年")
    private Integer taskTimeRange; // 任务筛选时间范围（天）- 决定分析哪些账户
    
    @NotNull(message = "指标计算时间窗口不能为空")
    @Min(value = 1, message = "指标计算时间窗口最少为1天")
    @Max(value = 365, message = "指标计算时间窗口最多为1年")
    private Integer indicatorTimeWindow; // 指标计算时间窗口（天）- 从最后交易时间往前推多少天
    
    // 兼容旧版本的字段
    @Deprecated
    private Integer timeRange; // 兼容旧版本，现在由taskTimeRange替代
    
    private String description; // 任务描述（可选）
    
    // 默认构造函数
    public TaskCreateDTO() {}
    
    // 带参构造函数
    public TaskCreateDTO(String taskName, String indicatorIds, JsonNode ruleConfig, Integer timeRange) {
        this.taskName = taskName;
        this.indicatorIds = indicatorIds;
        this.ruleConfig = ruleConfig;
        this.timeRange = timeRange;
    }
    
    // Getter和Setter方法
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public String getIndicatorIds() {
        return indicatorIds;
    }
    
    public void setIndicatorIds(String indicatorIds) {
        this.indicatorIds = indicatorIds;
    }
    
    public JsonNode getRuleConfig() {
        return ruleConfig;
    }
    
    public void setRuleConfig(JsonNode ruleConfig) {
        this.ruleConfig = ruleConfig;
    }
    
    public Integer getTimeRange() {
        return timeRange;
    }
    
    public void setTimeRange(Integer timeRange) {
        this.timeRange = timeRange;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public String getVictimIndicatorIds() {
        return victimIndicatorIds;
    }
    
    public void setVictimIndicatorIds(String victimIndicatorIds) {
        this.victimIndicatorIds = victimIndicatorIds;
    }
    
    public String getSuspectIndicatorIds() {
        return suspectIndicatorIds;
    }
    
    public void setSuspectIndicatorIds(String suspectIndicatorIds) {
        this.suspectIndicatorIds = suspectIndicatorIds;
    }
    
    public Integer getTaskTimeRange() {
        return taskTimeRange;
    }
    
    public void setTaskTimeRange(Integer taskTimeRange) {
        this.taskTimeRange = taskTimeRange;
    }
    
    public Integer getIndicatorTimeWindow() {
        return indicatorTimeWindow;
    }
    
    public void setIndicatorTimeWindow(Integer indicatorTimeWindow) {
        this.indicatorTimeWindow = indicatorTimeWindow;
    }
    
    @Override
    public String toString() {
        return "TaskCreateDTO{" +
                "taskName='" + taskName + '\'' +
                ", indicatorIds='" + indicatorIds + '\'' +
                ", timeRange=" + timeRange +
                '}';
    }
}
