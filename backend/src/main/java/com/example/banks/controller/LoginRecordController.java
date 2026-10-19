package com.example.banks.controller;

import com.example.banks.dto.LoginRecordDTO;
import com.example.banks.service.LoginRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录记录控制器
 */
@RestController
@RequestMapping("/logins")
@CrossOrigin(origins = "*")
public class LoginRecordController {

    private static final Logger logger = LoggerFactory.getLogger(LoginRecordController.class);

    @Autowired
    private LoginRecordService loginRecordService;

    /**
     * 分页查询登录记录
     * GET /api/logins?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLoginRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            logger.info("分页查询登录记录: page={}, size={}", page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<LoginRecordDTO> loginPage = loginRecordService.getAllLoginRecords(pageable);
            
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("content", loginPage.getContent());
            pageData.put("totalElements", loginPage.getTotalElements());
            pageData.put("totalPages", loginPage.getTotalPages());
            pageData.put("currentPage", page);
            pageData.put("pageSize", size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pageData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询登录记录失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询登录记录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 搜索登录记录
     * GET /api/logins/search
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchLoginRecords(
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String loginIp,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            logger.info("搜索登录记录 - 账户名: {}, 地区: {}, 时间范围: {}-{}, 页码: {}, 大小: {}", 
                    accountName, loginIp, startTime, endTime, page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<LoginRecordDTO> loginPage = loginRecordService.searchLoginRecords(
                    accountName, loginIp, startTime, endTime, pageable);
            
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("content", loginPage.getContent());
            pageData.put("totalElements", loginPage.getTotalElements());
            pageData.put("totalPages", loginPage.getTotalPages());
            pageData.put("currentPage", page);
            pageData.put("pageSize", size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pageData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("搜索登录记录失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "搜索登录记录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据ID获取登录记录详情
     * GET /api/logins/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLoginRecordById(@PathVariable Long id) {
        try {
            logger.info("根据ID查询登录记录: {}", id);
            
            LoginRecordDTO loginRecord = loginRecordService.getLoginRecordById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loginRecord);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询登录记录详情失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询登录记录详情失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取指定账户的登录记录
     * GET /api/logins/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Map<String, Object>> getAccountLoginRecords(@PathVariable Long accountId) {
        try {
            logger.info("获取账户登录记录: {}", accountId);
            
            List<LoginRecordDTO> loginRecords = loginRecordService.getAccountLoginRecords(accountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loginRecords);
            response.put("count", loginRecords.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取账户登录记录失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取账户登录记录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }


    /**
     * 获取登录统计信息
     * GET /api/logins/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLoginStatistics() {
        try {
            logger.info("获取登录统计信息");
            
            Map<String, Object> statistics = loginRecordService.getLoginStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取登录统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取登录统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取账户登录统计
     * GET /api/logins/account/{accountId}/statistics
     */
    @GetMapping("/account/{accountId}/statistics")
    public ResponseEntity<Map<String, Object>> getAccountLoginStatistics(@PathVariable Long accountId) {
        try {
            logger.info("获取账户登录统计: {}", accountId);
            
            Map<String, Object> statistics = loginRecordService.getAccountLoginStatistics(accountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取账户登录统计失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取账户登录统计失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 测试接口 - 检查服务状态
     * GET /api/logins/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testService() {
        try {
            logger.info("测试登录记录服务");
            
            long totalCount = loginRecordService.getLoginStatistics().get("total_count") != null 
                    ? (Long) loginRecordService.getLoginStatistics().get("total_count") : 0L;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录记录服务正常");
            response.put("totalRecords", totalCount);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("测试登录记录服务失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务异常: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}

