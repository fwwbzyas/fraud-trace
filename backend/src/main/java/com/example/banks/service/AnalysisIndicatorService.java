package com.example.banks.service;

import com.example.banks.entity.AnalysisIndicator;
import com.example.banks.dto.IndicatorConfigDTO;
import com.example.banks.repository.AnalysisIndicatorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分析指标业务逻辑服务
 * 提供指标配置的增删改查和参数管理功能
 */
@Service
@Transactional
public class AnalysisIndicatorService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalysisIndicatorService.class);
    
    @Autowired
    private AnalysisIndicatorRepository indicatorRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 获取所有激活的指标
     * @return 指标列表
     */
    @Transactional(readOnly = true)
    public List<AnalysisIndicator> getAllActiveIndicators() {
        logger.info("获取所有激活的指标");
        return indicatorRepository.findAllActiveIndicators();
    }
    
    /**
     * 获取所有指标（包括未激活的）
     * @return 指标列表
     */
    @Transactional(readOnly = true)
    public List<AnalysisIndicator> getAllIndicators() {
        logger.info("获取所有指标（包括未激活的）");
        return indicatorRepository.findAll();
    }
    
    /**
     * 获取核心指标
     * @return 核心指标列表
     */
    @Transactional(readOnly = true)
    public List<AnalysisIndicator> getCoreIndicators() {
        logger.info("获取核心指标");
        return indicatorRepository.findCoreIndicators();
    }
    
    /**
     * 获取扩展指标
     * @return 扩展指标列表
     */
    @Transactional(readOnly = true)
    public List<AnalysisIndicator> getExtendedIndicators() {
        logger.info("获取扩展指标");
        return indicatorRepository.findExtendedIndicators();
    }
    
    /**
     * 根据指标代码获取指标
     * @param indicatorCode 指标代码
     * @return 指标实体
     */
    @Transactional(readOnly = true)
    public Optional<AnalysisIndicator> getIndicatorByCode(String indicatorCode) {
        logger.info("根据代码获取指标: {}", indicatorCode);
        return indicatorRepository.findByIndicatorCode(indicatorCode);
    }
    
    /**
     * 根据ID获取指标
     * @param indicatorId 指标ID
     * @return 指标实体
     */
    @Transactional(readOnly = true)
    public Optional<AnalysisIndicator> getIndicatorById(Long indicatorId) {
        logger.info("根据ID获取指标: {}", indicatorId);
        return indicatorRepository.findById(indicatorId);
    }
    
    /**
     * 创建新指标
     * @param dto 指标配置DTO
     * @return 创建的指标实体
     */
    public AnalysisIndicator createIndicator(IndicatorConfigDTO dto) {
        logger.info("创建新指标: {}", dto.getIndicatorCode());
        
        // 检查指标代码是否已存在
        if (indicatorRepository.existsByIndicatorCode(dto.getIndicatorCode())) {
            throw new IllegalArgumentException("指标代码已存在: " + dto.getIndicatorCode());
        }
        
        // 验证参数配置
        validateParamConfig(dto.getParamConfig());
        
        AnalysisIndicator indicator = new AnalysisIndicator();
        BeanUtils.copyProperties(dto, indicator);
        indicator.setCreateTime(LocalDateTime.now());
        indicator.setUpdateTime(LocalDateTime.now());
        
        AnalysisIndicator savedIndicator = indicatorRepository.save(indicator);
        logger.info("指标创建成功: {}", savedIndicator.getIndicatorCode());
        
        return savedIndicator;
    }
    
    /**
     * 更新指标配置
     * @param indicatorId 指标ID
     * @param dto 更新的指标配置
     * @return 更新后的指标实体
     */
    public AnalysisIndicator updateIndicator(Long indicatorId, IndicatorConfigDTO dto) {
        logger.info("更新指标配置: {}", indicatorId);
        
        AnalysisIndicator existingIndicator = indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("指标不存在: " + indicatorId));
        
        // 检查指标代码是否被其他指标使用
        if (!existingIndicator.getIndicatorCode().equals(dto.getIndicatorCode()) &&
            indicatorRepository.existsByIndicatorCode(dto.getIndicatorCode())) {
            throw new IllegalArgumentException("指标代码已被其他指标使用: " + dto.getIndicatorCode());
        }
        
        // 验证参数配置
        validateParamConfig(dto.getParamConfig());
        
        // 更新属性
        existingIndicator.setIndicatorCode(dto.getIndicatorCode());
        existingIndicator.setIndicatorName(dto.getIndicatorName());
        existingIndicator.setIndicatorDesc(dto.getIndicatorDesc());
        existingIndicator.setParamConfig(dto.getParamConfig());
        existingIndicator.setIsCore(dto.getIsCore());
        existingIndicator.setIsActive(dto.getIsActive());
        existingIndicator.setUpdateTime(LocalDateTime.now());
        
        AnalysisIndicator updatedIndicator = indicatorRepository.save(existingIndicator);
        logger.info("指标更新成功: {}", updatedIndicator.getIndicatorCode());
        
        return updatedIndicator;
    }
    
    /**
     * 更新指标参数
     * @param indicatorCode 指标代码
     * @param paramUpdates 参数更新映射
     * @return 更新后的指标实体
     */
    public AnalysisIndicator updateIndicatorParams(String indicatorCode, Map<String, Object> paramUpdates) {
        logger.info("更新指标参数: {}, 参数: {}", indicatorCode, paramUpdates);
        
        AnalysisIndicator indicator = indicatorRepository.findByIndicatorCode(indicatorCode)
                .orElseThrow(() -> new IllegalArgumentException("指标不存在: " + indicatorCode));
        
        try {
            // 更新isCore字段
            if (paramUpdates.containsKey("isCore")) {
                Object isCoreValue = paramUpdates.get("isCore");
                if (isCoreValue instanceof Integer) {
                    indicator.setIsCore(((Integer) isCoreValue).shortValue());
                } else if (isCoreValue instanceof Short) {
                    indicator.setIsCore((Short) isCoreValue);
                }
                paramUpdates.remove("isCore"); // 从参数配置中移除
            }
            
            // 更新isActive字段
            if (paramUpdates.containsKey("isActive")) {
                Object isActiveValue = paramUpdates.get("isActive");
                if (isActiveValue instanceof Integer) {
                    indicator.setIsActive(((Integer) isActiveValue).shortValue());
                } else if (isActiveValue instanceof Short) {
                    indicator.setIsActive((Short) isActiveValue);
                }
                paramUpdates.remove("isActive"); // 从参数配置中移除
            }
            
            // 更新userType字段
            if (paramUpdates.containsKey("user_type")) {
                indicator.setUserType((String) paramUpdates.get("user_type"));
                // user_type保留在参数配置中，不移除
            }
            
            // 更新JSON参数配置（如果有剩余参数）
            if (!paramUpdates.isEmpty()) {
                JsonNode currentConfig = indicator.getParamConfig();
                @SuppressWarnings("unchecked")
                Map<String, Object> configMap = objectMapper.convertValue(currentConfig, Map.class);
                
                // 更新参数
                configMap.putAll(paramUpdates);
                
                // 转换回JsonNode
                JsonNode updatedConfig = objectMapper.valueToTree(configMap);
                
                // 验证更新后的配置
                validateParamConfig(updatedConfig);
                
                indicator.setParamConfig(updatedConfig);
            }
            
            indicator.setUpdateTime(LocalDateTime.now());
            
            AnalysisIndicator updatedIndicator = indicatorRepository.save(indicator);
            logger.info("指标参数更新成功: {}", indicatorCode);
            
            return updatedIndicator;
            
        } catch (Exception e) {
            logger.error("更新指标参数失败: {}", e.getMessage());
            throw new RuntimeException("更新指标参数失败: " + e.getMessage());
        }
    }
    
    /**
     * 激活/停用指标
     * @param indicatorCode 指标代码
     * @param isActive 是否激活
     * @return 更新后的指标实体
     */
    public AnalysisIndicator toggleIndicatorStatus(String indicatorCode, boolean isActive) {
        logger.info("切换指标状态: {}, 激活: {}", indicatorCode, isActive);
        
        AnalysisIndicator indicator = indicatorRepository.findByIndicatorCode(indicatorCode)
                .orElseThrow(() -> new IllegalArgumentException("指标不存在: " + indicatorCode));
        
        indicator.setIsActive((short) (isActive ? 1 : 0));
        indicator.setUpdateTime(LocalDateTime.now());
        
        AnalysisIndicator updatedIndicator = indicatorRepository.save(indicator);
        logger.info("指标状态切换成功: {}", indicatorCode);
        
        return updatedIndicator;
    }
    
    /**
     * 删除指标
     * @param indicatorId 指标ID
     */
    public void deleteIndicator(Long indicatorId) {
        logger.info("删除指标: {}", indicatorId);
        
        AnalysisIndicator indicator = indicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("指标不存在: " + indicatorId));
        
        // 检查指标是否被分析任务使用
        // TODO: 添加检查指标是否被使用的逻辑
        
        indicatorRepository.delete(indicator);
        logger.info("指标删除成功: {}", indicator.getIndicatorCode());
    }
    
    /**
     * 根据指标代码列表获取指标
     * @param indicatorCodes 指标代码列表
     * @return 指标列表
     */
    @Transactional(readOnly = true)
    public List<AnalysisIndicator> getIndicatorsByCodes(List<String> indicatorCodes) {
        logger.info("根据代码列表获取指标: {}", indicatorCodes);
        return indicatorRepository.findByIndicatorCodes(indicatorCodes);
    }
    
    /**
     * 搜索指标
     * @param keyword 关键字
     * @return 匹配的指标列表
     */
    @Transactional(readOnly = true)
    public List<AnalysisIndicator> searchIndicators(String keyword) {
        logger.info("搜索指标: {}", keyword);
        return indicatorRepository.findByIndicatorNameContaining(keyword);
    }
    
    /**
     * 获取指标统计信息
     * @return 统计信息映射
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getIndicatorStatistics() {
        logger.info("获取指标统计信息");
        
        long totalCount = indicatorRepository.count();
        long activeCount = indicatorRepository.countActiveIndicators();
        long coreCount = indicatorRepository.findCoreIndicators().size();
        long extendedCount = indicatorRepository.findExtendedIndicators().size();
        
        return Map.of(
            "totalCount", totalCount,
            "activeCount", activeCount,
            "coreCount", coreCount,
            "extendedCount", extendedCount,
            "inactiveCount", totalCount - activeCount
        );
    }
    
    /**
     * 验证参数配置
     * @param paramConfig 参数配置
     */
    private void validateParamConfig(JsonNode paramConfig) {
        if (paramConfig == null || paramConfig.isEmpty()) {
            throw new IllegalArgumentException("参数配置不能为空");
        }
        
        // 检查必需的字段
        if (!paramConfig.has("configurable_fields")) {
            throw new IllegalArgumentException("参数配置必须包含 configurable_fields 字段");
        }
        
        // 可以添加更多的验证逻辑
        logger.debug("参数配置验证通过");
    }
}
