package com.example.banks.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 账户实体类
 * 对应数据库中的account表
 */
@Entity
@Table(name = "account")
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "account_no", nullable = false, unique = true, length = 50)
    private String accountNo;
    
    @Column(name = "holder_name", nullable = false, length = 100)
    private String holderName;
    
    @Column(name = "id_card_no", nullable = false, unique = true, length = 20)
    private String idCardNo;
    
    @Column(name = "phone", nullable = false, unique = true, length = 20)
    private String phone;
    
    @Column(name = "account_type", nullable = false)
    private Short accountType; // 1=个人，2=企业
    
    @Column(name = "status", nullable = false)
    private Short status = 0; // 0=正常，1=可疑
    
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    // 默认构造函数
    public Account() {}
    
    // 带参构造函数
    public Account(String accountNo, String holderName, String idCardNo, 
                  String phone, Short accountType) {
        this.accountNo = accountNo;
        this.holderName = holderName;
        this.idCardNo = idCardNo;
        this.phone = phone;
        this.accountType = accountType;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getAccountNo() {
        return accountNo;
    }
    
    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }
    
    public String getHolderName() {
        return holderName;
    }
    
    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }
    
    public String getIdCardNo() {
        return idCardNo;
    }
    
    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Short getAccountType() {
        return accountType;
    }
    
    public void setAccountType(Short accountType) {
        this.accountType = accountType;
    }
    
    public Short getStatus() {
        return status;
    }
    
    public void setStatus(Short status) {
        this.status = status;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    

    public boolean isPersonal() {
        return accountType != null && accountType == 1;
    }
    
    public boolean isEnterprise() {
        return accountType != null && accountType == 2;
    }
    
    public boolean isNormal() {
        return status != null && status == 0;
    }
    
    public boolean isSuspicious() {
        return status != null && status == 1;
    }
    
    public String getAccountTypeName() {
        if (accountType == null) return "未知";
        switch (accountType) {
            case 0: return "个人";
            case 1: return "企业";
            default: return "未知";
        }
    }
    
    public String getStatusName() {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "正常";
            case 1: return "可疑";
            default: return "未知";
        }
    }
    
    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", accountNo='" + accountNo + '\'' +
                ", holderName='" + holderName + '\'' +
                ", accountType=" + accountType +
                ", status=" + status +
                '}';
    }
}
