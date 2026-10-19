package com.example.banks.repository;

import com.example.banks.entity.LoginRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录记录数据访问层
 */
@Repository
public interface LoginRecordRepository extends JpaRepository<LoginRecord, Long> {

    /**
     * 根据账户ID查找登录记录
     */
    @Query("SELECT lr FROM LoginRecord lr WHERE lr.accountId = :accountId ORDER BY lr.loginTime DESC")
    List<LoginRecord> findByAccountId(@Param("accountId") Long accountId);

    /**
     * 根据登录IP查找登录记录
     */
    @Query("SELECT lr FROM LoginRecord lr WHERE lr.loginIp = :loginIp ORDER BY lr.loginTime DESC")
    List<LoginRecord> findByLoginIp(@Param("loginIp") String loginIp);


    /**
     * 根据时间范围查找登录记录
     */
    @Query("SELECT lr FROM LoginRecord lr WHERE lr.loginTime >= :startTime AND lr.loginTime <= :endTime ORDER BY lr.loginTime DESC")
    List<LoginRecord> findByLoginTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询所有登录记录
     */
    @Query("SELECT lr FROM LoginRecord lr ORDER BY lr.loginTime DESC")
    Page<LoginRecord> findAllOrderByLoginTimeDesc(Pageable pageable);

    /**
     * 查找指定账户在指定时间范围内的登录记录
     */
    @Query("SELECT lr FROM LoginRecord lr WHERE lr.accountId = :accountId AND lr.loginTime >= :startTime AND lr.loginTime <= :endTime ORDER BY lr.loginTime DESC")
    List<LoginRecord> findLoginsByAccountAndTime(@Param("accountId") Long accountId,
                                                 @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计总登录记录数
     */
    @Query("SELECT COUNT(lr) FROM LoginRecord lr")
    long countAllLogins();


    /**
     * 统计指定账户的登录次数
     */
    @Query("SELECT COUNT(lr) FROM LoginRecord lr WHERE lr.accountId = :accountId")
    long countByAccountId(@Param("accountId") Long accountId);

    /**
     * 统计指定IP的登录次数
     */
    @Query("SELECT COUNT(lr) FROM LoginRecord lr WHERE lr.loginIp = :loginIp")
    long countByLoginIp(@Param("loginIp") String loginIp);

    /**
     * 查找指定时间范围内的登录记录数量
     */
    @Query("SELECT COUNT(lr) FROM LoginRecord lr WHERE lr.loginTime >= :startTime AND lr.loginTime <= :endTime")
    long countByLoginTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计独立IP数量
     */
    @Query("SELECT COUNT(DISTINCT lr.loginIp) FROM LoginRecord lr")
    long countDistinctLoginIps();

    /**
     * 获取最近的登录记录
     */
    @Query("SELECT lr FROM LoginRecord lr ORDER BY lr.loginTime DESC")
    List<LoginRecord> findRecentLogins(Pageable pageable);

    /**
     * 查找异常地区登录（同一地区多个账户登录）
     */
    @Query("SELECT lr.loginIp, COUNT(DISTINCT lr.accountId) as accountCount FROM LoginRecord lr GROUP BY lr.loginIp HAVING COUNT(DISTINCT lr.accountId) > :threshold ORDER BY accountCount DESC")
    List<Object[]> findSuspiciousLocations(@Param("threshold") int threshold);

    /**
     * 根据账户名搜索登录记录（精确匹配账户名，模糊匹配IP，使用原生SQL）
     */
    @Query(value = "SELECT lr.* FROM login_record lr LEFT JOIN account a ON lr.account_id = a.id " +
            "WHERE (COALESCE(:accountName, '') = '' OR a.holder_name = :accountName) " +
            "AND (COALESCE(:loginIp, '') = '' OR lr.login_ip ILIKE '%' || :loginIp || '%') " +
            "AND (CAST(:startTime AS timestamp) IS NULL OR lr.login_time >= CAST(:startTime AS timestamp)) " +
            "AND (CAST(:endTime AS timestamp) IS NULL OR lr.login_time <= CAST(:endTime AS timestamp)) " +
            "ORDER BY lr.login_time DESC",
            nativeQuery = true)
    Page<LoginRecord> searchLoginRecords(@Param("accountName") String accountName,
                                         @Param("loginIp") String loginIp,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime,
                                         Pageable pageable);
}

