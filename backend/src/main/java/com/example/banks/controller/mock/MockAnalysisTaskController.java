package com.example.banks.controller.mock;

import com.example.banks.dto.TaskResultDTO;
import com.example.banks.entity.AnalysisTask;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * 压测专用 - 任务管理Controller
 * 路径：/api/mock/tasks
 * 
 * 功能：
 * - 不查询数据库
 * - 模拟真实响应时间
 * - 返回Mock数据
 * - 用于并发压力测试
 */
@RestController
@RequestMapping("/mock/tasks")
//@CrossOrigin(origins = "*")
public class MockAnalysisTaskController {
    
    private static final Logger logger = LoggerFactory.getLogger(MockAnalysisTaskController.class);
    
    /**
     * 创建任务（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @PostMapping
    public ResponseEntity<AnalysisTask> createTask(@RequestBody AnalysisTask task) {
        logger.info("[压测] 创建任务: {}", task.getTaskName());
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        // 生成Mock返回数据
        task.setTaskId(ThreadLocalRandom.current().nextLong(1000, 999999));
        task.setTaskCode("MOCK_TASK_" + System.currentTimeMillis());
        task.setTaskStatus((short) 0);
        task.setProgress(0);
        task.setAnalyzedAccounts(0);
        task.setResultCount(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        logger.info("[压测] 任务创建成功，任务ID: {}", task.getTaskId());
        return ResponseEntity.ok(task);
    }
    
    /**
     * 获取所有任务（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping
    public ResponseEntity<List<AnalysisTask>> getAllTasks() {
        logger.info("[压测] 获取所有任务列表");
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        // 生成Mock数据
        List<AnalysisTask> tasks = MockDataGenerator.generateTasks(15);
        
        logger.info("[压测] 返回 {} 条任务记录", tasks.size());
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * 分页获取任务（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping("/page")
    public ResponseEntity<Page<AnalysisTask>> getTasksPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("[压测] 分页获取任务: page={}, size={}", page, size);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        // 生成Mock数据
        List<AnalysisTask> tasks = MockDataGenerator.generateTasks(size);
        Pageable pageable = PageRequest.of(page, size);
        Page<AnalysisTask> taskPage = new PageImpl<>(tasks, pageable, 100); // 假设总共100条
        
        logger.info("[压测] 返回第 {} 页，共 {} 条记录", page, tasks.size());
        return ResponseEntity.ok(taskPage);
    }
    
    /**
     * 获取单个任务详情（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<AnalysisTask> getTask(@PathVariable Long taskId) {
        logger.info("[压测] 获取任务详情: taskId={}", taskId);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        // 生成Mock数据
        List<AnalysisTask> tasks = MockDataGenerator.generateTasks(1);
        AnalysisTask task = tasks.get(0);
        task.setTaskId(taskId);
        
        logger.info("[压测] 返回任务: {}", task.getTaskName());
        return ResponseEntity.ok(task);
    }
    
    /**
     * 更新任务（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<AnalysisTask> updateTask(
            @PathVariable Long taskId,
            @RequestBody AnalysisTask task) {
        
        logger.info("[压测] 更新任务: taskId={}", taskId);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        task.setTaskId(taskId);
        task.setUpdateTime(LocalDateTime.now());
        
        logger.info("[压测] 任务更新成功");
        return ResponseEntity.ok(task);
    }
    
    /**
     * 删除任务（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable Long taskId) {
        logger.info("[压测] 删除任务: taskId={}", taskId);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "任务删除成功（Mock）");
        response.put("taskId", taskId);
        
        logger.info("[压测] 任务删除成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 启动任务分析（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @PostMapping("/{taskId}/start")
    public ResponseEntity<Map<String, Object>> startTask(@PathVariable Long taskId) {
        logger.info("[压测] 启动任务分析: taskId={}", taskId);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "任务启动成功（Mock）");
        response.put("taskId", taskId);
        response.put("status", 1); // 运行中
        response.put("startTime", LocalDateTime.now());
        
        logger.info("[压测] 任务启动成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 停止任务（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @PostMapping("/{taskId}/stop")
    public ResponseEntity<Map<String, Object>> stopTask(@PathVariable Long taskId) {
        logger.info("[压测] 停止任务: taskId={}", taskId);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "任务已停止（Mock）");
        response.put("taskId", taskId);
        response.put("status", 3); // 已取消
        
        logger.info("[压测] 任务停止成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取任务结果（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping("/{taskId}/results")
    public ResponseEntity<Page<TaskResultDTO.RiskAccountDTO>> getTaskResults(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("[压测] 获取任务结果: taskId={}, page={}, size={}", taskId, page, size);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        // 生成Mock结果数据
        TaskResultDTO result = MockDataGenerator.generateTaskDetailedResults(taskId, 100);
        List<TaskResultDTO.RiskAccountDTO> riskAccounts = result.getRiskAccounts();
        
        // 分页处理
        int start = Math.min(page * size, riskAccounts.size());
        int end = Math.min(start + size, riskAccounts.size());
        List<TaskResultDTO.RiskAccountDTO> pageData = riskAccounts.subList(start, end);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskResultDTO.RiskAccountDTO> resultPage = 
            new PageImpl<>(pageData, pageable, riskAccounts.size());
        
        logger.info("[压测] 返回 {} 条风险账户记录", pageData.size());
        return ResponseEntity.ok(resultPage);
    }
    
    /**
     * 获取任务详细结果（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping("/{taskId}/detailed-results")
    public ResponseEntity<TaskResultDTO> getTaskDetailedResults(@PathVariable Long taskId) {
        logger.info("[压测] 获取任务详细结果: taskId={}", taskId);
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        // 生成完整Mock数据
        TaskResultDTO result = MockDataGenerator.generateTaskDetailedResults(taskId, 156);
        
        logger.info("[压测] 返回详细结果，共 {} 条风险账户", result.getResultCount());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取任务统计信息（压测版）
     * 模拟延迟：100-1000ms（85%概率），1000-2000ms（15%概率）
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTaskStatistics() {
        logger.info("[压测] 获取任务统计信息");
        
        // 模拟真实响应时间
        ResponseTimeSimulator.simulateRealisticQuery();
        
        Map<String, Object> statistics = MockDataGenerator.generateStatistics();
        
        logger.info("[压测] 返回统计数据");
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 健康检查接口（压测版）
     * 模拟延迟：10-30ms（极快响应）
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        // 极小延迟
        ResponseTimeSimulator.simulateDelay(10, 30);
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("mode", "MOCK_MODE");
        health.put("timestamp", LocalDateTime.now());
        health.put("message", "压测接口运行正常");
        
        return ResponseEntity.ok(health);
    }
}


