package com.example.banks.controller;

import com.example.banks.dto.TransferRecordDTO;
import com.example.banks.service.TransferRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 转账记录控制器 - 重新实现
 */
@RestController
@RequestMapping("/transfers")
@CrossOrigin(origins = "*")
public class TransferRecordController {

    private static final Logger logger = LoggerFactory.getLogger(TransferRecordController.class);

    @Autowired
    private TransferRecordService transferRecordService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询转账记录
     * GET /api/transfers?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTransferRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            logger.info("分页查询转账记录: page={}, size={}", page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TransferRecordDTO> transferPage = transferRecordService.getAllTransferRecords(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferPage.getContent());
            response.put("totalElements", transferPage.getTotalElements());
            response.put("totalPages", transferPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("message", "查询成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询转账记录失败", e);
            return createErrorResponse("查询转账记录失败: " + e.getMessage());
        }
    }

    /**
     * 搜索转账记录 - 简化版本
     * GET /api/transfers/search
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTransferRecords(
            @RequestParam(required = false) String transferNo,
            @RequestParam(required = false) String fromAccountName,
            @RequestParam(required = false) String toAccountName,
            @RequestParam(required = false) String minAmount,
            @RequestParam(required = false) String maxAmount,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            logger.info("搜索转账记录 - 流水号: {}, 转出账户: {}, 转入账户: {}, 金额范围: {}-{}, 时间范围: {}-{}, 页码: {}, 大小: {}", 
                    transferNo, fromAccountName, toAccountName, minAmount, maxAmount, startTime, endTime, page, size);
            
            // 转换参数
            BigDecimal minAmountValue = parseAmount(minAmount);
            BigDecimal maxAmountValue = parseAmount(maxAmount);
            LocalDateTime startTimeValue = parseTime(startTime);
            LocalDateTime endTimeValue = parseTime(endTime);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TransferRecordDTO> transferPage = transferRecordService.searchTransferRecords(
                    transferNo, fromAccountName, toAccountName, 
                    minAmountValue, maxAmountValue, startTimeValue, endTimeValue, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferPage.getContent());
            response.put("totalElements", transferPage.getTotalElements());
            response.put("totalPages", transferPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("message", "搜索成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("搜索转账记录失败", e);
            return createErrorResponse("搜索转账记录失败: " + e.getMessage());
        }
    }

    /**
     * 快速筛选 - 大额交易
     * GET /api/transfers/large-amount
     */
    @GetMapping("/large-amount")
    public ResponseEntity<Map<String, Object>> getLargeAmountTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            logger.info("获取大额转账记录");
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TransferRecordDTO> transferPage = transferRecordService.getLargeAmountTransfers(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferPage.getContent());
            response.put("totalElements", transferPage.getTotalElements());
            response.put("totalPages", transferPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("message", "获取大额交易成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取大额转账记录失败", e);
            return createErrorResponse("获取大额转账记录失败: " + e.getMessage());
        }
    }

    /**
     * 快速筛选 - 今日交易
     * GET /api/transfers/today
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            logger.info("获取今日转账记录");
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TransferRecordDTO> transferPage = transferRecordService.getTodayTransfers(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferPage.getContent());
            response.put("totalElements", transferPage.getTotalElements());
            response.put("totalPages", transferPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("message", "获取今日交易成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取今日转账记录失败", e);
            return createErrorResponse("获取今日转账记录失败: " + e.getMessage());
        }
    }

    /**
     * 快速筛选 - 本周交易
     * GET /api/transfers/week
     */
    @GetMapping("/week")
    public ResponseEntity<Map<String, Object>> getWeekTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            logger.info("获取本周转账记录");
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TransferRecordDTO> transferPage = transferRecordService.getWeekTransfers(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferPage.getContent());
            response.put("totalElements", transferPage.getTotalElements());
            response.put("totalPages", transferPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("message", "获取本周交易成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取本周转账记录失败", e);
            return createErrorResponse("获取本周转账记录失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取转账记录详情
     * GET /api/transfers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTransferRecordById(@PathVariable Long id) {
        try {
            logger.info("根据ID查询转账记录: {}", id);
            
            TransferRecordDTO transferRecord = transferRecordService.getTransferRecordById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferRecord);
            response.put("message", "查询成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询转账记录详情失败", e);
            return createErrorResponse("查询转账记录详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据流水号获取转账记录
     * GET /api/transfers/by-no/{transferNo}
     */
    @GetMapping("/by-no/{transferNo}")
    public ResponseEntity<Map<String, Object>> getTransferRecordByNo(@PathVariable String transferNo) {
        try {
            logger.info("根据流水号查询转账记录: {}", transferNo);
            
            TransferRecordDTO transferRecord = transferRecordService.getTransferRecordByNo(transferNo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferRecord);
            response.put("message", "查询成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("根据流水号查询转账记录失败", e);
            return createErrorResponse("根据流水号查询转账记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定账户的转账记录
     * GET /api/transfers/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Map<String, Object>> getAccountTransferRecords(@PathVariable Long accountId) {
        try {
            logger.info("获取账户转账记录: {}", accountId);
            
            List<TransferRecordDTO> transferRecords = transferRecordService.getAccountTransferRecords(accountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferRecords);
            response.put("count", transferRecords.size());
            response.put("message", "查询成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取账户转账记录失败", e);
            return createErrorResponse("获取账户转账记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定账户的转出记录
     * GET /api/transfers/account/{accountId}/outbound
     */
    @GetMapping("/account/{accountId}/outbound")
    public ResponseEntity<Map<String, Object>> getAccountOutboundRecords(@PathVariable Long accountId) {
        try {
            logger.info("获取账户转出记录: {}", accountId);
            
            List<TransferRecordDTO> transferRecords = transferRecordService.getAccountOutboundRecords(accountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferRecords);
            response.put("count", transferRecords.size());
            response.put("message", "查询成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取账户转出记录失败", e);
            return createErrorResponse("获取账户转出记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定账户的转入记录
     * GET /api/transfers/account/{accountId}/inbound
     */
    @GetMapping("/account/{accountId}/inbound")
    public ResponseEntity<Map<String, Object>> getAccountInboundRecords(@PathVariable Long accountId) {
        try {
            logger.info("获取账户转入记录: {}", accountId);
            
            List<TransferRecordDTO> transferRecords = transferRecordService.getAccountInboundRecords(accountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transferRecords);
            response.put("count", transferRecords.size());
            response.put("message", "查询成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取账户转入记录失败", e);
            return createErrorResponse("获取账户转入记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取转账统计信息
     * GET /api/transfers/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTransferStatistics() {
        try {
            logger.info("获取转账统计信息");
            
            Map<String, Object> statistics = transferRecordService.getTransferStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            response.put("message", "获取统计信息成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取转账统计信息失败", e);
            return createErrorResponse("获取转账统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取账户转账统计
     * GET /api/transfers/account/{accountId}/statistics
     */
    @GetMapping("/account/{accountId}/statistics")
    public ResponseEntity<Map<String, Object>> getAccountTransferStatistics(@PathVariable Long accountId) {
        try {
            logger.info("获取账户转账统计: {}", accountId);
            
            Map<String, Object> statistics = transferRecordService.getAccountTransferStatistics(accountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            response.put("message", "获取账户统计信息成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取账户转账统计失败", e);
            return createErrorResponse("获取账户转账统计失败: " + e.getMessage());
        }
    }

    /**
     * 测试接口 - 检查服务状态
     * GET /api/transfers/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testService() {
        try {
            logger.info("测试转账记录服务");
            
            Map<String, Object> statistics = transferRecordService.getTransferStatistics();
            Long totalCount = (Long) statistics.get("total_count");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "转账记录服务正常");
            response.put("totalRecords", totalCount);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("测试转账记录服务失败", e);
            return createErrorResponse("服务异常: " + e.getMessage());
        }
    }

    /**
     * 解析金额字符串
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(amountStr.trim());
        } catch (NumberFormatException e) {
            logger.warn("无法解析金额: {}", amountStr);
            return null;
        }
    }

    /**
     * 解析时间字符串
     */
    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(timeStr.trim(), TIME_FORMATTER);
        } catch (Exception e) {
            logger.warn("无法解析时间: {}", timeStr);
            return null;
        }
    }

    /**
     * 创建错误响应
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("data", null);
        return ResponseEntity.status(500).body(errorResponse);
    }
}