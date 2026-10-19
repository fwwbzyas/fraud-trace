package com.example.banks.repository;

import com.example.banks.entity.RiskResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 风险结果数据访问层
 * 提供风险分析结果的数据库操作接口
 */
@Repository
public interface RiskResultRepository extends JpaRepository<RiskResult, Long> {

    /**
     * 根据任务ID查找所有风险结果
     * @param taskId 任务ID
     * @return 风险结果列表
     */
    @Query("SELECT rr FROM RiskResult rr WHERE rr.taskId = :taskId ORDER BY rr.accountRole, rr.createTime")
    List<RiskResult> findByTaskId(@Param("taskId") Long taskId);

    /**
     * 根据任务ID和账户角色查找风险结果
     * @param taskId 任务ID
     * @param accountRole 账户角色 (1=付款方/受害人，2=收款方/可疑账户)
     * @return 风险结果列表
     */
    @Query("SELECT rr FROM RiskResult rr WHERE rr.taskId = :taskId AND rr.accountRole = :accountRole ORDER BY rr.createTime")
    List<RiskResult> findByTaskIdAndAccountRole(@Param("taskId") Long taskId,
                                                @Param("accountRole") Short accountRole);

    /**
     * 查找指定任务的受害人账户
     * @param taskId 任务ID
     * @return 受害人风险结果列表
     */
    @Query("SELECT rr FROM RiskResult rr WHERE rr.taskId = :taskId AND rr.accountRole = 1 ORDER BY rr.createTime")
    List<RiskResult> findVictimsByTaskId(@Param("taskId") Long taskId);

    /**
     * 查找指定任务的可疑账户
     * @param taskId 任务ID
     * @return 可疑账户风险结果列表
     */
    @Query("SELECT rr FROM RiskResult rr WHERE rr.taskId = :taskId AND rr.accountRole = 2 ORDER BY rr.createTime")
    List<RiskResult> findSuspiciousAccountsByTaskId(@Param("taskId") Long taskId);

    /**
     * 统计指定任务的风险结果数量
     * @param taskId 任务ID
     * @return 风险结果数量
     */
    @Query("SELECT COUNT(rr) FROM RiskResult rr WHERE rr.taskId = :taskId")
    long countByTaskId(@Param("taskId") Long taskId);

    /**
     * 统计指定任务的受害人数量
     * @param taskId 任务ID
     * @return 受害人数量
     */
    @Query("SELECT COUNT(rr) FROM RiskResult rr WHERE rr.taskId = :taskId AND rr.accountRole = 1")
    long countVictimsByTaskId(@Param("taskId") Long taskId);

    /**
     * 统计指定任务的可疑账户数量
     * @param taskId 任务ID
     * @return 可疑账户数量
     */
    @Query("SELECT COUNT(rr) FROM RiskResult rr WHERE rr.taskId = :taskId AND rr.accountRole = 2")
    long countSuspiciousAccountsByTaskId(@Param("taskId") Long taskId);


    /**
     * 查找包含账户详细信息的风险结果（连接查询）
     * @param taskId 任务ID
     * @return 包含账户信息的风险结果列表
     */
    @Query("SELECT rr, a FROM RiskResult rr LEFT JOIN Account a ON rr.accountId = a.id WHERE rr.taskId = :taskId ORDER BY rr.accountRole, rr.createTime")
    List<Object[]> findByTaskIdWithAccountInfo(@Param("taskId") Long taskId);

    /**
     * 分页查找包含账户详细信息的风险结果（使用原生SQL，支持角色过滤）
     * @param taskId 任务ID
     * @param offset 偏移量
     * @param limit 查询数量
     * @param accountRole 账户角色（可选）
     * @return 包含账户信息的风险结果列表
     */
    @Query(value = "SELECT rr.risk_id, rr.task_id, rr.account_id, rr.account_role, rr.matched_indicators, rr.matched_rules, rr.create_time, " +
            "a.id, a.account_no, a.holder_name, a.id_card_no, a.phone, a.account_type, a.status " +
            "FROM risk_results rr LEFT JOIN account a ON rr.account_id = a.id " +
            "WHERE rr.task_id = ?1 AND (?4 IS NULL OR rr.account_role = ?4) " +
            "ORDER BY rr.account_role, rr.create_time LIMIT ?3 OFFSET ?2",
            nativeQuery = true)
    List<Object[]> findByTaskIdWithAccountInfoPaged(@Param("taskId") Long taskId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit,
                                                    @Param("accountRole") Short accountRole);

    /**
     * 按任务ID和账户角色统计数量
     * @param taskId 任务ID
     * @param accountRole 账户角色
     * @return 数量
     */
    @Query("SELECT COUNT(rr) FROM RiskResult rr WHERE rr.taskId = :taskId AND rr.accountRole = :accountRole")
    long countByTaskIdAndAccountRole(@Param("taskId") Long taskId, @Param("accountRole") Short accountRole);

    /**
     * 删除指定任务的所有风险结果
     * @param taskId 任务ID
     */
    @Modifying
    @Query("DELETE FROM RiskResult rr WHERE rr.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);
}
