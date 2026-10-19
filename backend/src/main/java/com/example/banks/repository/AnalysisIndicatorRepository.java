package com.example.banks.repository;

import com.example.banks.entity.AnalysisIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 分析指标数据访问层
 * 提供指标配置的数据库操作接口
 */
@Repository
public interface AnalysisIndicatorRepository extends JpaRepository<AnalysisIndicator, Long> {
    
    /**
     * 根据指标代码查找指标
     * @param indicatorCode 指标代码
     * @return 指标实体
     */
    Optional<AnalysisIndicator> findByIndicatorCode(String indicatorCode);
    
    /**
     * 查找所有激活的指标
     * @return 激活的指标列表
     */
    @Query("SELECT ai FROM AnalysisIndicator ai WHERE ai.isActive = 1 ORDER BY ai.isCore DESC, ai.indicatorCode")
    List<AnalysisIndicator> findAllActiveIndicators();
    
    /**
     * 查找核心指标
     * @return 核心指标列表
     */
    @Query("SELECT ai FROM AnalysisIndicator ai WHERE ai.isCore = 1 AND ai.isActive = 1 ORDER BY ai.indicatorCode")
    List<AnalysisIndicator> findCoreIndicators();
    
    /**
     * 查找扩展指标
     * @return 扩展指标列表
     */
    @Query("SELECT ai FROM AnalysisIndicator ai WHERE ai.isCore = 0 AND ai.isActive = 1 ORDER BY ai.indicatorCode")
    List<AnalysisIndicator> findExtendedIndicators();
    
    /**
     * 根据指标代码列表查找指标
     * @param indicatorCodes 指标代码列表
     * @return 指标列表
     */
    @Query("SELECT ai FROM AnalysisIndicator ai WHERE ai.indicatorCode IN :indicatorCodes AND ai.isActive = 1")
    List<AnalysisIndicator> findByIndicatorCodes(@Param("indicatorCodes") List<String> indicatorCodes);
    
    /**
     * 检查指标代码是否存在
     * @param indicatorCode 指标代码
     * @return 是否存在
     */
    boolean existsByIndicatorCode(String indicatorCode);
    
    /**
     * 统计激活的指标数量
     * @return 指标数量
     */
    @Query("SELECT COUNT(ai) FROM AnalysisIndicator ai WHERE ai.isActive = 1")
    long countActiveIndicators();
    
    /**
     * 根据指标名称模糊查询
     * @param name 指标名称关键字
     * @return 匹配的指标列表
     */
    @Query("SELECT ai FROM AnalysisIndicator ai WHERE ai.indicatorName LIKE %:name% AND ai.isActive = 1")
    List<AnalysisIndicator> findByIndicatorNameContaining(@Param("name") String name);
}
