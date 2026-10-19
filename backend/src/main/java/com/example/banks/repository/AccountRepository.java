package com.example.banks.repository;

import com.example.banks.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 账户数据访问层
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * 根据账户号查找账户
     */
    Optional<Account> findByAccountNo(String accountNo);

    /**
     * 根据身份证号查找账户
     */
    Optional<Account> findByIdCardNo(String idCardNo);

    /**
     * 根据手机号查找账户
     */
    Optional<Account> findByPhone(String phone);

    /**
     * 根据持有人姓名模糊查找账户
     */
    @Query("SELECT a FROM Account a WHERE UPPER(a.holderName) LIKE UPPER(CONCAT('%', :holderName, '%'))")
    List<Account> findByHolderNameContainingIgnoreCase(@Param("holderName") String holderName);

    /**
     * 根据账户类型查找账户
     */
    List<Account> findByAccountType(Short accountType);

    /**
     * 根据账户状态查找账户
     */
    List<Account> findByStatus(Short status);

    /**
     * 查找正常状态的账户
     */
    @Query("SELECT a FROM Account a WHERE a.status = 0")
    List<Account> findNormalAccounts();

    /**
     * 查找可疑账户
     */
    @Query("SELECT a FROM Account a WHERE a.status = 1")
    List<Account> findSuspiciousAccounts();

    /**
     * 查找冻结账户（已废弃，冻结状态现在归为可疑）
     * @deprecated 冻结状态已合并到可疑状态，请使用 findSuspiciousAccounts()
     */
    @Deprecated
    @Query("SELECT a FROM Account a WHERE a.status = 1")
    List<Account> findFrozenAccounts();

    /**
     * 统计账户总数
     */
    @Query("SELECT COUNT(a) FROM Account a")
    long countAllAccounts();

    /**
     * 统计指定类型的账户数量
     */
    @Query("SELECT COUNT(a) FROM Account a WHERE a.accountType = :accountType")
    long countByAccountType(@Param("accountType") Short accountType);

    /**
     * 统计指定状态的账户数量
     */
    @Query("SELECT COUNT(a) FROM Account a WHERE a.status = :status")
    long countByStatus(@Param("status") Short status);
}

