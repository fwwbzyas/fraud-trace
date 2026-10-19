package com.example.banks.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

/**
 * Spark 集成服务 - 调用 fraudanalysis 项目的 Spark API
 */
@Service
public class SparkIntegrationService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String SPARK_API_BASE_URL = "http://localhost:8083/api/spark";
    
    /**
     * 执行 Spark 指标分析
     */
    public Map<String, Object> executeSparkAnalysis(String indicatorCode, Map<String, Object> params) {
        try {
            String url = SPARK_API_BASE_URL + "/analyze";
            
            // 构建请求参数
            Map<String, Object> request = new HashMap<>();
            request.put("indicator_code", indicatorCode);
            request.put("time_window_days", params.getOrDefault("timeWindowDays", 30));
            request.put("large_amount_threshold", params.getOrDefault("largeAmountThreshold", 50000.0));
            request.put("quick_transfer_minutes", params.getOrDefault("quickTransferMinutes", 2));
            request.put("min_transfer_amount", params.getOrDefault("minTransferAmount", 50000.0));
            request.put("quick_operation_minutes", params.getOrDefault("quickOperationMinutes", 5));
            request.put("min_single_amount", params.getOrDefault("minSingleAmount", 0.01));
            
            // 调用 Spark API
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                return Map.of("success", false, "message", "Spark API 调用失败");
            }
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Spark API 调用异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取 Spark 服务状态
     */
    public Map<String, Object> getSparkServiceStatus() {
        try {
            String url = SPARK_API_BASE_URL + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            return Map.of("status", "error", "message", "无法连接到 Spark 服务");
        }
    }
    
    /**
     * 获取数据源统计信息
     */
    public Map<String, Object> getDataSourceStatistics() {
        try {
            String url = SPARK_API_BASE_URL + "/datasource/stats";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            return Map.of("status", "error", "message", "无法获取数据源统计");
        }
    }
}
