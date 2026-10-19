-- ========================================
-- 插入新的风险分析指标（INDICATOR_D/E/F）
-- ========================================

-- 指标D：夜间登录次数
INSERT INTO "analysis_indicators" 
("indicator_id", "indicator_code", "indicator_name", "indicator_desc", "param_config", "is_core", "is_active", "create_time", "update_time", "user_type") 
VALUES (
  4, 
  'INDICATOR_D', 
  '夜间登录次数', 
  '统计账户在夜间时段（22:00-06:00）的登录次数，用于识别异常登录行为', 
  '{
    "user_type": "all", 
    "description": "检测异常登录时间模式，夜间登录可能表示账户被盗用", 
    "sql_template": "WITH latest_transaction AS (SELECT COALESCE(from_account_id, to_account_id) as account_id, MAX(transfer_time) as last_transaction_time FROM transfer_records GROUP BY COALESCE(from_account_id, to_account_id)), time_window AS (SELECT lt.account_id, lt.last_transaction_time, (lt.last_transaction_time - INTERVAL ''{time_window_days} days'') as window_start_time FROM latest_transaction lt) SELECT tw.account_id, COUNT(*) as value FROM time_window tw JOIN login_record rl ON tw.account_id = rl.account_id WHERE rl.login_time >= tw.window_start_time AND rl.login_time <= tw.last_transaction_time AND (EXTRACT(HOUR FROM rl.login_time) >= {night_start_hour} OR EXTRACT(HOUR FROM rl.login_time) < {night_end_hour}) GROUP BY tw.account_id", 
    "night_start_hour": 22,
    "night_end_hour": 6,
    "configurable_fields": [
      {
        "max": 23, 
        "min": 18, 
        "type": "integer", 
        "field": "night_start_hour", 
        "label": "夜间开始时间（小时）", 
        "default": 22
      },
      {
        "max": 10,
        "min": 4,
        "type": "integer",
        "field": "night_end_hour",
        "label": "夜间结束时间（小时）",
        "default": 6
      }
    ]
  }', 
  1, 
  1, 
  CURRENT_TIMESTAMP, 
  CURRENT_TIMESTAMP, 
  'all'
);

-- 指标E：团伙作案次数
INSERT INTO "analysis_indicators" 
("indicator_id", "indicator_code", "indicator_name", "indicator_desc", "param_config", "is_core", "is_active", "create_time", "update_time", "user_type") 
VALUES (
  5, 
  'INDICATOR_E', 
  '团伙作案次数', 
  '统计短时间内多个账户向同一目标账户转账的次数，用于识别团伙诈骗行为', 
  '{
    "user_type": "payee", 
    "description": "检测团伙诈骗模式，多个账户在短时间内向同一账户转账", 
    "sql_template": "WITH latest_transaction AS (SELECT COALESCE(from_account_id, to_account_id) as account_id, MAX(transfer_time) as last_transaction_time FROM transfer_records GROUP BY COALESCE(from_account_id, to_account_id)), time_window AS (SELECT lt.account_id, lt.last_transaction_time, (lt.last_transaction_time - INTERVAL ''{time_window_days} days'') as window_start_time FROM latest_transaction lt), gang_transfers AS (SELECT t1.to_account_id as target_account_id, t1.transfer_time as base_time, COUNT(DISTINCT t2.from_account_id) as gang_member_count FROM transfer_records t1 JOIN transfer_records t2 ON t1.to_account_id = t2.to_account_id AND t1.from_account_id != t2.from_account_id AND EXTRACT(EPOCH FROM (t2.transfer_time - t1.transfer_time)) BETWEEN 0 AND {time_window_minutes} * 60 GROUP BY t1.to_account_id, t1.transfer_time HAVING COUNT(DISTINCT t2.from_account_id) >= {gang_member_threshold}) SELECT gt.target_account_id as account_id, COUNT(*) as value FROM gang_transfers gt JOIN time_window tw ON gt.target_account_id = tw.account_id WHERE gt.base_time >= tw.window_start_time AND gt.base_time <= tw.last_transaction_time GROUP BY gt.target_account_id", 
    "gang_member_threshold": 3,
    "time_window_minutes": 30,
    "configurable_fields": [
      {
        "max": 10, 
        "min": 2, 
        "type": "integer", 
        "field": "gang_member_threshold", 
        "label": "团伙成员数量阈值（人）", 
        "default": 3
      },
      {
        "max": 120,
        "min": 10,
        "type": "integer",
        "field": "time_window_minutes",
        "label": "时间窗口（分钟）",
        "default": 30
      }
    ]
  }', 
  1, 
  1, 
  CURRENT_TIMESTAMP, 
  CURRENT_TIMESTAMP, 
  'payee'
);

-- 指标F：异地登录次数
INSERT INTO "analysis_indicators" 
("indicator_id", "indicator_code", "indicator_name", "indicator_desc", "param_config", "is_core", "is_active", "create_time", "update_time", "user_type") 
VALUES (
  6, 
  'INDICATOR_F', 
  '异地登录次数', 
  '统计账户从不同地理位置（IP地址段）登录的次数，用于识别账户盗用或异常登录', 
  '{
    "user_type": "all", 
    "description": "检测异地登录模式，多地点登录可能表示账户被盗用或多人使用", 
    "sql_template": "WITH latest_transaction AS (SELECT COALESCE(from_account_id, to_account_id) as account_id, MAX(transfer_time) as last_transaction_time FROM transfer_records GROUP BY COALESCE(from_account_id, to_account_id)), time_window AS (SELECT lt.account_id, lt.last_transaction_time, (lt.last_transaction_time - INTERVAL ''{time_window_days} days'') as window_start_time FROM latest_transaction lt), login_locations AS (SELECT rl.account_id, SUBSTRING(rl.login_ip FROM ''^[0-9]+\\.[0-9]+'') as ip_location, COUNT(*) as login_count FROM login_record rl JOIN time_window tw ON rl.account_id = tw.account_id WHERE rl.login_time >= tw.window_start_time AND rl.login_time <= tw.last_transaction_time GROUP BY rl.account_id, SUBSTRING(rl.login_ip FROM ''^[0-9]+\\.[0-9]+'')) SELECT account_id, COUNT(DISTINCT ip_location) as value FROM login_locations GROUP BY account_id", 
    "distinct_location_threshold": 3,
    "configurable_fields": [
      {
        "max": 10, 
        "min": 2, 
        "type": "integer", 
        "field": "distinct_location_threshold", 
        "label": "不同地点数量阈值（个）", 
        "default": 3
      }
    ]
  }', 
  1, 
  1, 
  CURRENT_TIMESTAMP, 
  CURRENT_TIMESTAMP, 
  'all'
);

-- 验证插入结果
SELECT 
    indicator_id,
    indicator_code,
    indicator_name,
    indicator_desc,
    user_type,
    is_core,
    is_active
FROM analysis_indicators
WHERE indicator_code IN ('INDICATOR_D', 'INDICATOR_E', 'INDICATOR_F')
ORDER BY indicator_id;

-- 显示所有指标
SELECT 
    indicator_id,
    indicator_code,
    indicator_name,
    user_type,
    is_core,
    is_active,
    create_time
FROM analysis_indicators
ORDER BY indicator_id;
