package com.example.banks.controller;

import com.example.banks.entity.AnalysisIndicator;
import com.example.banks.dto.IndicatorConfigDTO;
import com.example.banks.service.AnalysisIndicatorService;
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
 * 分析指标管理控制器
 * 提供指标配置的REST API接口
 */
@RestController
@RequestMapping("/indicators")
@CrossOrigin(origins = "*")
public class AnalysisIndicatorController {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalysisIndicatorController.class);
    
    @Autowired
    private AnalysisIndicatorService indicatorService;
    
    /**
     * 获取所有指标（包括停用的和扩展的）
     * GET /api/indicators
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllIndicators() {
        try {
            logger.info("获取所有指标");
            List<AnalysisIndicator> indicators = indicatorService.getAllIndicators();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "获取指标列表成功",
                "data", indicators,
                "total", indicators.size()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取指标列表失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取指标列表失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取激活的指标
     * GET /api/indicators/active
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveIndicators() {
        try {
            logger.info("获取激活的指标");
            List<AnalysisIndicator> indicators = indicatorService.getAllActiveIndicators();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "获取激活指标成功",
                "data", indicators,
                "total", indicators.size()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取激活指标失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取激活指标失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取所有指标（包括未激活的）- 调试用
     * GET /api/indicators/all
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllIndicatorsDebug() {
        try {
            logger.info("获取所有指标（包括未激活的）");
            List<AnalysisIndicator> indicators = indicatorService.getAllIndicators();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "获取所有指标成功",
                "data", indicators,
                "total", indicators.size()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取所有指标失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取所有指标失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取核心指标
     * GET /api/indicators/core
     */
    @GetMapping("/core")
    public ResponseEntity<Map<String, Object>> getCoreIndicators() {
        try {
            logger.info("获取核心指标");
            List<AnalysisIndicator> indicators = indicatorService.getCoreIndicators();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "获取核心指标成功",
                "data", indicators,
                "total", indicators.size()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取核心指标失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取核心指标失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取扩展指标
     * GET /api/indicators/extended
     */
    @GetMapping("/extended")
    public ResponseEntity<Map<String, Object>> getExtendedIndicators() {
        try {
            logger.info("获取扩展指标");
            List<AnalysisIndicator> indicators = indicatorService.getExtendedIndicators();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "获取扩展指标成功",
                "data", indicators,
                "total", indicators.size()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取扩展指标失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取扩展指标失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 根据ID获取指标详情
     * GET /api/indicators/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getIndicatorById(@PathVariable Long id) {
        try {
            logger.info("获取指标详情: {}", id);
            Optional<AnalysisIndicator> indicator = indicatorService.getIndicatorById(id);
            
            if (indicator.isPresent()) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "获取指标详情成功",
                    "data", indicator.get()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "指标不存在"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            logger.error("获取指标详情失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取指标详情失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 根据代码获取指标详情
     * GET /api/indicators/code/{code}
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Map<String, Object>> getIndicatorByCode(@PathVariable String code) {
        try {
            logger.info("根据代码获取指标: {}", code);
            Optional<AnalysisIndicator> indicator = indicatorService.getIndicatorByCode(code);
            
            if (indicator.isPresent()) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "获取指标成功",
                    "data", indicator.get()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "指标不存在"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            logger.error("根据代码获取指标失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "获取指标失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 创建新指标
     * POST /api/indicators
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createIndicator(@Valid @RequestBody IndicatorConfigDTO dto) {
        try {
            logger.info("创建新指标: {}", dto.getIndicatorCode());
            AnalysisIndicator createdIndicator = indicatorService.createIndicator(dto);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "指标创建成功",
                "data", createdIndicator
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("创建指标失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("创建指标失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "创建指标失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 更新指标配置
     * PUT /api/indicators/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateIndicator(@PathVariable Long id, 
                                                              @Valid @RequestBody IndicatorConfigDTO dto) {
        try {
            logger.info("更新指标: {}", id);
            AnalysisIndicator updatedIndicator = indicatorService.updateIndicator(id, dto);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "指标更新成功",
                "data", updatedIndicator
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("更新指标失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("更新指标失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "更新指标失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 更新指标参数
     * PATCH /api/indicators/{code}/params
     */
    @PatchMapping("/{code}/params")
    public ResponseEntity<Map<String, Object>> updateIndicatorParams(@PathVariable String code,
                                                                    @RequestBody Map<String, Object> paramUpdates) {
        try {
            logger.info("更新指标参数: {}", code);
            AnalysisIndicator updatedIndicator = indicatorService.updateIndicatorParams(code, paramUpdates);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "参数更新成功",
                "data", updatedIndicator
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("更新参数失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("更新参数失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "更新参数失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 切换指标激活状态
     * PATCH /api/indicators/{code}/toggle
     */
    @PatchMapping("/{code}/toggle")
    public ResponseEntity<Map<String, Object>> toggleIndicatorStatus(@PathVariable String code,
                                                                    @RequestParam boolean isActive) {
        try {
            logger.info("切换指标状态: {}, 激活: {}", code, isActive);
            AnalysisIndicator updatedIndicator = indicatorService.toggleIndicatorStatus(code, isActive);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "状态切换成功",
                "data", updatedIndicator
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("切换状态失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("切换状态失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "切换状态失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 删除指标
     * DELETE /api/indicators/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteIndicator(@PathVariable Long id) {
        try {
            logger.info("删除指标: {}", id);
            indicatorService.deleteIndicator(id);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "指标删除成功"
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("删除指标失败: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("删除指标失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "删除指标失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 搜索指标
     * GET /api/indicators/search?keyword={keyword}
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchIndicators(@RequestParam String keyword) {
        try {
            logger.info("搜索指标: {}", keyword);
            List<AnalysisIndicator> indicators = indicatorService.searchIndicators(keyword);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "搜索完成",
                "data", indicators,
                "total", indicators.size(),
                "keyword", keyword
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("搜索指标失败", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "搜索失败: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取指标统计信息
     * GET /api/indicators/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            logger.info("获取指标统计信息");
            Map<String, Object> statistics = indicatorService.getIndicatorStatistics();
            
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
}
