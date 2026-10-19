package com.example.banks.repository;

import com.example.banks.entity.AnalysisTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 分析任务数据访问层
 * 提供任务管理的数据库操作接口
 */
@Repository
public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, Long> {
    
    /**
     * 根据任务代码查找任务
     * @param taskCode 任务代码
     * @return 任务实体
     */
    Optional<AnalysisTask> findByTaskCode(String taskCode);
    
    /**
     * 检查任务代码是否存在
     * @param taskCode 任务代码
     * @return 是否存在
     */
    boolean existsByTaskCode(String taskCode);
    
    /**
     * 查找所有任务，按创建时间倒序
     * @return 任务列表
     */
    @Query("SELECT at FROM AnalysisTask at ORDER BY at.createTime DESC")
    List<AnalysisTask> findAllOrderByCreateTimeDesc();
    
    /**
     * 根据状态查找任务
     * @param status 任务状态
     * @return 任务列表
     */
    @Query("SELECT at FROM AnalysisTask at WHERE at.taskStatus = :status ORDER BY at.createTime DESC")
    List<AnalysisTask> findByTaskStatus(@Param("status") Short status);
    
    /**
     * 查找正在执行的任务
     * @return 正在执行的任务列表
     */
    @Query("SELECT at FROM AnalysisTask at WHERE at.taskStatus = 1 ORDER BY at.startTime")
    List<AnalysisTask> findRunningTasks();
    
    /**
     * 查找待执行的任务
     * @return 待执行的任务列表
     */
    @Query("SELECT at FROM AnalysisTask at WHERE at.taskStatus = 0 ORDER BY at.createTime")
    List<AnalysisTask> findPendingTasks();
    
    /**
     * 查找指定时间范围内的任务
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务列表
     */
    @Query("SELECT at FROM AnalysisTask at WHERE at.createTime >= :startTime AND at.createTime <= :endTime ORDER BY at.createTime DESC")
    List<AnalysisTask> findByCreateTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据任务名称模糊查询
     * @param taskName 任务名称关键字
     * @return 匹配的任务列表
     */
    @Query("SELECT at FROM AnalysisTask at WHERE at.taskName LIKE %:taskName% ORDER BY at.createTime DESC")
    List<AnalysisTask> findByTaskNameContaining(@Param("taskName") String taskName);
    
    /**
     * 统计各状态任务数量
     * @return 统计结果
     */
    @Query("SELECT at.taskStatus, COUNT(at) FROM AnalysisTask at GROUP BY at.taskStatus")
    List<Object[]> countByTaskStatus();
    
    /**
     * 统计今日创建的任务数量
     * @param today 今日开始时间
     * @return 任务数量
     */
    @Query("SELECT COUNT(at) FROM AnalysisTask at WHERE at.createTime >= :today")
    long countTodayTasks(@Param("today") LocalDateTime today);
    
    /**
     * 查找最近N个已完成的任务
     * @param limit 限制数量
     * @return 已完成任务列表
     */
    @Query("SELECT at FROM AnalysisTask at WHERE at.taskStatus = 2 ORDER BY at.endTime DESC LIMIT :limit")
    List<AnalysisTask> findRecentCompletedTasks(@Param("limit") int limit);
}
