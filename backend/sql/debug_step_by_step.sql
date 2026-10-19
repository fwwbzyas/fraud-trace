-- ========================================
-- 逐步调试SQL执行过程
-- 找出为什么连 A>0 C>0 都查不出数据
-- ========================================

-- 第一步：确认基础数据存在
SELECT '=== 第1步：确认基础数据 ===' AS 步骤;

SELECT 
    '账户总数' AS 项目, COUNT(*) AS 数量 FROM account
UNION ALL
SELECT '转账记录总数', COUNT(*) FROM transfer_records
UNION ALL
SELECT '登录记录总数', COUNT(*) FROM login_record
UNION ALL
SELECT '大额转账(≥5万)', COUNT(*) FROM transfer_records WHERE amount >= 50000;

-- 第二步：测试 COALESCE 逻辑
SELECT '=== 第2步：测试COALESCE逻辑 ===' AS 步骤;

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
    '这是每个账户的最后交易时间' AS 说明
FROM latest_transaction
ORDER BY account_id
LIMIT 10;

-- 第三步：测试时间窗口计算
SELECT '=== 第3步：测试时间窗口计算 ===' AS 步骤;

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
    account_id,
    last_transaction_time,
    window_start_time,
    '时间窗口：' || window_start_time || ' 到 ' || last_transaction_time AS 时间窗口
FROM time_window
ORDER BY account_id
LIMIT 10;

-- 第四步：测试指标A的JOIN条件
SELECT '=== 第4步：测试指标A的JOIN ===' AS 步骤;

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
    COUNT(*) as 匹配的转账对数,
    '这是JOIN后的结果数量' AS 说明
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
ORDER BY tw.account_id;

-- 第五步：简化版指标A测试（不用时间窗口）
SELECT '=== 第5步：简化版指标A测试 ===' AS 步骤;

SELECT 
    tr_in.to_account_id AS account_id,
    COUNT(*) as 快进快出次数,
    '简化版查询结果' AS 说明
FROM transfer_records tr_in
JOIN transfer_records tr_out ON tr_in.to_account_id = tr_out.from_account_id
WHERE tr_in.amount >= 50000
  AND tr_out.amount >= 50000
  AND tr_out.transfer_time >= tr_in.transfer_time
  AND tr_out.transfer_time <= tr_in.transfer_time + INTERVAL '2 minutes'
GROUP BY tr_in.to_account_id
ORDER BY tr_in.to_account_id;

-- 第六步：检查具体的转账数据
SELECT '=== 第6步：检查具体转账数据 ===' AS 步骤;

SELECT 
    tr.transfer_no,
    tr.from_account_id,
    tr.to_account_id,
    tr.amount,
    tr.transfer_time,
    CASE 
        WHEN tr.amount >= 50000 THEN '✅大额'
        ELSE '❌小额'
    END as 金额类型
FROM transfer_records tr
WHERE tr.amount >= 50000
ORDER BY tr.transfer_time
LIMIT 20;

-- 第七步：检查是否有快进快出的数据
SELECT '=== 第7步：检查快进快出数据 ===' AS 步骤;

SELECT 
    tr_in.to_account_id,
    tr_in.transfer_time as 入账时间,
    tr_in.amount as 入账金额,
    tr_out.transfer_time as 转出时间,
    tr_out.amount as 转出金额,
    EXTRACT(EPOCH FROM (tr_out.transfer_time - tr_in.transfer_time))/60 as 时间差_分钟
FROM transfer_records tr_in
JOIN transfer_records tr_out ON tr_in.to_account_id = tr_out.from_account_id
WHERE tr_in.amount >= 50000
  AND tr_out.amount >= 50000
  AND tr_out.transfer_time >= tr_in.transfer_time
  AND tr_out.transfer_time <= tr_in.transfer_time + INTERVAL '10 minutes'  -- 放宽到10分钟
ORDER BY tr_in.to_account_id, tr_in.transfer_time;

-- 第八步：检查登录后转账数据
SELECT '=== 第8步：检查登录后转账数据 ===' AS 步骤;

SELECT 
    lr.account_id,
    lr.login_time,
    tr.transfer_time,
    tr.amount,
    EXTRACT(EPOCH FROM (tr.transfer_time - lr.login_time))/60 as 时间差_分钟
FROM login_record lr
JOIN transfer_records tr ON lr.account_id = tr.from_account_id
WHERE tr.amount >= 50000
  AND tr.transfer_time >= lr.login_time
  AND tr.transfer_time <= lr.login_time + INTERVAL '10 minutes'  -- 放宽到10分钟
ORDER BY lr.account_id, lr.login_time;

-- 第九步：检查账户入账情况
SELECT '=== 第9步：检查账户入账情况 ===' AS 步骤;

SELECT 
    tr.to_account_id as account_id,
    COUNT(*) as 入账次数,
    SUM(tr.amount) as 入账总额,
    MAX(tr.amount) as 最大单笔,
    MIN(tr.amount) as 最小单笔
FROM transfer_records tr
GROUP BY tr.to_account_id
HAVING SUM(tr.amount) > 0
ORDER BY 入账总额 DESC
LIMIT 10;

-- 第十步：完整的原始SQL模板测试
SELECT '=== 第10步：完整原始SQL模板测试 ===' AS 步骤;

-- 测试指标A的完整SQL
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
    COUNT(*) as value,
    '指标A完整测试' as 说明
FROM time_window tw 
JOIN transfer_records tr_in ON tw.account_id = tr_in.to_account_id 
JOIN transfer_records tr_out ON tw.account_id = tr_out.from_account_id 
WHERE tr_in.amount >= 50000 
  AND tr_in.transfer_time >= tw.window_start_time 
  AND tr_in.transfer_time <= tw.last_transaction_time 
  AND tr_out.amount >= 50000 
  AND tr_out.transfer_time >= tr_in.transfer_time 
  AND tr_out.transfer_time <= tr_in.transfer_time + INTERVAL '2 minutes' 
GROUP BY tw.account_id;

-- 诊断总结
SELECT '=== 诊断总结 ===' AS 步骤;
SELECT '如果以上步骤都没有数据，说明：' AS 诊断
UNION ALL
SELECT '1. 数据本身有问题（没有大额转账或时间不匹配）' AS 诊断
UNION ALL
SELECT '2. SQL模板逻辑有问题（JOIN条件或时间窗口计算错误）' AS 诊断
UNION ALL
SELECT '3. 参数替换有问题（模板参数没有正确替换）' AS 诊断;
