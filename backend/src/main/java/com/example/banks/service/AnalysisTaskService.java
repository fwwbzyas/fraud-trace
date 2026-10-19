package com.example.banks.service;

import com.example.banks.engine.SqlBasedRuleEngine;
import com.example.banks.entity.AnalysisTask;
import com.example.banks.entity.RiskResult;
import com.example.banks.entity.AnalysisIndicator;
import com.example.banks.dto.TaskCreateDTO;
import com.example.banks.dto.TaskResultDTO;
import com.example.banks.repository.AnalysisTaskRepository;
import com.example.banks.repository.RiskResultRepository;
import com.example.banks.repository.AnalysisIndicatorRepository;
import com.example.banks.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 分析任务业务逻辑服务
 * 提供任务管理、执行和结果查询功能
 */
@Service
@Transactional
public class AnalysisTaskService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisTaskService.class);

    @Autowired
    private AnalysisTaskRepository taskRepository;

    @Autowired
    private RiskResultRepository riskResultRepository;

    @Autowired
    private AnalysisIndicatorRepository indicatorRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SqlBasedRuleEngine sqlRuleEngine;

    @Autowired
    private SparkIntegrationService sparkIntegrationService;

    @Autowired
    private IntegratedSparkService integratedSparkService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取所有任务
     * @return 任务列表
     */
    @Transactional(readOnly = true)
    public List<AnalysisTask> getAllTasks() {
        logger.info("获取所有分析任务");
        return taskRepository.findAllOrderByCreateTimeDesc();
    }

    /**
     * 根据ID获取任务详情
     * @param taskId 任务ID
     * @return 任务实体
     */
    @Transactional(readOnly = true)
    public Optional<AnalysisTask> getTaskById(Long taskId) {
        logger.info("获取任务详情: {}", taskId);
        return taskRepository.findById(taskId);
    }

    /**
     * 根据任务代码获取任务
     * @param taskCode 任务代码
     * @return 任务实体
     */
    @Transactional(readOnly = true)
    public Optional<AnalysisTask> getTaskByCode(String taskCode) {
        logger.info("根据代码获取任务: {}", taskCode);
        return taskRepository.findByTaskCode(taskCode);
    }

    /**
     * 创建新的分析任务
     * @param dto 任务创建DTO
     * @return 创建的任务
     */
    public AnalysisTask createTask(TaskCreateDTO dto) {
        logger.info("创建新的分析任务: {}", dto.getTaskName());

        // 生成唯一任务代码
        String taskCode = generateTaskCode();
        while (taskRepository.existsByTaskCode(taskCode)) {
            taskCode = generateTaskCode();
        }

        // 创建任务实体
        AnalysisTask task = new AnalysisTask();
        task.setTaskName(dto.getTaskName());
        task.setTaskCode(taskCode);
        task.setIndicatorIds(dto.getIndicatorIds());
        task.setRuleConfig(dto.getRuleConfig());

        // 处理双时间参数
        if (dto.getTaskTimeRange() != null && dto.getIndicatorTimeWindow() != null) {
            // 新版本：使用双时间参数
            task.setTaskTimeRange(dto.getTaskTimeRange());
            task.setIndicatorTimeWindow(dto.getIndicatorTimeWindow());
            task.setTimeRange(dto.getTaskTimeRange()); // 兼容旧版本字段
        } else if (dto.getTimeRange() != null) {
            // 兼容旧版本：使用单一时间参数
            task.setTimeRange(dto.getTimeRange());
            task.setTaskTimeRange(dto.getTimeRange());
            task.setIndicatorTimeWindow(30); // 默认30天指标窗口
        } else {
            throw new IllegalArgumentException("必须提供时间范围参数");
        }

        task.setStartTime(LocalDateTime.now());
        task.setTaskStatus((short) 0); // 待执行

        AnalysisTask savedTask = taskRepository.save(task);
        logger.info("任务创建成功: {}", savedTask.getTaskCode());

        return savedTask;
    }

    /**
     * 创建新的分离规则分析任务
     * @param dto 任务创建DTO
     * @return 创建的任务
     */
    public AnalysisTask createSeparateRuleTask(TaskCreateDTO dto) {
        logger.info("创建新的分离规则分析任务: {}", dto.getTaskName());

        // 生成唯一任务代码
        String taskCode = generateTaskCode();
        while (taskRepository.existsByTaskCode(taskCode)) {
            taskCode = generateTaskCode();
        }

        // 创建任务实体
        AnalysisTask task = new AnalysisTask();
        task.setTaskName(dto.getTaskName());
        task.setTaskCode(taskCode);
        task.setIndicatorIds(dto.getIndicatorIds()); // 保留旧字段以兼容
        task.setTimeRange(dto.getTimeRange());
        task.setStartTime(LocalDateTime.now());
        task.setTaskStatus((short) 0); // 待执行

        // 构建分离规则配置
        Map<String, Object> separateRuleConfig = new HashMap<>();
        separateRuleConfig.put("type", "separate_rules");
        separateRuleConfig.put("victimRule", dto.getVictimRule());
        separateRuleConfig.put("suspectRule", dto.getSuspectRule());
        separateRuleConfig.put("victimIndicatorIds", dto.getVictimIndicatorIds());
        separateRuleConfig.put("suspectIndicatorIds", dto.getSuspectIndicatorIds());

        task.setRuleConfig(objectMapper.valueToTree(separateRuleConfig));

        AnalysisTask savedTask = taskRepository.save(task);
        logger.info("分离规则任务创建成功: {}", savedTask.getTaskCode());

        return savedTask;
    }

    /**
     * 启动任务执行
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    public AnalysisTask startTask(Long taskId) {
        logger.info("启动任务执行: {}", taskId);

        AnalysisTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        if (task.getTaskStatus() != 0) {
            throw new IllegalStateException("只有待执行状态的任务才能启动");
        }

        // 更新任务状态为执行中
        task.setTaskStatus((short) 1);
        task.setStartTime(LocalDateTime.now());
        task.setProgress(0);

        AnalysisTask updatedTask = taskRepository.save(task);

        // 异步执行任务
        executeTaskAsync(updatedTask);

        logger.info("任务启动成功: {}", task.getTaskCode());
        return updatedTask;
    }

    /**
     * 停止任务执行
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    public AnalysisTask stopTask(Long taskId) {
        logger.info("停止任务执行: {}", taskId);

        AnalysisTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        if (task.getTaskStatus() != 1) {
            throw new IllegalStateException("只有执行中的任务才能停止");
        }

        // 更新任务状态为失败（手动停止）
        task.setTaskStatus((short) 3);
        task.setEndTime(LocalDateTime.now());

        AnalysisTask updatedTask = taskRepository.save(task);
        logger.info("任务停止成功: {}", task.getTaskCode());

        return updatedTask;
    }

    /**
     * 删除任务
     * @param taskId 任务ID
     */
    @Transactional
    public void deleteTask(Long taskId) {
        logger.info("删除任务: {}", taskId);

        AnalysisTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        if (task.getTaskStatus() == 1) {
            throw new IllegalStateException("正在执行的任务不能删除，请先停止任务");
        }

        // 删除任务相关的风险结果
        riskResultRepository.deleteByTaskId(taskId);

        // 删除任务
        taskRepository.delete(task);
        logger.info("任务删除成功: {}", task.getTaskCode());
    }

    /**
     * 获取任务统计信息
     * @return 统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTaskStatistics() {
        logger.info("获取任务统计信息");

        long totalCount = taskRepository.count();
        long pendingCount = taskRepository.findPendingTasks().size();
        long runningCount = taskRepository.findRunningTasks().size();
        long todayCount = taskRepository.countTodayTasks(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));

        List<Object[]> statusCounts = taskRepository.countByTaskStatus();
        long completedCount = 0;
        long failedCount = 0;

        for (Object[] statusCount : statusCounts) {
            Short status = (Short) statusCount[0];
            Long count = (Long) statusCount[1];
            if (status == 2) completedCount = count;
            if (status == 3) failedCount = count;
        }

        return Map.of(
                "totalCount", totalCount,
                "pendingCount", pendingCount,
                "runningCount", runningCount,
                "completedCount", completedCount,
                "failedCount", failedCount,
                "todayCount", todayCount
        );
    }

    /**
     * 根据状态查找任务
     * @param status 任务状态
     * @return 任务列表
     */
    @Transactional(readOnly = true)
    public List<AnalysisTask> getTasksByStatus(Short status) {
        logger.info("根据状态查找任务: {}", status);
        return taskRepository.findByTaskStatus(status);
    }

    /**
     * 搜索任务
     * @param keyword 关键字
     * @return 匹配的任务列表
     */
    @Transactional(readOnly = true)
    public List<AnalysisTask> searchTasks(String keyword) {
        logger.info("搜索任务: {}", keyword);
        return taskRepository.findByTaskNameContaining(keyword);
    }

    /**
     * 异步执行任务
     * @param task 要执行的任务
     */
    private void executeTaskAsync(AnalysisTask task) {
        CompletableFuture.runAsync(() -> {
            try {
                executeTask(task);
            } catch (Exception e) {
                logger.error("任务执行失败: {}", task.getTaskCode(), e);

                // 更新任务状态为失败
                task.setTaskStatus((short) 3);
                task.setEndTime(LocalDateTime.now());
                taskRepository.save(task);
            }
        });
    }

    /**
     * 执行任务的具体逻辑
     * @param task 要执行的任务
     */
    private void executeTask(AnalysisTask task) {
        // 🔧 判断使用SQL引擎还是外部Spark API
        // 可以通过环境变量、配置文件或任务参数来决定
        boolean useExternalSpark = true; // 默认使用SQL引擎，可以改为true调用外部Spark API
        
        if (useExternalSpark) {
            logger.info("🚀 开始执行任务: {} - 调用外部Spark API", task.getTaskCode());
            executeSeparateRulesWithExternalSpark(task);
        } else {
            logger.info("🚀 开始执行任务: {} - 使用SQL引擎", task.getTaskCode());
            executeSeparateRulesTask(task);
        }
    }

    /**
     * 使用 Spark API 执行分析任务 - 支持分离规则
     */
    private void executeWithSparkAPI(AnalysisTask task, String ruleLogicJson) {
        logger.info("🔥 使用 Spark API 执行任务: {}", task.getTaskCode());
        
        try {
            // 解析分离规则
            var ruleNode = objectMapper.readTree(ruleLogicJson);
            String victimRule = ruleNode.has("victimRule") ? ruleNode.get("victimRule").asText() : "";
            String suspectRule = ruleNode.has("suspectRule") ? ruleNode.get("suspectRule").asText() : "";
            
            logger.info("📋 解析分离规则");
            logger.info("👥 受害人规则: {}", victimRule);
            logger.info("🕵️ 可疑分子规则: {}", suspectRule);
            
            // 智能解析规则表达式，提取指标
            if (!victimRule.isEmpty()) {
                Set<String> victimIndicators = extractIndicatorCodesFromRule(victimRule);
                logger.info("👥 受害人规则中提取到指标: {}", victimIndicators);
            }
            
            if (!suspectRule.isEmpty()) {
                Set<String> suspectIndicators = extractIndicatorCodesFromRule(suspectRule);
                logger.info("🕵️ 可疑分子规则中提取到指标: {}", suspectIndicators);
            }
            
            // 设置基础参数
            Map<String, Object> baseParams = new HashMap<>();
            baseParams.put("timeWindowDays", task.getIndicatorTimeWindow() != null ? task.getIndicatorTimeWindow() : 30);
            baseParams.put("largeAmountThreshold", 50000.0);
            baseParams.put("quickTransferMinutes", 2);
            baseParams.put("minTransferAmount", 50000.0);
            baseParams.put("quickOperationMinutes", 5);
            baseParams.put("minSingleAmount", 0.01);
            
            List<RiskResult> allResults = new ArrayList<>();
            
            // 执行受害人规则分析
            if (victimRule != null && !victimRule.trim().isEmpty()) {
                List<RiskResult> victimResults = executeVictimAnalysis(task, victimRule, baseParams);
                allResults.addAll(victimResults);
            }
            
            // 执行可疑分子规则分析
            if (suspectRule != null && !suspectRule.trim().isEmpty()) {
                List<RiskResult> suspectResults = executeSuspectAnalysis(task, suspectRule, baseParams);
                allResults.addAll(suspectResults);
            }
            
            // 更新任务状态为完成
            updateTaskProgress(task.getTaskId(), 100);
            
            // 手动更新任务状态
            Optional<AnalysisTask> taskOpt = taskRepository.findById(task.getTaskId());
            if (taskOpt.isPresent()) {
                AnalysisTask taskToUpdate = taskOpt.get();
                taskToUpdate.setTaskStatus((short) 2); // 2=已完成
                taskToUpdate.setEndTime(LocalDateTime.now());
                taskToUpdate.setResultCount(allResults.size());
                taskRepository.save(taskToUpdate);
                logger.info("📋 任务状态已更新为 COMPLETED，结果数量: {}", allResults.size());
            }
            
            logger.info("✅ 分离规则 Spark 分析完成，共保存 {} 条结果", allResults.size());
            
        } catch (Exception e) {
            logger.error("Spark API 执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("Spark API 执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行受害人规则分析
     * 支持复杂规则表达式，如: INDICATOR_A > 0 AND INDICATOR_B >= 5
     */
    private List<RiskResult> executeVictimAnalysis(AnalysisTask task, String victimRule, Map<String, Object> baseParams) {
        logger.info("👥 执行受害人规则分析: {}", victimRule);
        
        List<RiskResult> allResults = new ArrayList<>();
        
        // 从受害人规则中提取指标
        Set<String> victimIndicators = extractIndicatorCodesFromRule(victimRule);
        logger.info("👥 受害人规则解析结果: 提取到 {} 个指标 {}", victimIndicators.size(), victimIndicators);
        
        if (victimIndicators.isEmpty()) {
            logger.warn("⚠️ 受害人规则中没有找到有效的指标");
            return allResults;
        }
        
        // 分别执行每个指标的计算，收集所有结果
        Map<String, Map<String, Object>> indicatorResults = new HashMap<>();
        
        for (String indicatorCode : victimIndicators) {
            logger.info("📊 执行受害人指标: {}", indicatorCode);
            Map<String, Object> result = sparkIntegrationService.executeSparkAnalysis(indicatorCode, baseParams);
            
            if (result != null && (Boolean) result.getOrDefault("success", false)) {
                logger.info("✅ 受害人指标 {} 分析成功", indicatorCode);
                logger.info("🔍 Spark API 返回结果结构: {}", result.keySet());
                logger.info("🔍 结果详情: {}", result);
                indicatorResults.put(indicatorCode, result);
            } else {
                logger.warn("⚠️ 受害人指标 {} 分析失败", indicatorCode);
                if (result != null) {
                    logger.warn("⚠️ 失败结果: {}", result);
                }
            }
        }
        
        // 合并同一账户的多个指标结果，避免唯一约束冲突
        if (!indicatorResults.isEmpty()) {
            List<RiskResult> mergedResults = mergeIndicatorResultsForAccounts(task, indicatorResults, "VICTIM", victimRule);
            allResults.addAll(mergedResults);
        }
        
        // 解析并显示具体的规则条件
        logRuleConditions(victimRule, "受害人规则");
        
        // 记录规则执行摘要
        logger.info("👥 受害人规则执行完成: 规则='{}', 指标数={}, 结果数={}", 
            victimRule, victimIndicators.size(), allResults.size());
        
        return allResults;
    }
    
    /**
     * 执行可疑分子规则分析
     * 支持复杂规则表达式，如: INDICATOR_C = 0 OR INDICATOR_D > 10
     */
    private List<RiskResult> executeSuspectAnalysis(AnalysisTask task, String suspectRule, Map<String, Object> baseParams) {
        logger.info("🕵️ 执行可疑分子规则分析: {}", suspectRule);
        
        List<RiskResult> allResults = new ArrayList<>();
        
        // 从可疑分子规则中提取指标
        Set<String> suspectIndicators = extractIndicatorCodesFromRule(suspectRule);
        logger.info("🕵️ 可疑分子规则解析结果: 提取到 {} 个指标 {}", suspectIndicators.size(), suspectIndicators);
        
        if (suspectIndicators.isEmpty()) {
            logger.warn("⚠️ 可疑分子规则中没有找到有效的指标");
            return allResults;
        }
        
        // 分别执行每个指标的计算，收集所有结果
        Map<String, Map<String, Object>> indicatorResults = new HashMap<>();
        
        for (String indicatorCode : suspectIndicators) {
            logger.info("📊 执行可疑分子指标: {}", indicatorCode);
            Map<String, Object> result = sparkIntegrationService.executeSparkAnalysis(indicatorCode, baseParams);
            
            if (result != null && (Boolean) result.getOrDefault("success", false)) {
                logger.info("✅ 可疑分子指标 {} 分析成功", indicatorCode);
                logger.info("🔍 Spark API 返回结果结构: {}", result.keySet());
                logger.info("🔍 结果详情: {}", result);
                indicatorResults.put(indicatorCode, result);
            } else {
                logger.warn("⚠️ 可疑分子指标 {} 分析失败", indicatorCode);
                if (result != null) {
                    logger.warn("⚠️ 失败结果: {}", result);
                }
            }
        }
        
        // 合并同一账户的多个指标结果，避免唯一约束冲突
        if (!indicatorResults.isEmpty()) {
            List<RiskResult> mergedResults = mergeIndicatorResultsForAccounts(task, indicatorResults, "SUSPECT", suspectRule);
            allResults.addAll(mergedResults);
        }
        
        // 解析并显示具体的规则条件
        logRuleConditions(suspectRule, "可疑分子规则");
        
        // 记录规则执行摘要
        logger.info("🕵️ 可疑分子规则执行完成: 规则='{}', 指标数={}, 结果数={}", 
            suspectRule, suspectIndicators.size(), allResults.size());
        
        return allResults;
    }
    
    
    /**
     * 合并同一账户的多个指标结果，避免唯一约束冲突
     */
    private List<RiskResult> mergeIndicatorResultsForAccounts(AnalysisTask task, 
                                                            Map<String, Map<String, Object>> indicatorResults, 
                                                            String riskType, 
                                                            String ruleExpression) {
        List<RiskResult> mergedResults = new ArrayList<>();
        
        // 按账户ID分组所有指标的结果
        Map<Long, Map<String, Object>> accountResultsMap = new HashMap<>();
        
        // 遍历每个指标的结果
        for (Map.Entry<String, Map<String, Object>> indicatorEntry : indicatorResults.entrySet()) {
            String indicatorCode = indicatorEntry.getKey();
            Map<String, Object> indicatorResult = indicatorEntry.getValue();
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) indicatorResult.get("results");
            
            if (results != null) {
                logger.info("🔍 处理指标 {} 的 {} 条结果", indicatorCode, results.size());
                for (Map<String, Object> result : results) {
                    logger.info("🔍 Spark返回的单条结果: {}", result);
                    Object accountIdObj = result.get("account_id");
                    if (accountIdObj != null) {
                        Long accountId = Long.valueOf(accountIdObj.toString());
                        
                        // 获取或创建该账户的合并结果
                        Map<String, Object> accountResult = accountResultsMap.computeIfAbsent(accountId, k -> {
                            Map<String, Object> newResult = new HashMap<>();
                            newResult.put("account_id", accountId);
                            newResult.put("indicators", new HashMap<String, Object>());
                            return newResult;
                        });
                        
                        // 添加当前指标的值
                        @SuppressWarnings("unchecked")
                        Map<String, Object> indicators = (Map<String, Object>) accountResult.get("indicators");
                        
                        // Spark返回的字段名格式: INDICATOR_A → indicator_A, INDICATOR_B → indicator_B, INDICATOR_C → indicator_C
                        Object indicatorValue = null;
                        String[] possibleKeys = {
                            "indicator_" + indicatorCode.substring(indicatorCode.lastIndexOf("_") + 1), // indicator_A, indicator_B, indicator_C
                            indicatorCode.toLowerCase(),  // indicator_a, indicator_b, indicator_c
                            indicatorCode,                // INDICATOR_A, INDICATOR_B, INDICATOR_C
                            "indicator_value",
                            "value"
                        };
                        
                        for (String key : possibleKeys) {
                            indicatorValue = result.get(key);
                            if (indicatorValue != null) {
                                logger.info("✅ 找到指标值: {} = {} (使用键: {})", indicatorCode, indicatorValue, key);
                                break;
                            }
                        }
                        
                        if (indicatorValue != null) {
                            // 确保数值类型正确
                            if (indicatorValue instanceof Number) {
                                indicators.put(indicatorCode, ((Number) indicatorValue).doubleValue());
                            } else {
                                try {
                                    double numValue = Double.parseDouble(indicatorValue.toString());
                                    indicators.put(indicatorCode, numValue);
                                } catch (NumberFormatException e) {
                                    indicators.put(indicatorCode, indicatorValue);
                                }
                            }
                        } else {
                            logger.warn("⚠️ 未找到指标 {} 的值，可用键: {}", indicatorCode, result.keySet());
                            logger.warn("⚠️ 结果内容: {}", result);
                            // 设置默认值，避免空值
                            indicators.put(indicatorCode, 0.0);
                        }
                    }
                }
            }
        }
        
        // 为每个账户创建一条合并的风险结果记录
        for (Map.Entry<Long, Map<String, Object>> accountEntry : accountResultsMap.entrySet()) {
            Long accountId = accountEntry.getKey();
            Map<String, Object> accountResult = accountEntry.getValue();
            
            try {
                RiskResult riskResult = new RiskResult();
                riskResult.setTaskId(task.getTaskId());
                riskResult.setAccountId(accountId);
                
                // 设置账户角色：1=受害人，2=可疑账户
                Short accountRole = "VICTIM".equals(riskType) ? (short) 1 : (short) 2;
                riskResult.setAccountRole(accountRole);
                
                // 构建合并的指标结果JSON
                @SuppressWarnings("unchecked")
                Map<String, Object> indicators = (Map<String, Object>) accountResult.get("indicators");
                
                // 解析规则条件，生成详细的指标值描述
                List<String> indicatorDescriptions = new ArrayList<>();
                Map<String, Object> detailedIndicators = new HashMap<>();
                
                // 解析规则表达式中的条件
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(INDICATOR_[A-Z_]+)\\s*(>=|<=|>|<|=|!=)\\s*(\\d+(?:\\.\\d+)?)"
                );
                java.util.regex.Matcher matcher = pattern.matcher(ruleExpression);
                
                while (matcher.find()) {
                    String indicatorCode = matcher.group(1);
                    String operator = matcher.group(2);
                    String threshold = matcher.group(3);
                    
                    Object actualValue = indicators.get(indicatorCode);
                    // 只要指标存在就显示规则，不管值是什么
                    if (actualValue != null) {
                        // 根据风险类型格式化显示
                        String description;
                        if ("VICTIM".equals(riskType)) {
                            description = String.format("受害人规则: %s %s %s", indicatorCode, operator, threshold);
                        } else {
                            description = String.format("可疑分子规则: %s %s %s", indicatorCode, operator, threshold);
                        }
                        indicatorDescriptions.add(description);
                        
                        logger.info("📝 生成规则描述: {}", description);
                        
                        // 保存详细信息
                        Map<String, Object> indicatorDetail = new HashMap<>();
                        indicatorDetail.put("actual_value", actualValue);
                        indicatorDetail.put("rule_operator", operator);
                        indicatorDetail.put("rule_threshold", threshold);
                        indicatorDetail.put("description", description);
                        detailedIndicators.put(indicatorCode, indicatorDetail);
                    }
                }
                
                // 直接保存指标值，格式类似: {"INDICATOR_A": 5.0, "INDICATOR_C": 460000.0}
                JsonNode matchedIndicators = objectMapper.valueToTree(indicators);
                riskResult.setMatchedIndicators(matchedIndicators);
                
                // 设置匹配规则信息 - 按照原来的格式，只显示完整的规则表达式
                String ruleInfo;
                if ("VICTIM".equals(riskType)) {
                    ruleInfo = "受害人规则: " + ruleExpression;
                } else {
                    ruleInfo = "可疑分子规则: " + ruleExpression;
                }
                riskResult.setMatchedRules(ruleInfo);
                
                // 设置创建时间
                riskResult.setCreateTime(LocalDateTime.now());
                riskResult.setUpdateTime(LocalDateTime.now());
                
                // 保存到数据库
                RiskResult saved = riskResultRepository.save(riskResult);
                mergedResults.add(saved);
                
                logger.info("💾 合并保存账户 {} 的 {} 风险结果，包含 {} 个指标", 
                    accountId, riskType, indicators.size());
                logger.info("📋 保存的规则信息: {}", ruleInfo);
                logger.info("📊 保存的指标数据: {}", indicators);
                
            } catch (Exception e) {
                logger.error("保存账户 {} 的合并结果失败: {}", accountId, e.getMessage(), e);
            }
        }
        
        logger.info("✅ 成功合并保存 {} 条 {} 风险结果", mergedResults.size(), riskType);
        return mergedResults;
    }
    
    /**
     * 计算风险等级
     */
    private String calculateRiskLevel(double riskScore) {
        if (riskScore >= 10) return "HIGH";
        if (riskScore >= 5) return "MEDIUM";
        if (riskScore >= 1) return "LOW";
        return "MINIMAL";
    }
    
    /**
     * 解析并记录规则条件
     */
    private void logRuleConditions(String ruleExpression, String ruleType) {
        if (ruleExpression == null || ruleExpression.trim().isEmpty()) {
            return;
        }
        
        try {
            // 使用正则表达式提取所有条件
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(INDICATOR_[A-Z_]+)\\s*(>=|<=|>|<|=|!=)\\s*(\\d+(?:\\.\\d+)?)"
            );
            java.util.regex.Matcher matcher = pattern.matcher(ruleExpression);
            
            while (matcher.find()) {
                String indicatorCode = matcher.group(1);
                String operator = matcher.group(2);
                String value = matcher.group(3);
                
                // 根据规则类型显示不同的日志
                if ("受害人规则".equals(ruleType)) {
                    logger.info("受害人规则: {} {} {}", indicatorCode, operator, value);
                } else if ("可疑分子规则".equals(ruleType)) {
                    logger.info("可疑分子规则: {} {} {}", indicatorCode, operator, value);
                }
            }
            
        } catch (Exception e) {
            logger.warn("解析规则条件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从规则中智能提取指标类型
     */
    private String extractIndicatorFromRule(String ruleLogicJson) {
        try {
            // 解析规则内容，根据关键词判断指标类型
            String ruleText = ruleLogicJson.toLowerCase();
            
            if (ruleText.contains("快进快出") || ruleText.contains("大额") || ruleText.contains("转入") && ruleText.contains("转出")) {
                logger.info("🎯 检测到大额快进快出规则，使用 INDICATOR_A");
                return "INDICATOR_A";
            } else if (ruleText.contains("登录") || ruleText.contains("login")) {
                logger.info("🎯 检测到登录相关规则，使用 INDICATOR_B");
                return "INDICATOR_B";
            } else if (ruleText.contains("累计") || ruleText.contains("总额") || ruleText.contains("入账")) {
                logger.info("🎯 检测到累计金额规则，使用 INDICATOR_C");
                return "INDICATOR_C";
            } else {
                logger.info("🎯 未识别规则类型，默认使用 INDICATOR_A");
                return "INDICATOR_A";
            }
        } catch (Exception e) {
            logger.warn("规则解析失败，使用默认指标 INDICATOR_A: {}", e.getMessage());
            return "INDICATOR_A";
        }
    }

    /**
     * 解析并执行分离规则
     */
    private void parseAndExecuteSeparateRules(AnalysisTask task, String ruleLogicJson) {
        try {
            // 解析JSON规则
            var ruleNode = objectMapper.readTree(ruleLogicJson);
            String victimRule = ruleNode.has("victimRule") ? ruleNode.get("victimRule").asText() : "";
            String suspectRule = ruleNode.has("suspectRule") ? ruleNode.get("suspectRule").asText() : "";

            logger.info("解析分离规则 - 受害人: {}, 可疑分子: {}", victimRule, suspectRule);

            // 临时更新任务的规则配置以便后续处理
            Map<String, Object> tempRuleConfig = new HashMap<>();
            tempRuleConfig.put("type", "separate_rules");
            tempRuleConfig.put("victimRule", victimRule);
            tempRuleConfig.put("suspectRule", suspectRule);

            task.setRuleConfig(objectMapper.valueToTree(tempRuleConfig));

            // 执行分离规则任务
            executeSeparateRulesTask(task);

        } catch (Exception e) {
            logger.error("解析分离规则JSON失败: {}", e.getMessage(), e);
            throw new RuntimeException("分离规则格式错误: " + e.getMessage(), e);
        }
    }


    /**
     * 调用外部Spark API执行分离规则任务
     */
    private void executeSeparateRulesWithExternalSpark(AnalysisTask task) {
        logger.info("🔥 调用外部Spark API执行分离规则任务: {}", task.getTaskCode());
        
        try {
            // 解析规则配置
            String victimRule = task.getRuleConfig().has("victimRule") ?
                    task.getRuleConfig().get("victimRule").asText() : "";
            String suspectRule = task.getRuleConfig().has("suspectRule") ?
                    task.getRuleConfig().get("suspectRule").asText() : "";
            
            logger.info("📋 准备调用外部Spark API");
            logger.info("👥 受害人规则: {}", victimRule);
            logger.info("🕵️ 可疑分子规则: {}", suspectRule);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("victim_rule", victimRule);
            requestBody.put("suspect_rule", suspectRule);
            requestBody.put("time_window_days", task.getIndicatorTimeWindow());
            requestBody.put("large_amount_threshold", 50000.0);
            requestBody.put("quick_transfer_minutes", 2);
            requestBody.put("quick_operation_minutes", 5);
            requestBody.put("min_single_amount", 0.01);
            
            // 调用外部Spark API
            String sparkApiUrl = "http://localhost:8083/api/spark/separate-rules-analysis";
            logger.info("🌐 调用外部Spark API: {}", sparkApiUrl);
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            org.springframework.http.HttpEntity<Map<String, Object>> request = 
                new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                sparkApiUrl,
                request,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                logger.info("✅ 外部Spark API调用成功");
                logger.info("📊 返回结果: {}", responseBody);
                
                // 解析Spark返回的结果并保存到数据库
                int savedCount = processSparkResults(task, responseBody, victimRule, suspectRule);
                
                // 更新任务状态
                task.setTaskStatus((short) 2);
                task.setEndTime(LocalDateTime.now());
                task.setProgress(100);
                task.setResultCount(savedCount);  // 🔧 设置结果数量
                task.setAnalyzedAccounts(savedCount);  // 设置分析账户数
                taskRepository.save(task);
                
                logger.info("✅ 外部Spark分析任务完成: {}", task.getTaskCode());
            } else {
                logger.error("❌ 外部Spark API返回错误状态: {}", response.getStatusCode());
                throw new RuntimeException("Spark API返回错误: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("❌ 调用外部Spark API失败: {}", e.getMessage(), e);
            logger.info("🔄 回退到SQL引擎执行");
            executeSeparateRulesTask(task);
        }
    }
    
    /**
     * 处理Spark API返回的结果
     * @return 保存的记录数
     */
    private int processSparkResults(AnalysisTask task, Map<String, Object> responseBody, 
                                     String victimRule, String suspectRule) {
        int savedCount = 0;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> statistics = (Map<String, Object>) responseBody.get("statistics");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
            
            if (statistics != null) {
                int victimCount = ((Number) statistics.getOrDefault("victim_count", 0)).intValue();
                int suspectCount = ((Number) statistics.getOrDefault("suspect_count", 0)).intValue();
                logger.info("📊 Spark分析结果: 受害人={}, 可疑分子={}", victimCount, suspectCount);
            }
            
            if (results != null && !results.isEmpty()) {
                logger.info("💾 开始保存Spark分析结果到数据库...");
                
                for (Map<String, Object> result : results) {
                    try {
                        RiskResult riskResult = new RiskResult();
                        riskResult.setTaskId(task.getTaskId());
                        
                        // 账户ID - 兼容字符串和数字类型
                        Object accountIdObj = result.get("account_id");
                        if (accountIdObj != null) {
                            Long accountId = null;
                            if (accountIdObj instanceof Number) {
                                accountId = ((Number) accountIdObj).longValue();
                            } else if (accountIdObj instanceof String) {
                                try {
                                    accountId = Long.parseLong((String) accountIdObj);
                                } catch (NumberFormatException e) {
                                    logger.warn("无法解析账户ID为数字: {}", accountIdObj);
                                    continue; // 跳过这条记录
                                }
                            } else {
                                logger.warn("账户ID类型不支持: {} (类型: {})", accountIdObj, accountIdObj.getClass().getName());
                                continue; // 跳过这条记录
                            }
                            riskResult.setAccountId(accountId);
                        } else {
                            logger.warn("账户ID为空，跳过该记录");
                            continue; // 跳过这条记录
                        }
                        
                        // 账户角色
                        String accountRole = (String) result.get("account_role");
                        if ("VICTIM".equals(accountRole)) {
                            riskResult.setAccountRole((short) 1);
                        } else if ("SUSPECT".equals(accountRole)) {
                            riskResult.setAccountRole((short) 2);
                        }
                        
                        // 指标值 - 动态提取所有指标
                        Map<String, Object> indicators = new HashMap<>();
                        for (String key : result.keySet()) {
                            // 跳过 account_id 和 account_role 字段
                            if ("account_id".equalsIgnoreCase(key) || "account_role".equalsIgnoreCase(key)) {
                                continue;
                            }
                            
                            // 提取所有 indicator_ 开头的字段（不区分大小写）
                            if (key.toLowerCase().startsWith("indicator_")) {
                                // 转换为大写的标准格式 INDICATOR_X
                                String indicatorCode = key.toUpperCase();
                                Object indicatorValue = result.get(key);
                                
                                // 确保指标值是数字类型（如果是字符串，尝试转换）
                                if (indicatorValue instanceof String) {
                                    try {
                                        // 尝试解析为数字
                                        if (((String) indicatorValue).contains(".")) {
                                            indicatorValue = Double.parseDouble((String) indicatorValue);
                                        } else {
                                            indicatorValue = Long.parseLong((String) indicatorValue);
                                        }
                                    } catch (NumberFormatException e) {
                                        logger.warn("无法解析指标值 {} 为数字: {}", indicatorCode, indicatorValue);
                                        // 保持原值
                                    }
                                }
                                
                                indicators.put(indicatorCode, indicatorValue);
                            }
                        }
                        
                        riskResult.setMatchedIndicators(objectMapper.valueToTree(indicators));
                        
                        // 🔧 匹配规则 - 使用完整的规则表达式
                        String matchedRule = "";
                        if ("VICTIM".equals(accountRole)) {
                            matchedRule = victimRule != null ? victimRule : "受害人规则";
                        } else if ("SUSPECT".equals(accountRole)) {
                            matchedRule = suspectRule != null ? suspectRule : "可疑分子规则";
                        }
                        riskResult.setMatchedRules(matchedRule);
                        
                        // 时间
                        riskResult.setCreateTime(LocalDateTime.now());
                        riskResult.setUpdateTime(LocalDateTime.now());
                        
                        riskResultRepository.save(riskResult);
                        savedCount++;  // 计数成功保存的记录
                        
                    } catch (Exception e) {
                        logger.error("保存单条Spark结果失败: {}", e.getMessage(), e);
                    }
                }
                
                logger.info("✅ Spark结果保存完成，共 {} 条", savedCount);
            }
            
        } catch (Exception e) {
            logger.error("❌ 处理Spark结果失败: {}", e.getMessage(), e);
        }
        
        return savedCount;
    }

    /**
     * 执行分离规则任务（使用SQL引擎）
     */
    private void executeSeparateRulesTask(AnalysisTask task) {
        logger.info("💾 使用SQL引擎执行分离规则任务: {}", task.getTaskCode());
        executeSeparateRulesTaskWithEngine(task, "SQL");
    }

    /**
     * 执行分离规则任务（通用方法，支持指定引擎）
     */
    private void executeSeparateRulesTaskWithEngine(AnalysisTask task, String selectedEngine) {
        logger.info("执行分离规则任务: {} (引擎: {})", task.getTaskCode(), selectedEngine);

        // 1. 解析分离规则配置
        updateTaskProgress(task.getTaskId(), 5);
        String victimRule = task.getRuleConfig().has("victimRule") ?
                task.getRuleConfig().get("victimRule").asText() : null;
        String suspectRule = task.getRuleConfig().has("suspectRule") ?
                task.getRuleConfig().get("suspectRule").asText() : null;

        logger.info("分离规则配置 - 受害人规则: {}, 可疑分子规则: {}", victimRule, suspectRule);

        // 2. 从规则表达式中动态提取所需的指标
        updateTaskProgress(task.getTaskId(), 10);

        List<AnalysisIndicator> allIndicators = getIndicatorsFromTask(task);
        Map<String, AnalysisIndicator> indicatorMap = allIndicators.stream()
                .collect(java.util.stream.Collectors.toMap(
                        AnalysisIndicator::getIndicatorCode,
                        indicator -> indicator
                ));

        // 提取受害人规则中使用的指标
        List<AnalysisIndicator> victimIndicators = new ArrayList<>();
        if (victimRule != null && !victimRule.trim().isEmpty()) {
            Set<String> victimIndicatorCodes = extractIndicatorCodesFromRule(victimRule);
            for (String code : victimIndicatorCodes) {
                if (indicatorMap.containsKey(code)) {
                    victimIndicators.add(indicatorMap.get(code));
                }
            }
        }

        // 提取可疑分子规则中使用的指标
        List<AnalysisIndicator> suspectIndicators = new ArrayList<>();
        if (suspectRule != null && !suspectRule.trim().isEmpty()) {
            Set<String> suspectIndicatorCodes = extractIndicatorCodesFromRule(suspectRule);
            for (String code : suspectIndicatorCodes) {
                if (indicatorMap.containsKey(code)) {
                    suspectIndicators.add(indicatorMap.get(code));
                }
            }
        }

        logger.info("动态提取指标完成 - 受害人规则使用: {}, 可疑分子规则使用: {}",
                victimIndicators.stream().map(AnalysisIndicator::getIndicatorCode).toList(),
                suspectIndicators.stream().map(AnalysisIndicator::getIndicatorCode).toList());

        // 3. 使用指定的引擎
        updateTaskProgress(task.getTaskId(), 20);
        logger.info("🎯 使用分析引擎: {}", selectedEngine);

        // 4. 执行分离规则（使用选定的引擎）
        updateTaskProgress(task.getTaskId(), 30);
        var combinedResult = executeWithSelectedEngine(
                selectedEngine, victimRule, suspectRule,
                victimIndicators, suspectIndicators,
                task.getTaskTimeRange(), task.getIndicatorTimeWindow());

        updateTaskProgress(task.getTaskId(), 80);
        logger.info("分离规则执行完成 - 受害人: {}, 可疑分子: {}",
                combinedResult.getVictimCount(), combinedResult.getSuspectCount());

        // 5. 生成风险结果
        updateTaskProgress(task.getTaskId(), 90);
        generateSeparateRiskResults(task, combinedResult, victimIndicators, suspectIndicators);

        // 6. 任务完成
        task.setTaskStatus((short) 2);
        task.setEndTime(LocalDateTime.now());
        task.setProgress(100);
        task.setAnalyzedAccounts(combinedResult.getTotalCount());
        task.setResultCount((int) riskResultRepository.countByTaskId(task.getTaskId()));
        taskRepository.save(task);

        logger.info("✅ 分离规则任务执行完成: {}，发现 {} 个受害人，{} 个可疑分子 (引擎: {})",
                task.getTaskCode(), combinedResult.getVictimCount(), combinedResult.getSuspectCount(), selectedEngine);
    }

    /**
     * 使用选定的引擎执行分析（仅支持SQL引擎）
     */
    private SqlBasedRuleEngine.CombinedRuleResult executeWithSelectedEngine(
            String engine,
            String victimRule,
            String suspectRule,
            List<AnalysisIndicator> victimIndicators,
            List<AnalysisIndicator> suspectIndicators,
            Integer taskTimeRange,
            Integer indicatorTimeWindow) {

        // 只支持SQL引擎，外部Spark通过API调用
        if (!"SQL".equals(engine)) {
            logger.warn("⚠️ 不支持的引擎类型: {}，回退到SQL引擎", engine);
        }
        
        logger.info("💾 使用SQL引擎执行分析...");
        return sqlRuleEngine.executeSeparateRules(
                victimRule, suspectRule, victimIndicators, suspectIndicators,
                taskTimeRange, indicatorTimeWindow);
    }

    /**
     * 从任务中获取所有指标
     */
    private List<AnalysisIndicator> getIndicatorsFromTask(AnalysisTask task) {
        String[] indicatorIdStrs = task.getIndicatorIds().split(",");
        List<Long> indicatorIds = new ArrayList<>();
        for (String idStr : indicatorIdStrs) {
            try {
                indicatorIds.add(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException e) {
                logger.warn("无效的指标ID: {}", idStr);
            }
        }
        return indicatorRepository.findAllById(indicatorIds);
    }

    /**
     * 从规则表达式中提取指标代码
     * 支持格式：INDICATOR_A > 0 AND INDICATOR_B >= 5 OR INDICATOR_C = 0
     */
    private Set<String> extractIndicatorCodesFromRule(String ruleExpression) {
        Set<String> indicatorCodes = new HashSet<>();
        if (ruleExpression == null || ruleExpression.trim().isEmpty()) {
            return indicatorCodes;
        }

        // 使用正则表达式匹配所有的指标代码
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("INDICATOR_[A-Z0-9_]+");
        java.util.regex.Matcher matcher = pattern.matcher(ruleExpression);

        while (matcher.find()) {
            String indicatorCode = matcher.group();
            indicatorCodes.add(indicatorCode);
            logger.debug("从规则中提取到指标代码: {}", indicatorCode);
        }

        logger.info("从规则 '{}' 中提取到 {} 个指标: {}", ruleExpression, indicatorCodes.size(), indicatorCodes);
        return indicatorCodes;
    }

    /**
     * 更新任务进度
     * @param taskId 任务ID
     * @param progress 进度
     */
    private void updateTaskProgress(Long taskId, Integer progress) {
        Optional<AnalysisTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            AnalysisTask task = taskOpt.get();
            task.setProgress(progress);
            taskRepository.save(task);
            logger.info("任务进度更新: {} - {}%", task.getTaskCode(), progress);
        }
    }

    /**
     * 生成分离规则风险结果
     * @param task 任务
     * @param combinedResult 合并的规则结果
     * @param victimIndicators 受害人指标
     * @param suspectIndicators 可疑分子指标
     */
    private void generateSeparateRiskResults(AnalysisTask task,
                                             SqlBasedRuleEngine.CombinedRuleResult combinedResult,
                                             List<AnalysisIndicator> victimIndicators,
                                             List<AnalysisIndicator> suspectIndicators) {
        logger.info("生成分离规则任务结果: {}", task.getTaskCode());

        try {
            // 1. 处理受害人结果
            if (combinedResult.getVictimResult() != null) {
                var victimMatchedAccounts = combinedResult.getVictimResult().getMatchedAccounts();
                String victimRule = task.getRuleConfig().get("victimRule").asText();

                for (Map.Entry<Long, Map<String, Double>> entry : victimMatchedAccounts.entrySet()) {
                    Long accountId = entry.getKey();
                    Map<String, Double> accountIndicators = entry.getValue();

                    RiskResult riskResult = new RiskResult();
                    riskResult.setTaskId(task.getTaskId());
                    riskResult.setAccountId(accountId);
                    riskResult.setAccountRole((short) 1); // 1=受害人/付款方
                    riskResult.setMatchedIndicators(objectMapper.valueToTree(accountIndicators));
                    riskResult.setMatchedRules(victimRule);

                    riskResultRepository.save(riskResult);
                }

                logger.info("受害人风险结果生成完成，共 {} 个", victimMatchedAccounts.size());
            }

            // 2. 处理可疑分子结果
            if (combinedResult.getSuspectResult() != null) {
                var suspectMatchedAccounts = combinedResult.getSuspectResult().getMatchedAccounts();
                String suspectRule = task.getRuleConfig().get("suspectRule").asText();

                for (Map.Entry<Long, Map<String, Double>> entry : suspectMatchedAccounts.entrySet()) {
                    Long accountId = entry.getKey();
                    Map<String, Double> accountIndicators = entry.getValue();

                    RiskResult riskResult = new RiskResult();
                    riskResult.setTaskId(task.getTaskId());
                    riskResult.setAccountId(accountId);
                    riskResult.setAccountRole((short) 2); // 2=可疑分子/收款方
                    riskResult.setMatchedIndicators(objectMapper.valueToTree(accountIndicators));
                    riskResult.setMatchedRules(suspectRule);

                    riskResultRepository.save(riskResult);
                }

                logger.info("可疑分子风险结果生成完成，共 {} 个", suspectMatchedAccounts.size());
            }

            logger.info("分离规则风险结果生成完成，受害人: {}, 可疑分子: {}",
                    combinedResult.getVictimCount(), combinedResult.getSuspectCount());

        } catch (Exception e) {
            logger.error("生成分离规则风险结果失败", e);
            throw new RuntimeException("生成分析结果失败", e);
        }
    }




    /**
     * 获取任务的基本风险结果
     * @param taskId 任务ID
     * @return 风险结果列表
     */
    public List<RiskResult> getTaskResults(Long taskId) {
        logger.info("获取任务 {} 的基本风险结果", taskId);
        return riskResultRepository.findByTaskId(taskId);
    }

    /**
     * 获取任务的详细结果（分页版本）
     * @param taskId 任务ID
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     * @param accountRole 账户角色过滤（1=受害人，2=可疑账户，null=所有）
     * @return 详细结果DTO
     */
    public TaskResultDTO getTaskDetailedResultsPaged(Long taskId, int page, int pageSize, Short accountRole) {
        logger.info("获取任务 {} 的详细结果 (分页: {}/{}, 角色: {})", taskId, page, pageSize, accountRole);

        // 获取任务信息
        Optional<AnalysisTask> taskOpt = taskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        AnalysisTask task = taskOpt.get();

        // 计算偏移量
        int offset = (page - 1) * pageSize;

        // 获取总数（用于统计，根据角色过滤）
        long totalCount;
        if (accountRole != null) {
            totalCount = riskResultRepository.countByTaskIdAndAccountRole(taskId, accountRole);
        } else {
            totalCount = riskResultRepository.countByTaskId(taskId);
        }

        // 分页获取风险结果（包含账户信息，根据角色过滤）
        List<Object[]> resultWithAccounts = riskResultRepository.findByTaskIdWithAccountInfoPaged(taskId, offset, pageSize, accountRole);
        List<RiskResult> pagedResults = new ArrayList<>();

        for (Object[] row : resultWithAccounts) {
            // Native query返回的是原始列值，需要手动映射
            // row[0-6]: risk_result的字段, row[7-13]: account的字段
            RiskResult riskResult = new RiskResult();
            riskResult.setRiskId(((Number) row[0]).longValue());
            riskResult.setTaskId(((Number) row[1]).longValue());
            riskResult.setAccountId(((Number) row[2]).longValue());
            riskResult.setAccountRole(((Number) row[3]).shortValue());

            // matchedIndicators 是 JSONB，需要转换为 JsonNode
            if (row[4] != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    riskResult.setMatchedIndicators(mapper.readTree(row[4].toString()));
                } catch (Exception e) {
                    logger.warn("解析 matchedIndicators 失败: {}", e.getMessage());
                }
            }

            riskResult.setMatchedRules((String) row[5]);

            // 处理时间类型转换：Timestamp -> LocalDateTime
            if (row[6] != null) {
                if (row[6] instanceof java.sql.Timestamp) {
                    riskResult.setCreateTime(((java.sql.Timestamp) row[6]).toLocalDateTime());
                } else if (row[6] instanceof java.time.LocalDateTime) {
                    riskResult.setCreateTime((java.time.LocalDateTime) row[6]);
                }
            }

            // 如果有账户信息，也手动映射
            if (row[7] != null) {
                com.example.banks.entity.Account account = new com.example.banks.entity.Account();
                account.setId(((Number) row[7]).longValue());
                account.setAccountNo((String) row[8]);
                account.setHolderName((String) row[9]);
                account.setIdCardNo((String) row[10]);
                account.setPhone((String) row[11]);
                account.setAccountType(row[12] != null ? ((Number) row[12]).shortValue() : null);
                account.setStatus(row[13] != null ? ((Number) row[13]).shortValue() : null);

                riskResult.setAccount(account);
            }

            pagedResults.add(riskResult);
        }

        // 构建风险统计（基于总数，不是分页数据）
        TaskResultDTO.RiskStatistics riskStatistics = buildRiskStatisticsFromCount(taskId);

        // 构建风险账户详情（只包含当前页）
        List<TaskResultDTO.RiskAccountDTO> riskAccountDetails = buildRiskAccountDetails(pagedResults);

        // 构建任务结果DTO
        TaskResultDTO result = new TaskResultDTO();
        result.setTaskId(taskId);
        result.setTaskName(task.getTaskName());
        result.setTaskCode(task.getTaskCode());
        result.setTaskStatus(task.getTaskStatus());
        result.setTimeRange(task.getTimeRange());
        result.setStartTime(task.getStartTime());
        result.setEndTime(task.getEndTime());
        result.setAnalyzedAccounts((int) totalCount);
        result.setResultCount((int) totalCount);
        result.setStatistics(riskStatistics);
        result.setRiskAccounts(riskAccountDetails);

        // 添加分页信息
        result.setCurrentPage(page);
        result.setPageSize(pageSize);
        result.setTotalPages((int) Math.ceil((double) totalCount / pageSize));

        return result;
    }

    /**
     * 获取任务的详细结果（包含统计和账户信息）
     * @deprecated 使用 getTaskDetailedResultsPaged 替代
     * @param taskId 任务ID
     * @return 详细结果DTO
     */
    @Deprecated
    public TaskResultDTO getTaskDetailedResults(Long taskId) {
        logger.warn("调用了已弃用的方法 getTaskDetailedResults，建议使用分页版本");
        return getTaskDetailedResultsPaged(taskId, 1, 100, null);
    }

    /**
     * 构建风险统计信息（从数据库直接统计，性能更好）
     */
    private TaskResultDTO.RiskStatistics buildRiskStatisticsFromCount(Long taskId) {
        TaskResultDTO.RiskStatistics statistics = new TaskResultDTO.RiskStatistics();

        // 获取风险结果统计
        long totalRiskCount = riskResultRepository.countByTaskId(taskId);
        long victimCount = riskResultRepository.countByTaskIdAndAccountRole(taskId, (short) 1);
        long suspiciousCount = riskResultRepository.countByTaskIdAndAccountRole(taskId, (short) 2);
        
        // 获取account表的总账户数
        long totalAccountsCount = accountRepository.countAllAccounts();

        statistics.setTotalRiskAccounts((int) totalRiskCount);
        statistics.setVictimAccounts((int) victimCount);
        statistics.setSuspiciousAccounts((int) suspiciousCount);
        statistics.setTotalAccounts(totalAccountsCount);

        logger.info("风险统计 - 总账户数: {}, 风险账户数: {}, 风险占比: {:.2f}%", 
                   totalAccountsCount, totalRiskCount, statistics.getRiskRatio());

        return statistics;
    }

    /**
     * 构建风险统计信息
     */
    private TaskResultDTO.RiskStatistics buildRiskStatistics(List<RiskResult> results) {
        TaskResultDTO.RiskStatistics statistics = new TaskResultDTO.RiskStatistics();

        int totalCount = results.size();
        int victimCount = 0;
        int suspiciousCount = 0;

        for (RiskResult result : results) {
            if (result.getAccountRole() == 1) { // 1=受害人
                victimCount++;
            } else if (result.getAccountRole() == 2) { // 2=可疑账户
                suspiciousCount++;
            }
        }

        statistics.setTotalRiskAccounts(totalCount);
        statistics.setVictimAccounts(victimCount);
        statistics.setSuspiciousAccounts(suspiciousCount);

        return statistics;
    }

    /**
     * 构建风险账户详情
     */
    private List<TaskResultDTO.RiskAccountDTO> buildRiskAccountDetails(List<RiskResult> results) {
        List<TaskResultDTO.RiskAccountDTO> details = new ArrayList<>();

        for (RiskResult result : results) {
            TaskResultDTO.RiskAccountDTO detail = new TaskResultDTO.RiskAccountDTO();
            detail.setRiskId(result.getRiskId());
            detail.setAccountId(result.getAccountId());
            detail.setAccountRole(result.getAccountRole());
            detail.setAccountRoleText(getAccountRoleText(result.getAccountRole()));
            detail.setMatchedIndicators(result.getMatchedIndicators());
            detail.setMatchedRules(result.getMatchedRules());
            detail.setCreateTime(result.getCreateTime());

            // 设置账户详细信息
            if (result.getAccount() != null) {
                detail.setAccountNo(result.getAccount().getAccountNo());
                detail.setHolderName(result.getAccount().getHolderName());
                detail.setIdCardNo(result.getAccount().getIdCardNo());
                detail.setPhone(result.getAccount().getPhone());
                detail.setAccountType(result.getAccount().getAccountType());
                detail.setAccountStatus(result.getAccount().getStatus());
            }

            details.add(detail);
        }

        return details;
    }

    /**
     * 获取账户角色文本
     */
    private String getAccountRoleText(Short role) {
        switch (role) {
            case 1: return "受害人";
            case 2: return "可疑账户";
            default: return "未知";
        }
    }



    /**
     * 生成任务代码
     * @return 任务代码
     */
    private String generateTaskCode() {
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timeStr = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        return "TASK_" + dateStr + "_" + timeStr;
    }
}
