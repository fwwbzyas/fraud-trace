package com.example.banks.controller.mock;

import com.example.banks.entity.TransferRecord;
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
 * 压测专用 - 转账记录Controller
 * 路径：/api/mock/transfers
 */
@RestController
@RequestMapping("/mock/transfers")
//@CrossOrigin(origins = "*")
public class MockTransferRecordController {
    
    private static final Logger logger = LoggerFactory.getLogger(MockTransferRecordController.class);
    
    /**
     * 分页获取转账记录（压测版）
     * 模拟延迟：根据页大小动态调整
     */
    @GetMapping
    public ResponseEntity<Page<TransferRecord>> getTransferRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 分页获取转账记录: page={}, size={}", page, size);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        // 生成Mock数据
        List<TransferRecord> records = MockDataGenerator.generateTransferRecords(size);
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferRecord> recordPage = new PageImpl<>(records, pageable, 10000); // 假设总共10000条
        
        logger.info("[压测] 返回第 {} 页，共 {} 条转账记录", page, records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 按账户ID获取转账记录（压测版）
     * 模拟延迟：150-300ms
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransferRecord>> getTransfersByAccount(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 获取账户 {} 的转账记录: page={}, size={}", accountId, page, size);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<TransferRecord> records = MockDataGenerator.generateTransferRecords(size);
        // 设置相同的账户ID
        records.forEach(r -> {
            if (r.getTransferId() % 2 == 0) {
                r.setFromAccountId(accountId);
            } else {
                r.setToAccountId(accountId);
            }
        });
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferRecord> recordPage = new PageImpl<>(records, pageable, 500);
        
        logger.info("[压测] 返回 {} 条记录", records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 按时间范围获取转账记录（压测版）
     * 模拟延迟：200-400ms（带索引的范围查询）
     */
    @GetMapping("/dateRange")
    public ResponseEntity<Page<TransferRecord>> getTransfersByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 获取时间范围内的转账: {} 到 {}, page={}, size={}", 
                startDate, endDate, page, size);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<TransferRecord> records = MockDataGenerator.generateTransferRecords(size);
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferRecord> recordPage = new PageImpl<>(records, pageable, 5000);
        
        logger.info("[压测] 返回 {} 条记录", records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 按金额范围查询（压测版）
     * 模拟延迟：200-400ms
     */
    @GetMapping("/amountRange")
    public ResponseEntity<Page<TransferRecord>> getTransfersByAmountRange(
            @RequestParam Double minAmount,
            @RequestParam Double maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 获取金额范围内的转账: {} 到 {}", minAmount, maxAmount);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<TransferRecord> records = MockDataGenerator.generateTransferRecords(size);
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferRecord> recordPage = new PageImpl<>(records, pageable, 2000);
        
        logger.info("[压测] 返回 {} 条记录", records.size());
        return ResponseEntity.ok(recordPage);
    }
    
    /**
     * 获取单条转账详情（压测版）
     * 模拟延迟：50-100ms
     */
    @GetMapping("/{transferId}")
    public ResponseEntity<TransferRecord> getTransfer(@PathVariable Long transferId) {
        logger.info("[压测] 获取转账详情: transferId={}", transferId);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<TransferRecord> records = MockDataGenerator.generateTransferRecords(1);
        TransferRecord record = records.get(0);
        record.setTransferId(transferId);
        
        logger.info("[压测] 返回转账记录");
        return ResponseEntity.ok(record);
    }
    
    /**
     * 统计转账数据（压测版）
     * 模拟延迟：300-600ms（聚合统计）
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTransferStatistics(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        logger.info("[压测] 获取转账统计数据");
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", 100000 + (int)(Math.random() * 900000));
        statistics.put("totalAmount", 5000000.0 + Math.random() * 50000000);
        statistics.put("avgAmount", 50000.0 + Math.random() * 50000);
        statistics.put("maxAmount", 500000.0 + Math.random() * 500000);
        statistics.put("minAmount", 100.0 + Math.random() * 900);
        statistics.put("largeAmountCount", (int)(Math.random() * 5000));
        statistics.put("suspiciousCount", (int)(Math.random() * 500));
        statistics.put("timestamp", LocalDateTime.now());
        
        logger.info("[压测] 返回统计数据");
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 搜索转账记录（压测版）
     * 模拟延迟：250-500ms（全文搜索）
     */
    @GetMapping("/search")
    public ResponseEntity<Page<TransferRecord>> searchTransfers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 搜索转账记录: keyword={}", keyword);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<TransferRecord> records = MockDataGenerator.generateTransferRecords(size);
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferRecord> recordPage = new PageImpl<>(records, pageable, 300);
        
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
        health.put("service", "TransferService");
        health.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(health);
    }
}


