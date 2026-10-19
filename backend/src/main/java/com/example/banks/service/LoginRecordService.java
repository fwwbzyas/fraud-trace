package com.example.banks.service;

import com.example.banks.dto.LoginRecordDTO;
import com.example.banks.entity.Account;
import com.example.banks.entity.LoginRecord;
import com.example.banks.repository.AccountRepository;
import com.example.banks.repository.LoginRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 登录记录服务层
 */
@Service
public class LoginRecordService {

    private static final Logger logger = LoggerFactory.getLogger(LoginRecordService.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private LoginRecordRepository loginRecordRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * 分页查询所有登录记录
     */
    public Page<LoginRecordDTO> getAllLoginRecords(Pageable pageable) {
        logger.info("分页查询登录记录: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<LoginRecord> loginPage = loginRecordRepository.findAllOrderByLoginTimeDesc(pageable);
            List<LoginRecordDTO> dtoList = loginPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return new PageImpl<>(dtoList, pageable, loginPage.getTotalElements());
        } catch (Exception e) {
            logger.error("查询登录记录失败", e);
            throw new RuntimeException("查询登录记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据条件搜索登录记录（优化版：使用数据库查询，避免加载全部数据）
     */
    public Page<LoginRecordDTO> searchLoginRecords(
            String accountName, String loginIp,
            LocalDateTime startTime, LocalDateTime endTime,
            Pageable pageable) {
        
        logger.info("搜索登录记录 - 账户名: {}, 地区: {}, 时间范围: {}-{}", 
                accountName, loginIp, startTime, endTime);
        
        try {
            // 处理空字符串参数
            String searchAccountName = (accountName != null && accountName.trim().isEmpty()) ? null : accountName;
            String searchLoginIp = (loginIp != null && loginIp.trim().isEmpty()) ? null : loginIp;
            
            // 转换 LocalDateTime 为 String（用于原生SQL查询）
            String startTimeStr = (startTime != null) ? startTime.format(DATETIME_FORMATTER) : null;
            String endTimeStr = (endTime != null) ? endTime.format(DATETIME_FORMATTER) : null;
            
            // 使用数据库级别的查询
            Page<LoginRecord> recordPage = loginRecordRepository.searchLoginRecords(
                    searchAccountName, searchLoginIp, startTimeStr, endTimeStr, pageable);
            
            // 转换为DTO
            List<LoginRecordDTO> dtoList = recordPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return new PageImpl<>(dtoList, pageable, recordPage.getTotalElements());
            
        } catch (Exception e) {
            logger.error("搜索登录记录失败", e);
            throw new RuntimeException("搜索登录记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据ID查询登录记录
     */
    public LoginRecordDTO getLoginRecordById(Long id) {
        logger.info("根据ID查询登录记录: {}", id);
        
        Optional<LoginRecord> optional = loginRecordRepository.findById(id);
        if (optional.isPresent()) {
            return convertToDTO(optional.get());
        } else {
            throw new RuntimeException("登录记录不存在: " + id);
        }
    }

    /**
     * 获取指定账户的登录记录
     */
    public List<LoginRecordDTO> getAccountLoginRecords(Long accountId) {
        logger.info("获取账户登录记录: {}", accountId);
        
        List<LoginRecord> records = loginRecordRepository.findByAccountId(accountId);
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    /**
     * 获取登录统计信息
     */
    public Map<String, Object> getLoginStatistics() {
        logger.info("获取登录统计信息");
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 今日时间范围
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime todayEnd = todayStart.plusDays(1);
            
            logger.info("开始执行统计查询...");
            long startTime = System.currentTimeMillis();
            
            // 分开查询，避免复杂SQL导致的问题
            long totalCount = loginRecordRepository.countAllLogins();
            long todayCount = loginRecordRepository.countByLoginTimeBetween(todayStart, todayEnd);
            long uniqueIpCount = loginRecordRepository.countDistinctLoginIps();
            
            long queryTime = System.currentTimeMillis() - startTime;
            logger.info("查询完成，耗时: {} ms", queryTime);
            
            logger.info("统计数据 - 总记录数: {}, 今日记录数: {}, 独立IP数: {}", 
                    totalCount, todayCount, uniqueIpCount);
            
            statistics.put("total_count", totalCount);
            statistics.put("today_count", todayCount);
            statistics.put("unique_ip_count", uniqueIpCount);
            
            return statistics;
        } catch (Exception e) {
            logger.error("获取登录统计信息失败", e);
            // 返回默认统计信息
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("total_count", 0L);
            defaultStats.put("today_count", 0L);
            defaultStats.put("unique_ip_count", 0L);
            return defaultStats;
        }
    }

    /**
     * 获取账户登录统计
     */
    public Map<String, Object> getAccountLoginStatistics(Long accountId) {
        logger.info("获取账户登录统计: {}", accountId);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // 登录记录
        List<LoginRecord> records = loginRecordRepository.findByAccountId(accountId);
        statistics.put("loginCount", records.size());
        
        
        // 最后登录时间
        Optional<LoginRecord> lastLogin = records.stream()
                .max(Comparator.comparing(LoginRecord::getLoginTime));
        
        if (lastLogin.isPresent()) {
            statistics.put("lastLoginTime", lastLogin.get().getLoginTime().format(DATETIME_FORMATTER));
        } else {
            statistics.put("lastLoginTime", "无");
        }
        
        // 登录IP数量
        long uniqueIpCount = records.stream()
                .map(LoginRecord::getLoginIp)
                .distinct()
                .count();
        statistics.put("uniqueIpCount", uniqueIpCount);
        
        return statistics;
    }

    /**
     * 转换实体为DTO，查询账户真实信息
     */
    private LoginRecordDTO convertToDTO(LoginRecord record) {
        LoginRecordDTO dto = new LoginRecordDTO();
        
        dto.setRecordId(record.getRecordId());
        dto.setAccountId(record.getAccountId());
        dto.setLoginTime(record.getLoginTime());
        dto.setLoginIp(record.getLoginIp());
        
        // 查询账户信息
        try {
            Optional<Account> account = accountRepository.findById(record.getAccountId());
            if (account.isPresent()) {
                dto.setAccountName(account.get().getHolderName());
                dto.setAccountNo(account.get().getAccountNo());
                dto.setPhone(account.get().getPhone());
            } else {
                dto.setAccountName("未知账户-" + record.getAccountId());
                dto.setAccountNo("");
                dto.setPhone("");
            }
        } catch (Exception e) {
            logger.warn("查询账户信息失败: {}", e.getMessage());
            dto.setAccountName("查询失败-" + record.getAccountId());
            dto.setAccountNo("");
            dto.setPhone("");
        }
        
        // 设置格式化字段
        dto.setLoginTimeDisplay(record.getLoginTime().format(DATETIME_FORMATTER));
        
        return dto;
    }
    
}

