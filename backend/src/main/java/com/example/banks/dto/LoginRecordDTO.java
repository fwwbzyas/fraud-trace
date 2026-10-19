package com.example.banks.dto;

import java.time.LocalDateTime;

/**
 * 登录记录数据传输对象
 */
public class LoginRecordDTO {
    
    private Long recordId;
    private Long accountId;
    private LocalDateTime loginTime;
    private String loginIp; // 登录地区，格式：省+市，如“北京市朝阳区”
    
    // 关联的账户信息
    private String accountName;
    private String accountNo;
    private String phone;
    
    // 格式化显示字段
    private String loginTimeDisplay;
    
    // 构造函数
    public LoginRecordDTO() {}
    
    public LoginRecordDTO(Long recordId, Long accountId, LocalDateTime loginTime, 
                         String loginIp) {
        this.recordId = recordId;
        this.accountId = accountId;
        this.loginTime = loginTime;
        this.loginIp = loginIp;
    }
    
    // Getter 和 Setter 方法
    public Long getRecordId() {
        return recordId;
    }
    
    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
    
    public String getLoginIp() {
        return loginIp;
    }
    
    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }
    
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getAccountNo() {
        return accountNo;
    }
    
    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getLoginTimeDisplay() {
        return loginTimeDisplay;
    }
    
    public void setLoginTimeDisplay(String loginTimeDisplay) {
        this.loginTimeDisplay = loginTimeDisplay;
    }
    
    
    @Override
    public String toString() {
        return "LoginRecordDTO{" +
                "recordId=" + recordId +
                ", accountId=" + accountId +
                ", accountName='" + accountName + '\'' +
                ", loginTime=" + loginTime +
                ", loginIp='" + loginIp + '\'' +
                '}';
    }
}

