package com.example.banks.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 指标配置数据传输对象
 * 用于前后端数据交互
 */
public class IndicatorConfigDTO {
    
    private Long indicatorId;
    
    @NotBlank(message = "指标代码不能为空")
    private String indicatorCode;
    
    @NotBlank(message = "指标名称不能为空")
    private String indicatorName;
    
    @NotBlank(message = "指标描述不能为空")
    private String indicatorDesc;
    
    @NotNull(message = "参数配置不能为空")
    private JsonNode paramConfig;
    
    private Short isCore = 1;
    private Short isActive = 1;
    
    // 默认构造函数
    public IndicatorConfigDTO() {}
    
    // 带参构造函数
    public IndicatorConfigDTO(String indicatorCode, String indicatorName, String indicatorDesc, 
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
}
