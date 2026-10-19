package com.example.banks.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务结果详情DTO
 * 包含任务基本信息、风险结果统计、账户详情等
 */
public class TaskResultDTO {

    // 任务基本信息
    private Long taskId;
    private String taskName;
    private String taskCode;
    private JsonNode ruleConfig;
    private Integer timeRange;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Short taskStatus;
    private Integer analyzedAccounts;
    private Integer resultCount;

    // 分页信息
    private Integer currentPage;
    private Integer pageSize;
    private Integer totalPages;

    // 风险结果统计
    private RiskStatistics statistics;

    // 风险账户列表
    private List<RiskAccountDTO> riskAccounts;

    /**
     * 风险统计信息
     */
    public static class RiskStatistics {
        private long totalRiskAccounts;        // 总风险账户数
        private long victimAccounts;           // 受害人账户数
        private long suspiciousAccounts;       // 可疑账户数
        private long totalAccounts;            // 总账户数（从account表获取）
        private Map<String, Long> accountRoleDistribution; // 账户角色分布
        
        // 添加计算属性：风险占比
        public double getRiskRatio() {
            return totalAccounts > 0 ? (double) totalRiskAccounts / totalAccounts * 100 : 0.0;
        }

        // Getters and Setters
        public long getTotalRiskAccounts() { return totalRiskAccounts; }
        public void setTotalRiskAccounts(long totalRiskAccounts) { this.totalRiskAccounts = totalRiskAccounts; }

        public long getVictimAccounts() { return victimAccounts; }
        public void setVictimAccounts(long victimAccounts) { this.victimAccounts = victimAccounts; }

        public long getSuspiciousAccounts() { return suspiciousAccounts; }
        public void setSuspiciousAccounts(long suspiciousAccounts) { this.suspiciousAccounts = suspiciousAccounts; }

        public long getTotalAccounts() { return totalAccounts; }
        public void setTotalAccounts(long totalAccounts) { this.totalAccounts = totalAccounts; }

        public Map<String, Long> getAccountRoleDistribution() { return accountRoleDistribution; }
        public void setAccountRoleDistribution(Map<String, Long> accountRoleDistribution) { this.accountRoleDistribution = accountRoleDistribution; }
    }

    /**
     * 风险账户信息
     */
    public static class RiskAccountDTO {
        private Long riskId;
        private Long accountId;
        private String accountNo;
        private String holderName;
        private String idCardNo;
        private String phone;
        private Short accountType;
        private Short accountStatus;
        private Short accountRole;
        private String accountRoleText;
        private JsonNode matchedIndicators;
        private String matchedRules;
        private LocalDateTime createTime;

        // Getters and Setters
        public Long getRiskId() { return riskId; }
        public void setRiskId(Long riskId) { this.riskId = riskId; }

        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }

        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

        public String getHolderName() { return holderName; }
        public void setHolderName(String holderName) { this.holderName = holderName; }

        public String getIdCardNo() { return idCardNo; }
        public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public Short getAccountType() { return accountType; }
        public void setAccountType(Short accountType) { this.accountType = accountType; }

        public Short getAccountStatus() { return accountStatus; }
        public void setAccountStatus(Short accountStatus) { this.accountStatus = accountStatus; }

        /**
         * 获取账户状态显示文本
         */
        public String getAccountStatusText() {
            if (accountStatus == null) return "未知";
            switch (accountStatus) {
                case 0: return "正常";
                case 1: return "可疑";
                default: return "未知";
            }
        }

        public Short getAccountRole() { return accountRole; }
        public void setAccountRole(Short accountRole) { this.accountRole = accountRole; }

        public String getAccountRoleText() { return accountRoleText; }
        public void setAccountRoleText(String accountRoleText) { this.accountRoleText = accountRoleText; }

        public JsonNode getMatchedIndicators() { return matchedIndicators; }
        public void setMatchedIndicators(JsonNode matchedIndicators) { this.matchedIndicators = matchedIndicators; }

        public String getMatchedRules() { return matchedRules; }
        public void setMatchedRules(String matchedRules) { this.matchedRules = matchedRules; }

        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }

    // 构造函数
    public TaskResultDTO() {}

    // Getters and Setters
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }

    public JsonNode getRuleConfig() { return ruleConfig; }
    public void setRuleConfig(JsonNode ruleConfig) { this.ruleConfig = ruleConfig; }

    public Integer getTimeRange() { return timeRange; }
    public void setTimeRange(Integer timeRange) { this.timeRange = timeRange; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Short getTaskStatus() { return taskStatus; }
    public void setTaskStatus(Short taskStatus) { this.taskStatus = taskStatus; }

    public Integer getAnalyzedAccounts() { return analyzedAccounts; }
    public void setAnalyzedAccounts(Integer analyzedAccounts) { this.analyzedAccounts = analyzedAccounts; }

    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }

    public RiskStatistics getStatistics() { return statistics; }
    public void setStatistics(RiskStatistics statistics) { this.statistics = statistics; }

    public List<RiskAccountDTO> getRiskAccounts() { return riskAccounts; }
    public void setRiskAccounts(List<RiskAccountDTO> riskAccounts) { this.riskAccounts = riskAccounts; }

    public Integer getCurrentPage() { return currentPage; }
    public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
}

