package com.example.spark.controller;

import com.example.spark.dto.AnalysisRequest;
import com.example.spark.dto.AnalysisResponse;
import com.example.spark.service.SparkAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 分析 API 控制器 (集成文件上传功能)
 */
@RestController
@RequestMapping("/api/spark")
@CrossOrigin(origins = "*") // 允许跨域请求
public class AnalysisController {

    @Autowired
    private SparkAnalysisService sparkAnalysisService;

    /**
     * 🔥🔥🔥 [新增接口] 上传数据文件到 HDFS 🔥🔥🔥
     * 前端调用此接口上传 accounts.csv, transfer_records.csv, login_record.csv
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadData(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 获取原始文件名
            String fileName = file.getOriginalFilename();

            // 调用 Service 层将文件上传至 HDFS
            String hdfsPath = sparkAnalysisService.uploadFileToHdfs(file, fileName);

            response.put("success", true);
            response.put("message", "文件上传 HDFS 成功");
            response.put("path", hdfsPath);
            response.put("fileName", fileName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 🔥 执行分离规则分析（受害人 + 可疑分子）
     */
    @PostMapping("/separate-rules-analysis")
    public ResponseEntity<AnalysisResponse> executeSeparateRulesAnalysis(@RequestBody AnalysisRequest request) {
        try {
            // 验证请求参数
            if (request.getTimeWindowDays() == null || request.getTimeWindowDays() <= 0) {
                return ResponseEntity.badRequest()
                        .body(new AnalysisResponse(false, "时间窗口天数必须大于0"));
            }

            if ((request.getVictimRule() == null || request.getVictimRule().trim().isEmpty()) &&
                    (request.getSuspectRule() == null || request.getSuspectRule().trim().isEmpty())) {
                return ResponseEntity.badRequest()
                        .body(new AnalysisResponse(false, "至少需要提供受害人规则或可疑分子规则"));
            }

            // 执行分离规则分析 (底层已适配 HDFS)
            AnalysisResponse response = sparkAnalysisService.executeSeparateRulesAnalysis(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new AnalysisResponse(false, "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 执行完整的风险分析（指标计算 + 规则评估）
     */
    @PostMapping("/risk-analysis")
    public ResponseEntity<AnalysisResponse> executeRiskAnalysis(@RequestBody AnalysisRequest request) {
        try {
            // 验证请求参数
            if (request.getTimeWindowDays() == null || request.getTimeWindowDays() <= 0) {
                return ResponseEntity.badRequest()
                        .body(new AnalysisResponse(false, "时间窗口天数必须大于0"));
            }

            // 执行完整风险分析 (底层已适配 HDFS)
            AnalysisResponse response = sparkAnalysisService.executeRiskAnalysis(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new AnalysisResponse(false, "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 执行单个指标分析
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyzeIndicator(@RequestBody AnalysisRequest request) {
        try {
            // 验证请求参数
            if (request.getIndicatorCode() == null || request.getIndicatorCode().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AnalysisResponse(false, "指标代码不能为空"));
            }

            if (request.getTimeWindowDays() == null || request.getTimeWindowDays() <= 0) {
                return ResponseEntity.badRequest()
                        .body(new AnalysisResponse(false, "时间窗口天数必须大于0"));
            }

            // 执行分析 (底层已适配 HDFS)
            AnalysisResponse response = sparkAnalysisService.executeAnalysis(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new AnalysisResponse(false, "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 获取数据源统计信息
     */
    @GetMapping("/datasource/stats")
    public ResponseEntity<Map<String, Object>> getDataSourceStats() {
        try {
            Map<String, Object> stats = sparkAnalysisService.getDataSourceStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "error_message", e.getMessage()));
        }
    }

    /**
     * API 根路径 - 显示可用端点
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> apiRoot() {
        return ResponseEntity.ok(Map.of(
                "service", "Spark Analysis API (HDFS Enabled)",
                "version", "1.1.0",
                "endpoints", Map.of(
                        "upload", "/api/spark/upload (POST) - 上传文件到 HDFS",
                        "health", "/api/spark/health",
                        "indicators", "/api/spark/indicators",
                        "analyze", "/api/spark/analyze (POST) - 单个指标分析",
                        "risk_analysis", "/api/spark/risk-analysis (POST) - 完整风险分析",
                        "datasource_stats", "/api/spark/datasource/stats"
                ),
                "status", "ready"
        ));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "Spark Analysis API",
                "mode", "HDFS Cluster Mode",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 获取支持的指标列表
     */
    @GetMapping("/indicators")
    public ResponseEntity<Map<String, Object>> getSupportedIndicators() {
        return ResponseEntity.ok(Map.of(
                "indicators", Map.of(
                        "INDICATOR_A", Map.of(
                                "name", "大额快进快出",
                                "description", "检测账户在短时间内大额转入后快速转出的行为",
                                "parameters", Map.of(
                                        "large_amount_threshold", "大额阈值（默认：50000）",
                                        "quick_transfer_minutes", "快速转账时间窗口（分钟，默认：2）",
                                        "min_transfer_amount", "最小转账金额（默认：50000）"
                                )
                        ),
                        "INDICATOR_B", Map.of(
                                "name", "登录后快速转账",
                                "description", "检测账户登录后立即进行大额转账的行为",
                                "parameters", Map.of(
                                        "large_amount_threshold", "大额阈值（默认：50000）",
                                        "quick_operation_minutes", "快速操作时间窗口（分钟，默认：5）"
                                )
                        ),
                        "INDICATOR_C", Map.of(
                                "name", "累计入账金额",
                                "description", "统计账户在指定时间窗口内的累计入账金额",
                                "parameters", Map.of(
                                        "min_single_amount", "最小单笔金额（默认：0.01）"
                                )
                        )
                )
        ));
    }
}