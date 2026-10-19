package com.example.banks.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转账记录实体类
 * 对应数据库表 transfer_records
 */
@Entity
@Table(name = "transfer_records")
public class TransferRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long transferId;
    
    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;
    
    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;
    
    @Column(name = "transfer_no", nullable = false, length = 50, unique = true)
    private String transferNo;
    
    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "transfer_time", nullable = false)
    private LocalDateTime transferTime;
    
    // 关联查询用的字段，不映射到数据库
    @Transient
    private String fromAccountName;
    
    @Transient
    private String toAccountName;
    
    @Transient
    private String fromAccountPhone;
    
    @Transient
    private String toAccountPhone;
    
    // 构造函数
    public TransferRecord() {}
    
    public TransferRecord(Long fromAccountId, Long toAccountId, String transferNo, 
                         BigDecimal amount, LocalDateTime transferTime) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.transferNo = transferNo;
        this.amount = amount;
        this.transferTime = transferTime;
    }
    
    // Getter 和 Setter 方法
    public Long getTransferId() {
        return transferId;
    }
    
    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }
    
    public Long getFromAccountId() {
        return fromAccountId;
    }
    
    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }
    
    public Long getToAccountId() {
        return toAccountId;
    }
    
    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }
    
    public String getTransferNo() {
        return transferNo;
    }
    
    public void setTransferNo(String transferNo) {
        this.transferNo = transferNo;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public LocalDateTime getTransferTime() {
        return transferTime;
    }
    
    public void setTransferTime(LocalDateTime transferTime) {
        this.transferTime = transferTime;
    }
    
    public String getFromAccountName() {
        return fromAccountName;
    }
    
    public void setFromAccountName(String fromAccountName) {
        this.fromAccountName = fromAccountName;
    }
    
    public String getToAccountName() {
        return toAccountName;
    }
    
    public void setToAccountName(String toAccountName) {
        this.toAccountName = toAccountName;
    }
    
    public String getFromAccountPhone() {
        return fromAccountPhone;
    }
    
    public void setFromAccountPhone(String fromAccountPhone) {
        this.fromAccountPhone = fromAccountPhone;
    }
    
    public String getToAccountPhone() {
        return toAccountPhone;
    }
    
    public void setToAccountPhone(String toAccountPhone) {
        this.toAccountPhone = toAccountPhone;
    }
    
    @Override
    public String toString() {
        return "TransferRecord{" +
                "transferId=" + transferId +
                ", fromAccountId=" + fromAccountId +
                ", toAccountId=" + toAccountId +
                ", transferNo='" + transferNo + '\'' +
                ", amount=" + amount +
                ", transferTime=" + transferTime +
                '}';
    }
}
