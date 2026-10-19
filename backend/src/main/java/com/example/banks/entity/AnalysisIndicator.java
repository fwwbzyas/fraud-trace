package com.example.banks.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 分析指标配置实体类
 * 对应数据库中的analysis_indicators表
 */
@Entity
@Table(name = "analysis_indicators")
public class AnalysisIndicator {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_id")
    private Long indicatorId;
    
    @Column(name = "indicator_code", nullable = false, unique = true, length = 50)
    private String indicatorCode;
    
    @Column(name = "indicator_name", nullable = false, length = 100)
    private String indicatorName;
    
    @Column(name = "indicator_desc", nullable = false, columnDefinition = "TEXT")
    private String indicatorDesc;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "param_config", nullable = false, columnDefinition = "jsonb")
    private JsonNode paramConfig;
    
    @Column(name = "is_core", nullable = false)
    private Short isCore = 1;
    
    @Column(name = "is_active", nullable = false)
    private Short isActive = 1;
    
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
    
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime = LocalDateTime.now();
    
    @Column(name = "user_type", length = 20)
    private String userType; // "payer" (付款方/受害人) 或 "payee" (收款方/可疑分子) 或 "both" (通用)
    
    // 默认构造函数
    public AnalysisIndicator() {}
    
    // 带参构造函数
    public AnalysisIndicator(String indicatorCode, String indicatorName, String indicatorDesc, 
                           JsonNode paramConfig, Short isCore) {
        this.indicatorCode = indicatorCode;
        this.indicatorName = indicatorName;
        this.indicatorDesc = indicatorDesc;
        this.paramConfig = paramConfig;
        this.isCore = isCore;
    }
    
    // Getter和Setter方法
    public Long getIndicatorId() {
        return indicatorId;
    }
    
    public void setIndicatorId(Long indicatorId) {
        this.indicatorId = indicatorId;
    }
    
    public String getIndicatorCode() {
        return indicatorCode;
    }
    
    public void setIndicatorCode(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }
    
    public String getIndicatorName() {
        return indicatorName;
    }
    
    public void setIndicatorName(String indicatorName) {
        this.indicatorName = indicatorName;
    }
    
    public String getIndicatorDesc() {
        return indicatorDesc;
    }
    
    public void setIndicatorDesc(String indicatorDesc) {
        this.indicatorDesc = indicatorDesc;
    }
    
    public JsonNode getParamConfig() {
        return paramConfig;
    }
    
    public void setParamConfig(JsonNode paramConfig) {
        this.paramConfig = paramConfig;
    }
    
    public Short getIsCore() {
        return isCore;
    }
    
    public void setIsCore(Short isCore) {
        this.isCore = isCore;
    }
    
    public Short getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Short isActive) {
        this.isActive = isActive;
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
    
    public String getUserType() {
        return userType;
    }
    
    public void setUserType(String userType) {
        this.userType = userType;
    }
    
    // 业务方法：获取指定参数值
    public Object getParamValue(String paramName) {
        if (paramConfig != null && paramConfig.has(paramName)) {
            JsonNode paramNode = paramConfig.get(paramName);
            if (paramNode.isNumber()) {
                return paramNode.asDouble();
            } else if (paramNode.isBoolean()) {
                return paramNode.asBoolean();
            } else {
                return paramNode.asText();
            }
        }
        return null;
    }
    
    // 业务方法：检查是否为核心指标
    public boolean isCoreIndicator() {
        return isCore != null && isCore == 1;
    }
    
    // 业务方法：检查是否激活
    public boolean isActiveIndicator() {
        return isActive != null && isActive == 1;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "AnalysisIndicator{" +
                "indicatorId=" + indicatorId +
                ", indicatorCode='" + indicatorCode + '\'' +
                ", indicatorName='" + indicatorName + '\'' +
                ", isCore=" + isCore +
                ", isActive=" + isActive +
                '}';
    }
}
