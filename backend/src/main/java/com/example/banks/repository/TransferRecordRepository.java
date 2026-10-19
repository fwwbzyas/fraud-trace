package com.example.banks.repository;

import com.example.banks.entity.TransferRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 转账记录数据访问层 - 重新实现
 */
@Repository
public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {

    /**
     * 根据流水号查找转账记录
     */
    Optional<TransferRecord> findByTransferNo(String transferNo);

    /**
     * 查找指定账户的所有转出记录
     */
    List<TransferRecord> findByFromAccountIdOrderByTransferTimeDesc(Long fromAccountId);

    /**
     * 查找指定账户的所有转入记录
     */
    List<TransferRecord> findByToAccountIdOrderByTransferTimeDesc(Long toAccountId);

    /**
     * 查找指定账户相关的所有转账记录（转入+转出）
     */
    @Query("SELECT tr FROM TransferRecord tr WHERE tr.fromAccountId = :accountId OR tr.toAccountId = :accountId ORDER BY tr.transferTime DESC")
    List<TransferRecord> findByAccountId(@Param("accountId") Long accountId);

    /**
     * 查找大额转账记录（金额>=50000）
     */
    @Query("SELECT tr FROM TransferRecord tr WHERE tr.amount >= 50000 ORDER BY tr.transferTime DESC")
    List<TransferRecord> findLargeAmountTransfers();

    /**
     * 根据时间范围查找转账记录
     */
    List<TransferRecord> findByTransferTimeBetweenOrderByTransferTimeDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分页查询所有转账记录并包含账户信息
     */
    @Query(value = """
        SELECT 
            tr.transfer_id, 
            tr.from_account_id, 
            tr.to_account_id, 
            tr.transfer_no, 
            tr.amount, 
            tr.transfer_time, 
            COALESCE(fa.holder_name, 'Unknown') as from_account_name, 
            COALESCE(fa.phone, '') as from_account_phone, 
            COALESCE(ta.holder_name, 'Unknown') as to_account_name, 
            COALESCE(ta.phone, '') as to_account_phone 
        FROM transfer_records tr 
        LEFT JOIN account fa ON tr.from_account_id = fa.id 
        LEFT JOIN account ta ON tr.to_account_id = ta.id 
        ORDER BY tr.transfer_time DESC
        """, nativeQuery = true)
    Page<Object[]> findAllWithAccountInfo(Pageable pageable);

    /**
     * 简化的搜索查询 - 只按流水号搜索
     */
    @Query(value = """
        SELECT 
            tr.transfer_id, 
            tr.from_account_id, 
            tr.to_account_id, 
            tr.transfer_no, 
            tr.amount, 
            tr.transfer_time, 
            COALESCE(fa.holder_name, 'Unknown') as from_account_name, 
            COALESCE(fa.phone, '') as from_account_phone, 
            COALESCE(ta.holder_name, 'Unknown') as to_account_name, 
            COALESCE(ta.phone, '') as to_account_phone 
        FROM transfer_records tr 
        LEFT JOIN account fa ON tr.from_account_id = fa.id 
        LEFT JOIN account ta ON tr.to_account_id = ta.id 
        WHERE tr.transfer_no ILIKE CONCAT('%', :transferNo, '%')
        ORDER BY tr.transfer_time DESC
        """, nativeQuery = true)
    Page<Object[]> searchByTransferNo(@Param("transferNo") String transferNo, Pageable pageable);

    /**
     * 按转出账户名搜索
     */
    @Query(value = """
        SELECT 
            tr.transfer_id, 
            tr.from_account_id, 
            tr.to_account_id, 
            tr.transfer_no, 
            tr.amount, 
            tr.transfer_time, 
            COALESCE(fa.holder_name, 'Unknown') as from_account_name, 
            COALESCE(fa.phone, '') as from_account_phone, 
            COALESCE(ta.holder_name, 'Unknown') as to_account_name, 
            COALESCE(ta.phone, '') as to_account_phone 
        FROM transfer_records tr 
        LEFT JOIN account fa ON tr.from_account_id = fa.id 
        LEFT JOIN account ta ON tr.to_account_id = ta.id 
        WHERE fa.holder_name ILIKE CONCAT('%', :fromAccountName, '%')
        ORDER BY tr.transfer_time DESC
        """, nativeQuery = true)
    Page<Object[]> searchByFromAccountName(@Param("fromAccountName") String fromAccountName, Pageable pageable);

    /**
     * 按转入账户名搜索
     */
    @Query(value = """
        SELECT 
            tr.transfer_id, 
            tr.from_account_id, 
            tr.to_account_id, 
            tr.transfer_no, 
            tr.amount, 
            tr.transfer_time, 
            COALESCE(fa.holder_name, 'Unknown') as from_account_name, 
            COALESCE(fa.phone, '') as from_account_phone, 
            COALESCE(ta.holder_name, 'Unknown') as to_account_name, 
            COALESCE(ta.phone, '') as to_account_phone 
        FROM transfer_records tr 
        LEFT JOIN account fa ON tr.from_account_id = fa.id 
        LEFT JOIN account ta ON tr.to_account_id = ta.id 
        WHERE ta.holder_name ILIKE CONCAT('%', :toAccountName, '%')
        ORDER BY tr.transfer_time DESC
        """, nativeQuery = true)
    Page<Object[]> searchByToAccountName(@Param("toAccountName") String toAccountName, Pageable pageable);

    /**
     * 按转出和转入账户名组合搜索
     */
    @Query(value = """
        SELECT 
            tr.transfer_id, 
            tr.from_account_id, 
            tr.to_account_id, 
            tr.transfer_no, 
            tr.amount, 
            tr.transfer_time, 
            COALESCE(fa.holder_name, 'Unknown') as from_account_name, 
            COALESCE(fa.phone, '') as from_account_phone, 
            COALESCE(ta.holder_name, 'Unknown') as to_account_name, 
            COALESCE(ta.phone, '') as to_account_phone 
        FROM transfer_records tr 
        LEFT JOIN account fa ON tr.from_account_id = fa.id 
        LEFT JOIN account ta ON tr.to_account_id = ta.id 
        WHERE fa.holder_name ILIKE CONCAT('%', :fromAccountName, '%')
          AND ta.holder_name ILIKE CONCAT('%', :toAccountName, '%')
        ORDER BY tr.transfer_time DESC
        """, nativeQuery = true)
    Page<Object[]> searchByFromAndToAccountName(@Param("fromAccountName") String fromAccountName, 
                                                @Param("toAccountName") String toAccountName, 
                                                Pageable pageable);

    /**
     * 按金额范围搜索
     */
    @Query(value = """
        SELECT 
            tr.transfer_id, 
            tr.from_account_id, 
            tr.to_account_id, 
            tr.transfer_no, 
            tr.amount, 
            tr.transfer_time, 
            COALESCE(fa.holder_name, 'Unknown') as from_account_name, 
            COALESCE(fa.phone, '') as from_account_phone, 
            COALESCE(ta.holder_name, 'Unknown') as to_account_name, 
            COALESCE(ta.phone, '') as to_account_phone 
        FROM transfer_records tr 
        LEFT JOIN account fa ON tr.from_account_id = fa.id 
        LEFT JOIN account ta ON tr.to_account_id = ta.id 
        WHERE tr.amount >= :minAmount AND tr.amount <= :maxAmount
        ORDER BY tr.transfer_time DESC
        """, nativeQuery = true)
    Page<Object[]> searchByAmountRange(@Param("minAmount") BigDecimal minAmount, 
                                       @Param("maxAmount") BigDecimal maxAmount, 
                                       Pageable pageable);

    /**
     * 按时间范围搜索
     */
    @Query(value = """
        SELECT 
            tr.transfer_id, 
            tr.from_account_id, 
            tr.to_account_id, 
            tr.transfer_no, 
            tr.amount, 
            tr.transfer_time, 
            COALESCE(fa.holder_name, 'Unknown') as from_account_name, 
            COALESCE(fa.phone, '') as from_account_phone, 
            COALESCE(ta.holder_name, 'Unknown') as to_account_name, 
            COALESCE(ta.phone, '') as to_account_phone 
        FROM transfer_records tr 
        LEFT JOIN account fa ON tr.from_account_id = fa.id 
        LEFT JOIN account ta ON tr.to_account_id = ta.id 
        WHERE tr.transfer_time >= :startTime AND tr.transfer_time <= :endTime
        ORDER BY tr.transfer_time DESC
        """, nativeQuery = true)
    Page<Object[]> searchByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime, 
                                     Pageable pageable);

    /**
     * 大额交易搜索
     */
    @Query(value = """
        SELECT 
            tr.transfer_id, 
            tr.from_account_id, 
            tr.to_account_id, 
            tr.transfer_no, 
            tr.amount, 
            tr.transfer_time, 
            COALESCE(fa.holder_name, 'Unknown') as from_account_name, 
            COALESCE(fa.phone, '') as from_account_phone, 
            COALESCE(ta.holder_name, 'Unknown') as to_account_name, 
            COALESCE(ta.phone, '') as to_account_phone 
        FROM transfer_records tr 
        LEFT JOIN account fa ON tr.from_account_id = fa.id 
        LEFT JOIN account ta ON tr.to_account_id = ta.id 
        WHERE tr.amount >= 50000
        ORDER BY tr.transfer_time DESC
        """, nativeQuery = true)
    Page<Object[]> searchLargeAmountTransfers(Pageable pageable);

    /**
     * 获取转账统计信息 - 简化版本
     */
    @Query(value = """
        SELECT 
            CAST(COUNT(tr.transfer_id) AS BIGINT) as total_count,
            CAST(COALESCE(SUM(tr.amount), 0) AS DECIMAL) as total_amount,
            CAST(COUNT(CASE WHEN tr.amount >= 50000 THEN 1 END) AS BIGINT) as large_amount_count,
            CAST(COUNT(CASE WHEN DATE(tr.transfer_time) = CURRENT_DATE THEN 1 END) AS BIGINT) as today_count,
            CAST(COALESCE(SUM(CASE WHEN DATE(tr.transfer_time) = CURRENT_DATE THEN tr.amount ELSE 0 END), 0) AS DECIMAL) as today_amount
        FROM transfer_records tr
        """, nativeQuery = true)
    Object[] getTransferStatistics();
    
    /**
     * 获取基础统计信息 - 备用方法
     */
    @Query(value = "SELECT COUNT(*) FROM transfer_records", nativeQuery = true)
    Long getTotalCount();
    
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM transfer_records", nativeQuery = true)
    BigDecimal getTotalAmount();
    
    @Query(value = "SELECT COUNT(*) FROM transfer_records WHERE amount >= 50000", nativeQuery = true)
    Long getLargeAmountCount();
    
    @Query(value = "SELECT COUNT(*) FROM transfer_records WHERE DATE(transfer_time) = CURRENT_DATE", nativeQuery = true)
    Long getTodayCount();
    
    /**
     * 获取指定账户的转账统计信息
     */
    @Query(value = """
        SELECT 
            COUNT(tr.transfer_id) as total_count,
            COALESCE(SUM(tr.amount), 0) as total_amount
        FROM transfer_records tr 
        WHERE tr.from_account_id = :accountId OR tr.to_account_id = :accountId
        """, nativeQuery = true)
    Object[] getAccountTransferStats(@Param("accountId") Long accountId);
}