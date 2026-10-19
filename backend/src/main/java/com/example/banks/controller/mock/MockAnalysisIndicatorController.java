package com.example.banks.controller.mock;

import com.example.banks.entity.AnalysisIndicator;
import com.example.banks.util.MockDataGenerator;
import com.example.banks.util.ResponseTimeSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 压测专用 - 指标管理Controller
 * 路径：/api/mock/indicators
 */
@RestController
@RequestMapping("/mock/indicators")
@CrossOrigin(origins = "*")
public class MockAnalysisIndicatorController {
    
    private static final Logger logger = LoggerFactory.getLogger(MockAnalysisIndicatorController.class);
    
    /**
     * 获取所有指标（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping
    public ResponseEntity<List<AnalysisIndicator>> getAllIndicators() {
        logger.info("[压测] 获取所有指标列表");
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<AnalysisIndicator> indicators = MockDataGenerator.generateIndicators(6);
        
        logger.info("[压测] 返回 {} 条指标记录", indicators.size());
        return ResponseEntity.ok(indicators);
    }
    
    /**
     * 获取核心指标（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping("/core")
    public ResponseEntity<List<AnalysisIndicator>> getCoreIndicators() {
        logger.info("[压测] 获取核心指标");
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<AnalysisIndicator> indicators = MockDataGenerator.generateIndicators(3);
        
        logger.info("[压测] 返回 {} 条核心指标", indicators.size());
        return ResponseEntity.ok(indicators);
    }
    
    /**
     * 获取扩展指标（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping("/extended")
    public ResponseEntity<List<AnalysisIndicator>> getExtendedIndicators() {
        logger.info("[压测] 获取扩展指标");
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<AnalysisIndicator> indicators = MockDataGenerator.generateIndicators(10);
        
        logger.info("[压测] 返回 {} 条扩展指标", indicators.size());
        return ResponseEntity.ok(indicators);
    }
    
    /**
     * 获取单个指标详情（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping("/{indicatorId}")
    public ResponseEntity<AnalysisIndicator> getIndicator(@PathVariable Long indicatorId) {
        logger.info("[压测] 获取指标详情: indicatorId={}", indicatorId);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        List<AnalysisIndicator> indicators = MockDataGenerator.generateIndicators(1);
        AnalysisIndicator indicator = indicators.get(0);
        indicator.setIndicatorId(indicatorId);
        
        logger.info("[压测] 返回指标: {}", indicator.getIndicatorName());
        return ResponseEntity.ok(indicator);
    }
    
    /**
     * 创建指标（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @PostMapping
    public ResponseEntity<AnalysisIndicator> createIndicator(@RequestBody AnalysisIndicator indicator) {
        logger.info("[压测] 创建指标: {}", indicator.getIndicatorName());
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        indicator.setIndicatorId(ThreadLocalRandom.current().nextLong(1000, 999999));
        indicator.setCreateTime(LocalDateTime.now());
        indicator.setUpdateTime(LocalDateTime.now());
        indicator.setIsActive((short) 1);
        
        logger.info("[压测] 指标创建成功，ID: {}", indicator.getIndicatorId());
        return ResponseEntity.ok(indicator);
    }
    
    /**
     * 更新指标（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @PutMapping("/{indicatorId}")
    public ResponseEntity<AnalysisIndicator> updateIndicator(
            @PathVariable Long indicatorId,
            @RequestBody AnalysisIndicator indicator) {
        
        logger.info("[压测] 更新指标: indicatorId={}", indicatorId);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        indicator.setIndicatorId(indicatorId);
        indicator.setUpdateTime(LocalDateTime.now());
        
        logger.info("[压测] 指标更新成功");
        return ResponseEntity.ok(indicator);
    }
    
    /**
     * 删除指标（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @DeleteMapping("/{indicatorId}")
    public ResponseEntity<Map<String, Object>> deleteIndicator(@PathVariable Long indicatorId) {
        logger.info("[压测] 删除指标: indicatorId={}", indicatorId);
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "指标删除成功（Mock）");
        response.put("indicatorId", indicatorId);
        
        logger.info("[压测] 指标删除成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 批量更新指标参数（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @PutMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchUpdateIndicators(
            @RequestBody List<AnalysisIndicator> indicators) {
        
        logger.info("[压测] 批量更新指标，数量: {}", indicators.size());
        
        ResponseTimeSimulator.simulateRealisticQuery();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "批量更新成功（Mock）");
        response.put("updatedCount", indicators.size());
        
        logger.info("[压测] 批量更新完成");
        return ResponseEntity.ok(response);
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
        health.put("service", "IndicatorService");
        health.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(health);
    }
}


