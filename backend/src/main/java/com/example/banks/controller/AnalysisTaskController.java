package com.example.banks.controller;

import com.example.banks.entity.AnalysisTask;
import com.example.banks.entity.RiskResult;
import com.example.banks.dto.TaskCreateDTO;
import com.example.banks.dto.TaskResultDTO;
import com.example.banks.service.AnalysisTaskService;
import com.example.banks.repository.RiskResultRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分析任务管理控制器
 * 提供任务管理的REST API接口
 */
@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "*")
public class AnalysisTaskController {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisTaskController.class);

    @Autowired
    private AnalysisTaskService taskService;

    @Autowired
    private RiskResultRepository riskResultRepository;

    /**
     * 获取所有任务
     * GET /api/tasks
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTasks() {
        try {
            logger.info("获取所有分析任务");
            List<AnalysisTask> tasks = taskService.getAllTasks();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "获取任务列表成功",
                    "data", tasks,
                    "total", tasks.size()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取任务列表失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "获取任务列表失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 根据ID获取任务详情
     * GET /api/tasks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTaskById(@PathVariable Long id) {
        try {
            logger.info("获取任务详情: {}", id);
            Optional<AnalysisTask> task = taskService.getTaskById(id);

            if (task.isPresent()) {
                Map<String, Object> response = Map.of(
                        "success", true,
                        "message", "获取任务详情成功",
                        "data", task.get()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "任务不存在"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            logger.error("获取任务详情失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "获取任务详情失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 创建新任务
     * POST /api/tasks
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@Valid @RequestBody TaskCreateDTO dto) {
        try {
            logger.info("创建新任务: {}", dto.getTaskName());
            AnalysisTask createdTask = taskService.createTask(dto);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "任务创建成功",
                    "data", createdTask
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("创建任务失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("创建任务失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "创建任务失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 创建分离规则任务
     * POST /api/tasks/separate
     */
    @PostMapping("/separate")
    public ResponseEntity<Map<String, Object>> createSeparateRuleTask(@Valid @RequestBody TaskCreateDTO dto) {
        try {
            logger.info("创建分离规则任务: {}", dto.getTaskName());
            AnalysisTask createdTask = taskService.createSeparateRuleTask(dto);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "分离规则任务创建成功",
                    "data", createdTask
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("创建分离规则任务失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("创建分离规则任务失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "创建分离规则任务失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 启动任务
     * POST /api/tasks/{id}/start
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startTask(@PathVariable Long id) {
        try {
            logger.info("启动任务: {}", id);
            AnalysisTask updatedTask = taskService.startTask(id);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "任务启动成功",
                    "data", updatedTask
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("启动任务失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("启动任务失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "启动任务失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 停止任务
     * POST /api/tasks/{id}/stop
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopTask(@PathVariable Long id) {
        try {
            logger.info("停止任务: {}", id);
            AnalysisTask updatedTask = taskService.stopTask(id);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "任务停止成功",
                    "data", updatedTask
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("停止任务失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("停止任务失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "停止任务失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 删除任务
     * DELETE /api/tasks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable Long id) {
        try {
            logger.info("删除任务: {}", id);
            taskService.deleteTask(id);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "任务删除成功"
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("删除任务失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("删除任务失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "删除任务失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 根据状态获取任务
     * GET /api/tasks/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getTasksByStatus(@PathVariable Short status) {
        try {
            logger.info("根据状态获取任务: {}", status);
            List<AnalysisTask> tasks = taskService.getTasksByStatus(status);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "获取任务成功",
                    "data", tasks,
                    "total", tasks.size()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("根据状态获取任务失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "获取任务失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 搜索任务
     * GET /api/tasks/search?keyword={keyword}
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTasks(@RequestParam String keyword) {
        try {
            logger.info("搜索任务: {}", keyword);
            List<AnalysisTask> tasks = taskService.searchTasks(keyword);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "搜索完成",
                    "data", tasks,
                    "total", tasks.size(),
                    "keyword", keyword
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("搜索任务失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "搜索失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取任务统计信息
     * GET /api/tasks/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTaskStatistics() {
        try {
            logger.info("获取任务统计信息");
            Map<String, Object> statistics = taskService.getTaskStatistics();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "获取统计信息成功",
                    "data", statistics
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "获取统计信息失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取任务结果
     * GET /api/tasks/{id}/results
     */
    @GetMapping("/{id}/results")
    public ResponseEntity<Map<String, Object>> getTaskResults(@PathVariable Long id) {
        try {
            logger.info("获取任务结果: {}", id);

            // 检查任务是否存在
            Optional<AnalysisTask> taskOpt = taskService.getTaskById(id);
            if (!taskOpt.isPresent()) {
                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "任务不存在"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            AnalysisTask task = taskOpt.get();

            // 获取风险结果
            List<RiskResult> allResults = taskService.getTaskResults(id);
            List<RiskResult> victims = riskResultRepository.findVictimsByTaskId(id);
            List<RiskResult> suspicious = riskResultRepository.findSuspiciousAccountsByTaskId(id);

            Map<String, Object> results = Map.of(
                    "task", task,
                    "allResults", allResults,
                    "victims", victims,
                    "suspicious", suspicious,
                    "victimCount", victims.size(),
                    "suspiciousCount", suspicious.size(),
                    "totalCount", allResults.size()
            );

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "获取任务结果成功",
                    "data", results
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取任务结果失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "获取任务结果失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取任务的详细结果（包含统计和账户信息 - 分页版本）
     * GET /api/tasks/{id}/detailed-results?page=1&pageSize=100
     */
    @GetMapping("/{id}/detailed-results")
    public ResponseEntity<Map<String, Object>> getTaskDetailedResults(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(required = false) Short accountRole) {
        try {
            logger.info("获取任务 {} 的详细结果 (页码: {}, 每页: {}, 角色: {})", id, page, pageSize, accountRole);

            // 限制pageSize，避免一次请求过多数据（最大支持300000条）
            if (pageSize > 300000) {
                pageSize = 300000;
            }
            if (page < 1) {
                page = 1;
            }

            TaskResultDTO results = taskService.getTaskDetailedResultsPaged(id, page, pageSize, accountRole);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "获取详细结果成功",
                    "data", results
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("获取任务详细结果失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (IllegalStateException e) {
            logger.warn("获取任务详细结果失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("获取任务详细结果失败", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "获取任务详细结果失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

