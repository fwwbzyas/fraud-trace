package com.example.banks.util;

import com.example.banks.entity.AnalysisIndicator;
import com.example.banks.entity.AnalysisTask;
import com.example.banks.entity.LoginRecord;
import com.example.banks.entity.TransferRecord;
import com.example.banks.dto.TaskResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock数据生成器
 * 用于压测接口生成模拟数据
 */
public class MockDataGenerator {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Random random = ThreadLocalRandom.current();
    
    private static final String[] TASK_NAMES = {
        "2024Q1反诈排查", "2024Q2风险分析", "重点账户追查", "可疑转账监控", 
        "大额交易分析", "异常登录检测", "跨境洗钱排查", "网络赌博监控"
    };
    
    private static final String[] INDICATOR_NAMES = {
        "大额快进快出次数", "登录后转账次数", "累计入账金额", 
        "异地登录次数", "深夜交易频率", "单日转账笔数"
    };
    
    private static final String[] CITIES = {
        "北京市", "上海市", "广州市", "深圳市", "杭州市", "成都市", 
        "武汉市", "西安市", "重庆市", "天津市", "南京市", "苏州市"
    };
    
    /**
     * 生成指标列表
     */
    public static List<AnalysisIndicator> generateIndicators(int count) {
        List<AnalysisIndicator> indicators = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            AnalysisIndicator indicator = new AnalysisIndicator();
            indicator.setIndicatorId((long) (i + 1));
            indicator.setIndicatorCode("INDICATOR_" + (char)('A' + i));
            indicator.setIndicatorName(INDICATOR_NAMES[i % INDICATOR_NAMES.length]);
            indicator.setIndicatorDesc("压测模拟指标 - " + indicator.getIndicatorName());
            indicator.setIsCore((short) (i < 3 ? 1 : 0));
            indicator.setIsActive((short) 1);
            indicator.setCreateTime(LocalDateTime.now().minusDays(random.nextInt(30)));
            indicator.setUpdateTime(LocalDateTime.now());
            
            // 生成参数配置
            ObjectNode paramConfig = objectMapper.createObjectNode();
            paramConfig.put("large_amount_threshold", 50000 + random.nextInt(50000));
            paramConfig.put("quick_transfer_minutes", 2 + random.nextInt(10));
            paramConfig.put("min_transfer_amount", 1000 + random.nextInt(9000));
            indicator.setParamConfig(paramConfig);
            
            indicators.add(indicator);
        }
        
