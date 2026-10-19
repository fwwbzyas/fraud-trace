package com.example.banks.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 分析任务实体类
 * 对应数据库中的analysis_tasks表
 */
@Entity
@Table(name = "analysis_tasks")
public class AnalysisTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;
    
    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;
    
    @Column(name = "task_code", nullable = false, unique = true, length = 50)
    private String taskCode;
    
    @Column(name = "indicator_ids", nullable = false, length = 200)
    private String indicatorIds;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_config", nullable = false, columnDefinition = "jsonb")
    private JsonNode ruleConfig;
    
    @Column(name = "time_range", nullable = false)
    private Integer timeRange; // 兼容旧版本字段
    
    @Column(name = "task_time_range", nullable = false)
    private Integer taskTimeRange; // 任务筛选时间范围（天）- 决定分析哪些账户
    
    @Column(name = "indicator_time_window", nullable = false)
    private Integer indicatorTimeWindow; // 指标计算时间窗口（天）- 从最后交易时间往前推多少天
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "task_status", nullable = false)
    private Short taskStatus = 0; // 0=待执行，1=执行中，2=完成，3=失败
    
    @Column(name = "progress", nullable = false)
    private Integer progress = 0;
    
    @Column(name = "analyzed_accounts")
    private Integer analyzedAccounts = 0;
    
    @Column(name = "result_count")
    private Integer resultCount = 0;
    
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
    
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime = LocalDateTime.now();
    
    // 默认构造函数
    public AnalysisTask() {}
    
    // 带参构造函数
    public AnalysisTask(String taskName, String taskCode, String indicatorIds, 
                       JsonNode ruleConfig, Integer timeRange) {
        this.taskName = taskName;
        this.taskCode = taskCode;
        this.indicatorIds = indicatorIds;
        this.ruleConfig = ruleConfig;
        this.timeRange = timeRange;
        this.startTime = LocalDateTime.now();
    }
    
    // Getter和Setter方法
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public String getTaskCode() {
        return taskCode;
    }
    
    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
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
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Short getTaskStatus() {
        return taskStatus;
    }
    
    public void setTaskStatus(Short taskStatus) {
        this.taskStatus = taskStatus;
    }
    
    public Integer getProgress() {
        return progress;
    }
    
    public void setProgress(Integer progress) {
        this.progress = progress;
    }
    
    public Integer getAnalyzedAccounts() {
        return analyzedAccounts;
    }
    
    public void setAnalyzedAccounts(Integer analyzedAccounts) {
        this.analyzedAccounts = analyzedAccounts;
    }
    
    public Integer getResultCount() {
        return resultCount;
    }
    
    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    // 业务方法
    public boolean isCompleted() {
        return taskStatus != null && taskStatus == 2;
    }
    
    public boolean isRunning() {
        return taskStatus != null && taskStatus == 1;
    }
    
    public boolean isFailed() {
        return taskStatus != null && taskStatus == 3;
    }
    
    public String getStatusName() {
        if (taskStatus == null) return "未知";
        switch (taskStatus) {
            case 0: return "待执行";
            case 1: return "执行中";
            case 2: return "已完成";
            case 3: return "执行失败";
            default: return "未知";
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "AnalysisTask{" +
                "taskId=" + taskId +
                ", taskName='" + taskName + '\'' +
                ", taskCode='" + taskCode + '\'' +
                ", taskStatus=" + taskStatus +
                ", progress=" + progress +
                '}';
    }
}
