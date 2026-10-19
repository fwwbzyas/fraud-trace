package com.example.banks.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 风险分析结果实体类
 * 对应数据库中的risk_results表
 */
@Entity
@Table(name = "risk_results")
public class RiskResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "risk_id")
    private Long riskId;
    
    @Column(name = "task_id", nullable = false)
    private Long taskId;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "account_role", nullable = false)
    private Short accountRole; // 1=付款方/受害人，2=收款方/可疑账户
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_indicators", nullable = false, columnDefinition = "jsonb")
    private JsonNode matchedIndicators;
    
    @Column(name = "matched_rules", nullable = false, columnDefinition = "TEXT")
    private String matchedRules;
    
    
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
    
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime = LocalDateTime.now();
    
    // 关联账户信息（用于查询时连接）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;
    
    // 默认构造函数
    public RiskResult() {}
    
    // 带参构造函数
    public RiskResult(Long taskId, Long accountId, Short accountRole, 
                     JsonNode matchedIndicators, String matchedRules) {
        this.taskId = taskId;
        this.accountId = accountId;
        this.accountRole = accountRole;
        this.matchedIndicators = matchedIndicators;
        this.matchedRules = matchedRules;
    }
    
    // Getter和Setter方法
    public Long getRiskId() {
        return riskId;
    }
    
    public void setRiskId(Long riskId) {
        this.riskId = riskId;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public Short getAccountRole() {
        return accountRole;
    }
    
    public void setAccountRole(Short accountRole) {
        this.accountRole = accountRole;
    }
    
    public JsonNode getMatchedIndicators() {
        return matchedIndicators;
    }
    
    public void setMatchedIndicators(JsonNode matchedIndicators) {
        this.matchedIndicators = matchedIndicators;
    }
    
    public String getMatchedRules() {
        return matchedRules;
    }
    
    public void setMatchedRules(String matchedRules) {
        this.matchedRules = matchedRules;
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
    
    public Account getAccount() {
        return account;
    }
    
    public void setAccount(Account account) {
        this.account = account;
    }
    
    // 业务方法
    public boolean isVictim() {
        return accountRole != null && accountRole == 1;
    }
    
    public boolean isSuspicious() {
        return accountRole != null && accountRole == 2;
    }
    
    public String getAccountRoleName() {
        if (accountRole == null) return "未知";
        switch (accountRole) {
            case 1: return "付款方(受害人)";
            case 2: return "收款方(可疑账户)";
            default: return "未知";
        }
    }
    
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "RiskResult{" +
                "riskId=" + riskId +
                ", taskId=" + taskId +
                ", accountId=" + accountId +
                ", accountRole=" + accountRole +
                '}';
    }
}
