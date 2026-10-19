package com.example.banks.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 登录记录实体类
 * 对应数据库表 login_record
 */
@Entity
@Table(name = "login_record")
public class LoginRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;
    
    @Column(name = "login_ip", nullable = false, length = 50)
    private String loginIp; // 登录地区，格式：省+市，如"北京市朝阳区"
    
    
    // 关联查询用的字段，不映射到数据库
    @Transient
    private String accountName;
    
    @Transient
    private String accountNo;
    
    @Transient
    private String phone;
    
    // 构造函数
    public LoginRecord() {}
    
    public LoginRecord(Long accountId, LocalDateTime loginTime, String loginIp) {
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
    
    
    @Override
    public String toString() {
        return "LoginRecord{" +
                "recordId=" + recordId +
                ", accountId=" + accountId +
                ", loginTime=" + loginTime +
                ", loginIp='" + loginIp + '\'' +
                '}';
    }
}

