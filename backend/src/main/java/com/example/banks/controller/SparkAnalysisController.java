package com.example.banks.controller;

import com.example.banks.entity.AnalysisIndicator;
import com.example.banks.engine.SqlBasedRuleEngine;
import com.example.banks.service.IntegratedSparkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Banks 项目的 Spark 分析控制器 (HDFS 升级版)
 * 核心功能：
 * 1. 接收文件上传并存入 Hadoop HDFS
 * 2. 触发 Spark 读取 HDFS 数据进行分析
 */
@RestController
@RequestMapping("/spark") // 建议改为 /api/spark 以区分旧接口，或者保持 /api/analysis
@CrossOrigin(origins = "*")
public class SparkAnalysisController {

    @Autowired
    private IntegratedSparkService integratedSparkService;

    /**
     * 📤 【核心接口 1】上传数据文件到 Hadoop (HDFS)
     * 前端/Postman 调用此接口上传 csv 文件
     * * @param file 上传的文件
     * @return 上传结果和 HDFS 路径
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadData(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 获取原始文件名 (例如 transfer_records.csv)
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "upload_data_" + System.currentTimeMillis() + ".csv";
            }

            // 调用服务层，上传到 HDFS
            String hdfsPath = integratedSparkService.uploadFileToHdfs(file, fileName);

            response.put("success", true);
            response.put("message", "文件上传 Hadoop 成功");
            response.put("hdfsPath", hdfsPath);
            response.put("fileName", fileName);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * ⚙️ 【核心接口 2】触发 Spark 分析任务 (基于 HDFS 数据)
     * 可以通过 Postman 传入简单的参数来测试
     */
    @PostMapping("/analyze")
    public ResponseEntity<Object> startAnalysis(
            @RequestParam(required = false) String victimRule,
            @RequestParam(required = false) String suspectRule,
            @RequestParam(required = false, defaultValue = "30") Integer timeWindow) {

        try {
            // 为了演示方便，这里构造一些默认的指标配置
            // 实际场景中，这些应该由前端 JSON Body 传入
            List<AnalysisIndicator> defaultIndicators = createDefaultIndicators();

            // 如果前端没传规则，使用默认测试规则
            if (victimRule == null) {
                victimRule = "INDICATOR_A > 0"; // 默认寻找大额快进快出
            }
            if (suspectRule == null) {
                suspectRule = "INDICATOR_B > 0"; // 默认寻找登录后急转账
            }

            // 调用集成服务执行分析
            SqlBasedRuleEngine.CombinedRuleResult result = integratedSparkService.executeSeparateRules(
                    victimRule,
                    suspectRule,
                    defaultIndicators, // 受害人指标集
                    defaultIndicators, // 可疑分子指标集
                    30,
                    timeWindow
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "分析任务执行失败: " + e.getMessage());
            // 如果是因为数据没上传导致的，提示用户
            if (e.getMessage().contains("Path does not exist") || e.getMessage().contains("Input path does not exist")) {
                errorResponse.put("hint", "请先调用 /api/spark/upload 接口上传 accounts.csv, transfer_records.csv 和 login_records.csv");
            }
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 🛠️ 【辅助接口】手动初始化 Spark 会话
     */
    @GetMapping("/init")
    public ResponseEntity<String> initSpark() {
        try {
            integratedSparkService.initializeSpark();
            return ResponseEntity.ok("Spark Session 已成功连接到 Hadoop 集群");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 📊 【辅助接口】获取支持的指标列表 (保留原功能)
     * 用于前端展示有哪些指标可用
     */
    @GetMapping("/indicators")
    public ResponseEntity<Map<String, Object>> getSupportedIndicators() {
        return ResponseEntity.ok(Map.of(
                "indicators", Map.of(
                        "INDICATOR_A", Map.of(
                                "name", "大额快进快出",
                                "description", "检测账户在短时间内大额转入后快速转出的行为",
                                "code", "INDICATOR_A"
                        ),
                        "INDICATOR_B", Map.of(
                                "name", "登录后快速转账",
                                "description", "检测账户登录后立即进行大额转账的行为",
                                "code", "INDICATOR_B"
                        ),
                        "INDICATOR_C", Map.of(
                                "name", "累计入账金额",
                                "description", "统计账户在指定时间窗口内的累计入账金额",
                                "code", "INDICATOR_C"
                        )
                )
        ));
    }

    // --- 私有辅助方法：创建默认指标配置 (用于测试) ---
    private List<AnalysisIndicator> createDefaultIndicators() {
        List<AnalysisIndicator> list = new ArrayList<>();

        // 指标A: 快进快出
        AnalysisIndicator indA = new AnalysisIndicator();
        indA.setIndicatorCode("INDICATOR_A");
        indA.setParamConfig(createJsonNode("{\"large_amount_threshold\": 50000.0, \"quick_transfer_minutes\": 10}"));
        list.add(indA);

        // 指标B: 登录后急转
        AnalysisIndicator indB = new AnalysisIndicator();
        indB.setIndicatorCode("INDICATOR_B");
        indB.setParamConfig(createJsonNode("{\"large_amount_threshold\": 50000.0}"));
        list.add(indB);

        // 指标C: 累计入账
        AnalysisIndicator indC = new AnalysisIndicator();
        indC.setIndicatorCode("INDICATOR_C");
        indC.setParamConfig(createJsonNode("{\"minSingleAmount\": 1.0}"));
        list.add(indC);

        return list;
    }

    private com.fasterxml.jackson.databind.JsonNode createJsonNode(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}