package com.example.banks.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转账记录DTO类
 * 用于前端数据传输，包含关联账户信息
 */
public class TransferRecordDTO {
    
    private Long transferId;
    private Long fromAccountId;
    private Long toAccountId;
    private String transferNo;
    private BigDecimal amount;
    private LocalDateTime transferTime;
    
    // 关联账户信息
    private String fromAccountName;
    private String toAccountName;
    private String fromAccountPhone;
    private String toAccountPhone;
    
    // 业务字段
    private String transferDirection; // "OUT"转出, "IN"转入
    private String amountDisplay; // 格式化后的金额显示
    private String transferTimeDisplay; // 格式化后的时间显示
    private Boolean isLargeAmount; // 是否大额交易（>=50000）
    
    // 构造函数
    public TransferRecordDTO() {}
    
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
    
    public String getTransferDirection() {
        return transferDirection;
    }
    
    public void setTransferDirection(String transferDirection) {
        this.transferDirection = transferDirection;
    }
    
    public String getAmountDisplay() {
        return amountDisplay;
    }
    
    public void setAmountDisplay(String amountDisplay) {
        this.amountDisplay = amountDisplay;
    }
    
    public String getTransferTimeDisplay() {
        return transferTimeDisplay;
    }
    
    public void setTransferTimeDisplay(String transferTimeDisplay) {
        this.transferTimeDisplay = transferTimeDisplay;
    }
    
    public Boolean getIsLargeAmount() {
        return isLargeAmount;
    }
    
    public void setIsLargeAmount(Boolean isLargeAmount) {
        this.isLargeAmount = isLargeAmount;
    }
}
