-- ========================================
-- 调试：直接执行指标 SQL 模板
-- 看看能否查询到数据
-- ========================================

-- 先确认数据存在
SELECT '=== 1. 确认基础数据 ===' AS 步骤;
SELECT 
    '转账记录总数' AS 项目, COUNT(*) AS 数量 FROM transfer_records
UNION ALL
SELECT '大额转账(≥5万)', COUNT(*) FROM transfer_records WHERE amount >= 50000
UNION ALL
SELECT '账户16的转账', COUNT(*) FROM transfer_records WHERE from_account_id = 16 OR to_account_id = 16
UNION ALL
SELECT '账户17的转账', COUNT(*) FROM transfer_records WHERE from_account_id = 17 OR to_account_id = 17;

-- ========================================
-- 2. 测试指标A的 SQL 模板（原始的，不加过滤）
-- ========================================
SELECT '=== 2. 测试指标A SQL模板（不加HAVING） ===' AS 步骤;

WITH latest_transaction AS (
    SELECT 
        COALESCE(from_account_id, to_account_id) as account_id, 
        MAX(transfer_time) as last_transaction_time 
    FROM transfer_records 
    GROUP BY COALESCE(from_account_id, to_account_id)
), 
time_window AS (
    SELECT 
        lt.account_id, 
        lt.last_transaction_time, 
        (lt.last_transaction_time - INTERVAL '30 days') as window_start_time 
    FROM latest_transaction lt
) 
SELECT 
    tw.account_id, 
    COUNT(*) as value 
FROM time_window tw 
JOIN transfer_records tr_in ON tw.account_id = tr_in.to_account_id 
JOIN transfer_records tr_out ON tw.account_id = tr_out.from_account_id 
WHERE tr_in.amount >= 50000 
  AND tr_in.transfer_time >= tw.window_start_time 
  AND tr_in.transfer_time <= tw.last_transaction_time 
  AND tr_out.amount >= 50000 
  AND tr_out.transfer_time >= tr_in.transfer_time 
  AND tr_out.transfer_time <= tr_in.transfer_time + INTERVAL '2 minutes' 
GROUP BY tw.account_id
ORDER BY value DESC;

-- ========================================
-- 3. 加上 HAVING 条件（模拟后端逻辑）
-- ========================================
SELECT '=== 3. 加上HAVING条件 ===' AS 步骤;

WITH latest_transaction AS (
    SELECT 
        COALESCE(from_account_id, to_account_id) as account_id, 
        MAX(transfer_time) as last_transaction_time 
    FROM transfer_records 
    GROUP BY COALESCE(from_account_id, to_account_id)
), 
time_window AS (
    SELECT 
        lt.account_id, 
        lt.last_transaction_time, 
        (lt.last_transaction_time - INTERVAL '30 days') as window_start_time 
    FROM latest_transaction lt
) 
SELECT 
    tw.account_id, 
    COUNT(*) as value 
FROM time_window tw 
JOIN transfer_records tr_in ON tw.account_id = tr_in.to_account_id 
JOIN transfer_records tr_out ON tw.account_id = tr_out.from_account_id 
WHERE tr_in.amount >= 50000 
  AND tr_in.transfer_time >= tw.window_start_time 
  AND tr_in.transfer_time <= tw.last_transaction_time 
  AND tr_out.amount >= 50000 
  AND tr_out.transfer_time >= tr_in.transfer_time 
  AND tr_out.transfer_time <= tr_in.transfer_time + INTERVAL '2 minutes' 
GROUP BY tw.account_id
HAVING COUNT(*) >= 2  -- 这是后端添加的条件
ORDER BY value DESC;

-- ========================================
-- 4. 检查 latest_transaction 的结果
-- ========================================
SELECT '=== 4. 检查 latest_transaction CTE ===' AS 步骤;

WITH latest_transaction AS (
    SELECT 
        COALESCE(from_account_id, to_account_id) as account_id, 
        MAX(transfer_time) as last_transaction_time 
    FROM transfer_records 
    GROUP BY COALESCE(from_account_id, to_account_id)
)
SELECT 
    account_id,
    last_transaction_time,
    (last_transaction_time - INTERVAL '30 days') as window_start_time
FROM latest_transaction
WHERE account_id IN (16, 17, 18, 19, 20)
ORDER BY account_id;

-- ========================================
-- 5. 检查 COALESCE 的问题
-- ========================================
SELECT '=== 5. COALESCE 问题检查 ===' AS 步骤;

SELECT 
    '说明：COALESCE(from_account_id, to_account_id) 会导致每条记录都有值' AS 说明,
    '问题：无法区分转入和转出，导致时间窗口计算错误' AS 问题;

SELECT 
    transfer_no,
    from_account_id,
    to_account_id,
    COALESCE(from_account_id, to_account_id) as coalesce_result,
    amount,
    transfer_time
FROM transfer_records
WHERE from_account_id = 16 OR to_account_id = 16
ORDER BY transfer_time
LIMIT 10;

-- ========================================
-- 6. 正确的查询方式（不使用 COALESCE）
-- ========================================
SELECT '=== 6. 正确的指标A查询（不使用COALESCE） ===' AS 步骤;

-- 方法1：直接查询，不用复杂的时间窗口
SELECT 
    tr_in.to_account_id AS account_id,
    a.holder_name,
    COUNT(*) as 快进快出次数
FROM transfer_records tr_in
JOIN transfer_records tr_out ON tr_in.to_account_id = tr_out.from_account_id
JOIN account a ON tr_in.to_account_id = a.id
WHERE tr_in.amount >= 50000
  AND tr_out.amount >= 50000
  AND tr_out.transfer_time >= tr_in.transfer_time
  AND tr_out.transfer_time <= tr_in.transfer_time + INTERVAL '2 minutes'
GROUP BY tr_in.to_account_id, a.holder_name
HAVING COUNT(*) >= 2
ORDER BY 快进快出次数 DESC;

-- ========================================
-- 7. 测试指标B
-- ========================================
SELECT '=== 7. 测试指标B（登录后转账） ===' AS 步骤;

SELECT 
    lr.account_id,
    a.holder_name,
    COUNT(*) as 登录后转账次数
FROM login_record lr
JOIN transfer_records tr ON lr.account_id = tr.from_account_id
JOIN account a ON lr.account_id = a.id
WHERE tr.amount >= 50000
  AND tr.transfer_time >= lr.login_time
  AND tr.transfer_time <= lr.login_time + INTERVAL '5 minutes'
GROUP BY lr.account_id, a.holder_name
HAVING COUNT(*) >= 2
ORDER BY 登录后转账次数 DESC;

-- ========================================
-- 8. 测试指标C
-- ========================================
SELECT '=== 8. 测试指标C（闲置账户） ===' AS 步骤;

SELECT 
    a.id AS account_id,
    a.holder_name,
    COALESCE(SUM(tr_in.amount), 0) AS 入账总额,
    COALESCE(SUM(tr_out.amount), 0) AS 转出总额
FROM account a
LEFT JOIN transfer_records tr_in ON a.id = tr_in.to_account_id
LEFT JOIN transfer_records tr_out ON a.id = tr_out.from_account_id
WHERE a.id BETWEEN 18 AND 20
GROUP BY a.id, a.holder_name
HAVING COALESCE(SUM(tr_out.amount), 0) = 0
ORDER BY a.id;
