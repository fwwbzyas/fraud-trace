-- ========================================
-- PostgreSQL 数据库修复脚本
-- 修复 transfer_records 表损坏问题
-- ========================================

-- 1. 备份现有表结构（如果可以访问）
-- CREATE TABLE transfer_records_backup AS SELECT * FROM transfer_records;

-- 2. 删除损坏的表
DROP TABLE IF EXISTS transfer_records CASCADE;

-- 3. 重新创建 transfer_records 表
CREATE TABLE transfer_records (
    transfer_id BIGSERIAL PRIMARY KEY,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    transfer_no VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(20,2) NOT NULL,
    transfer_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_from_account FOREIGN KEY (from_account_id) 
        REFERENCES account(id) ON DELETE RESTRICT,
    CONSTRAINT fk_to_account FOREIGN KEY (to_account_id) 
        REFERENCES account(id) ON DELETE RESTRICT
);

-- 4. 创建索引以提高查询性能
CREATE INDEX idx_transfer_from_account ON transfer_records(from_account_id);
CREATE INDEX idx_transfer_to_account ON transfer_records(to_account_id);
CREATE INDEX idx_transfer_time ON transfer_records(transfer_time DESC);
CREATE INDEX idx_transfer_no ON transfer_records(transfer_no);

-- 5. 添加注释
COMMENT ON TABLE transfer_records IS '转账记录表';
COMMENT ON COLUMN transfer_records.transfer_id IS '转账记录ID（主键）';
COMMENT ON COLUMN transfer_records.from_account_id IS '付款账户ID';
COMMENT ON COLUMN transfer_records.to_account_id IS '收款账户ID';
COMMENT ON COLUMN transfer_records.transfer_no IS '转账流水号（唯一）';
COMMENT ON COLUMN transfer_records.amount IS '转账金额';
COMMENT ON COLUMN transfer_records.transfer_time IS '转账时间';

-- 6. 检查 login_record 表是否也有问题
-- 如果 login_record 也损坏，执行以下语句：
/*
DROP TABLE IF EXISTS login_record CASCADE;

CREATE TABLE login_record (
    record_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    login_time TIMESTAMP NOT NULL,
    login_ip VARCHAR(50) NOT NULL,
    CONSTRAINT fk_login_account FOREIGN KEY (account_id) 
        REFERENCES account(id) ON DELETE RESTRICT
);

CREATE INDEX idx_login_account ON login_record(account_id);
CREATE INDEX idx_login_time ON login_record(login_time DESC);
CREATE INDEX idx_login_ip ON login_record(login_ip);

COMMENT ON TABLE login_record IS '登录记录表';
*/

-- 7. 验证表结构
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name IN ('transfer_records', 'login_record')
ORDER BY table_name, ordinal_position;

-- 8. 显示修复完成信息
SELECT '数据库修复完成！请使用 Python 脚本重新生成测试数据。' AS status;
