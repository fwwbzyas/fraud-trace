package com.example.banks.controller.mock;

import com.example.banks.entity.LoginRecord;
import com.example.banks.util.MockDataGenerator;
import com.example.banks.util.ResponseTimeSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 压测专用 - 登录记录Controller
 * 路径：/api/mock/logins
 */
@RestController
@RequestMapping("/mock/logins")
//@CrossOrigin(origins = "*")
public class MockLoginRecordController {
    
    private static final Logger logger = LoggerFactory.getLogger(MockLoginRecordController.class);
    
    /**
     * 分页获取登录记录（压测版）
     * 模拟延迟：根据页大小动态调整
     */
    @GetMapping
    public ResponseEntity<Page<LoginRecord>> getLoginRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 分页获取登录记录: page={}, size={}", page, size);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        // 生成Mock数据
        List<LoginRecord> records = MockDataGenerator.generateLoginRecords(size);
        Pageable pageable = PageRequest.of(page, size);
        Page<LoginRecord> recordPage = new PageImpl<>(records, pageable, 15000); // 假设总共15000条
        
        logger.info("[压测] 返回第 {} 页，共 {} 条登录记录", page, records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 按账户ID获取登录记录（压测版）
     * 模拟延迟：150-300ms
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<LoginRecord>> getLoginsByAccount(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 获取账户 {} 的登录记录: page={}, size={}", accountId, page, size);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<LoginRecord> records = MockDataGenerator.generateLoginRecords(size);
        // 设置相同的账户ID
        records.forEach(r -> r.setAccountId(accountId));
        
        Pageable pageable = PageRequest.of(page, size);
        Page<LoginRecord> recordPage = new PageImpl<>(records, pageable, 300);
        
        logger.info("[压测] 返回 {} 条记录", records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 按时间范围获取登录记录（压测版）
     * 模拟延迟：200-400ms
     */
    @GetMapping("/dateRange")
    public ResponseEntity<Page<LoginRecord>> getLoginsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 获取时间范围内的登录: {} 到 {}, page={}, size={}", 
                startDate, endDate, page, size);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<LoginRecord> records = MockDataGenerator.generateLoginRecords(size);
        Pageable pageable = PageRequest.of(page, size);
        Page<LoginRecord> recordPage = new PageImpl<>(records, pageable, 8000);
        
        logger.info("[压测] 返回 {} 条记录", records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 按IP地址查询（压测版）
     * 模拟延迟：150-300ms
     */
    @GetMapping("/ip/{loginIp}")
    public ResponseEntity<Page<LoginRecord>> getLoginsByIp(
            @PathVariable String loginIp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 获取IP {} 的登录记录", loginIp);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<LoginRecord> records = MockDataGenerator.generateLoginRecords(size);
        records.forEach(r -> r.setLoginIp(loginIp));
        
        Pageable pageable = PageRequest.of(page, size);
        Page<LoginRecord> recordPage = new PageImpl<>(records, pageable, 150);
        
        logger.info("[压测] 返回 {} 条记录", records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 获取单条登录详情（压测版）
     * 模拟延迟：50-100ms
     */
    @GetMapping("/{recordId}")
    public ResponseEntity<LoginRecord> getLogin(@PathVariable Long recordId) {
        logger.info("[压测] 获取登录详情: recordId={}", recordId);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<LoginRecord> records = MockDataGenerator.generateLoginRecords(1);
        LoginRecord record = records.get(0);
        record.setRecordId(recordId);
        
        logger.info("[压测] 返回登录记录");
        return ResponseEntity.ok(record);
    }
    
    /**
     * 获取异地登录记录（压测版）
     * 模拟延迟：250-500ms（复杂查询）
     */
    @GetMapping("/remote-logins")
    public ResponseEntity<Page<LoginRecord>> getRemoteLogins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 获取异地登录记录");
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<LoginRecord> records = MockDataGenerator.generateLoginRecords(size);
        Pageable pageable = PageRequest.of(page, size);
        Page<LoginRecord> recordPage = new PageImpl<>(records, pageable, 1200);
        
        logger.info("[压测] 返回 {} 条异地登录记录", records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 获取频繁登录账户（压测版）
     * 模拟延迟：300-600ms（GROUP BY统计）
     */
    @GetMapping("/frequent-accounts")
    public ResponseEntity<List<Map<String, Object>>> getFrequentLoginAccounts(
            @RequestParam(defaultValue = "10") int topN) {
        
        logger.info("[压测] 获取登录频繁的Top {} 账户", topN);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<Map<String, Object>> frequentAccounts = new java.util.ArrayList<>();
        for (int i = 0; i < topN; i++) {
            Map<String, Object> account = new HashMap<>();
            account.put("accountId", 1000L + i);
            account.put("accountName", "用户" + String.format("%04d", i));
            account.put("loginCount", 100 + (int)(Math.random() * 900));
            account.put("lastLoginTime", LocalDateTime.now().minusHours((int)(Math.random() * 24)));
            frequentAccounts.add(account);
        }
        
        logger.info("[压测] 返回 {} 个频繁登录账户", topN);
        return ResponseEntity.ok(frequentAccounts);
    }
    
    /**
     * 统计登录数据（压测版）
     * 模拟延迟：300-600ms（聚合统计）
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLoginStatistics(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        logger.info("[压测] 获取登录统计数据");
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", 50000 + (int)(Math.random() * 450000));
        statistics.put("uniqueAccounts", 5000 + (int)(Math.random() * 45000));
        statistics.put("uniqueIps", 3000 + (int)(Math.random() * 27000));
        statistics.put("remoteLoginCount", 500 + (int)(Math.random() * 4500));
        statistics.put("suspiciousCount", 50 + (int)(Math.random() * 450));
        statistics.put("todayCount", 100 + (int)(Math.random() * 900));
        statistics.put("timestamp", LocalDateTime.now());
        
        logger.info("[压测] 返回统计数据");
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 搜索登录记录（压测版）
     * 模拟延迟：250-500ms
     */
    @GetMapping("/search")
    public ResponseEntity<Page<LoginRecord>> searchLogins(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 搜索登录记录: keyword={}", keyword);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<LoginRecord> records = MockDataGenerator.generateLoginRecords(size);
        Pageable pageable = PageRequest.of(page, size);
        Page<LoginRecord> recordPage = new PageImpl<>(records, pageable, 200);
        
        logger.info("[压测] 搜索返回 {} 条记录", records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        ResponseTimeSimulator.simulateDelay(10, 30);
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("mode", "MOCK_MODE");
        health.put("service", "LoginService");
        health.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(health);
    }
}