        return indicators;
    }
    
    /**
     * 生成任务列表
     */
    public static List<AnalysisTask> generateTasks(int count) {
        List<AnalysisTask> tasks = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            AnalysisTask task = new AnalysisTask();
            task.setTaskId((long) (i + 1));
            task.setTaskCode("TASK_" + generateRandomCode());
            task.setTaskName(TASK_NAMES[random.nextInt(TASK_NAMES.length)]);
            task.setIndicatorIds("1,2,3");
            task.setTimeRange(30 + random.nextInt(335));
            task.setTaskTimeRange(365);
            task.setIndicatorTimeWindow(30);
            
            // 生成规则配置
            ObjectNode ruleConfig = objectMapper.createObjectNode();
            ruleConfig.put("type", "separate_rules");
            ruleConfig.put("victimRule", "INDICATOR_A > 0 AND INDICATOR_B > 0");
            ruleConfig.put("suspectRule", "INDICATOR_C = 0");
            task.setRuleConfig(ruleConfig);
            
            // 随机任务状态
            short status = (short) random.nextInt(4); // 0-3
            task.setTaskStatus(status);
            task.setProgress(status == 2 ? 100 : (status == 1 ? random.nextInt(100) : 0));
            
            task.setStartTime(LocalDateTime.now().minusHours(random.nextInt(48)));
            if (status == 2 || status == 3) {
                task.setEndTime(task.getStartTime().plusMinutes(10 + random.nextInt(50)));
            }
            
            task.setAnalyzedAccounts(random.nextInt(10000));
            task.setResultCount(random.nextInt(500));
            task.setCreateTime(LocalDateTime.now().minusDays(random.nextInt(30)));
            task.setUpdateTime(LocalDateTime.now());
            
            tasks.add(task);
        }
        
        return tasks;
    }
    
    /**
     * 生成转账记录列表
     */
    public static List<TransferRecord> generateTransferRecords(int count) {
        List<TransferRecord> records = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            TransferRecord record = new TransferRecord();
            record.setTransferId((long) (i + 1));
            record.setFromAccountId((long) (1000 + random.nextInt(9000)));
            record.setToAccountId((long) (1000 + random.nextInt(9000)));
            record.setTransferNo("TXN" + System.currentTimeMillis() + String.format("%06d", i));
            record.setAmount(BigDecimal.valueOf(100 + random.nextDouble() * 99900).setScale(2, java.math.RoundingMode.HALF_UP));
            record.setTransferTime(LocalDateTime.now().minusHours(random.nextInt(720)));
            
            // 设置关联字段
            record.setFromAccountName("张" + (char)('三' + random.nextInt(20)));
            record.setToAccountName("李" + (char)('四' + random.nextInt(20)));
            record.setFromAccountPhone("138" + String.format("%08d", random.nextInt(100000000)));
            record.setToAccountPhone("139" + String.format("%08d", random.nextInt(100000000)));
            
            records.add(record);
        }
        
        return records;
    }
    
    /**
     * 生成登录记录列表
     */
    public static List<LoginRecord> generateLoginRecords(int count) {
        List<LoginRecord> records = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            LoginRecord record = new LoginRecord();
            record.setRecordId((long) (i + 1));
            record.setAccountId((long) (1000 + random.nextInt(9000)));
            record.setLoginTime(LocalDateTime.now().minusHours(random.nextInt(720)));
            record.setLoginIp(CITIES[random.nextInt(CITIES.length)]);
            
            // 设置关联字段
            record.setAccountName("用户" + String.format("%04d", random.nextInt(10000)));
            record.setAccountNo("6222" + String.format("%015d", random.nextLong(1000000000000000L)));
            record.setPhone("138" + String.format("%08d", random.nextInt(100000000)));
            
            records.add(record);
        }
        
        return records;
    }
    
    /**
     * 生成任务详细结果
     */
    public static TaskResultDTO generateTaskDetailedResults(Long taskId, int count) {
        TaskResultDTO result = new TaskResultDTO();
        
        result.setTaskId(taskId);
        result.setTaskName(TASK_NAMES[random.nextInt(TASK_NAMES.length)]);
        result.setTaskCode("TASK_" + generateRandomCode());
        result.setTaskStatus((short) 2); // 已完成
        result.setTimeRange(365);
        result.setStartTime(LocalDateTime.now().minusHours(2));
        result.setEndTime(LocalDateTime.now().minusMinutes(30));
        result.setAnalyzedAccounts(count);
        result.setResultCount(count);
        
        // 生成统计信息
        TaskResultDTO.RiskStatistics statistics = new TaskResultDTO.RiskStatistics();
        statistics.setTotalRiskAccounts(count);
        statistics.setVictimAccounts((int)(count * 0.4));
        statistics.setSuspiciousAccounts((int)(count * 0.6));
        result.setStatistics(statistics);
        
        // 生成风险账户列表
        List<TaskResultDTO.RiskAccountDTO> riskAccounts = new ArrayList<>();
        for (int i = 0; i < Math.min(count, 100); i++) { // 最多返回100条
            TaskResultDTO.RiskAccountDTO account = new TaskResultDTO.RiskAccountDTO();
            account.setRiskId((long) (i + 1));
            account.setAccountId((long) (1000 + random.nextInt(9000)));
            account.setAccountNo("6222" + String.format("%015d", random.nextLong(1000000000000000L)));
            account.setHolderName("持有人" + String.format("%04d", i));
            account.setIdCardNo("3301" + String.format("%014d", random.nextLong(100000000000000L)));
            account.setPhone("138" + String.format("%08d", random.nextInt(100000000)));
            account.setAccountRole((short) (random.nextBoolean() ? 1 : 2));
            account.setAccountRoleText(account.getAccountRole() == 1 ? "受害人" : "可疑账户");
            account.setAccountType((short) 1);
            account.setAccountStatus((short) 0);
            
            // 生成指标值
            ObjectNode indicators = objectMapper.createObjectNode();
            indicators.put("INDICATOR_A", random.nextInt(10));
            indicators.put("INDICATOR_B", random.nextInt(20));
            indicators.put("INDICATOR_C", random.nextDouble() * 100000);
            account.setMatchedIndicators(indicators);
            
            account.setMatchedRules(account.getAccountRole() == 1 ? 
                "受害人规则: INDICATOR_A > 0 AND INDICATOR_B > 0" : 
                "可疑分子规则: INDICATOR_C = 0");
            account.setCreateTime(LocalDateTime.now());
            
            riskAccounts.add(account);
        }
        result.setRiskAccounts(riskAccounts);
        
        return result;
    }
    
    /**
     * 生成随机任务代码
     */
    private static String generateRandomCode() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%04d%02d%02d_%02d%02d%02d",
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
            now.getHour(), now.getMinute(), now.getSecond());
    }
    
    /**
     * 生成统计数据
     */
    public static Map<String, Object> generateStatistics() {
        return Map.of(
            "totalCount", 100 + random.nextInt(900),
            "activeCount", 50 + random.nextInt(450),
            "coreCount", 3,
            "extendedCount", 10 + random.nextInt(40),
            "pendingCount", random.nextInt(20),
            "runningCount", random.nextInt(10),
            "completedCount", 50 + random.nextInt(450),
            "failedCount", random.nextInt(10),
            "todayCount", random.nextInt(50)
        );
    }
}

