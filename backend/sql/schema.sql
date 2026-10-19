-- auto-generated definition
create table account
(
    id           bigserial
        primary key,
    account_no   varchar(50)                         not null
        unique,
    holder_name  varchar(100)                        not null,
    id_card_no   varchar(20)                         not null
        unique,
    phone        varchar(20)                         not null
        unique,
    account_type smallint                            not null,
    status       smallint  default 1                 not null,
    create_time  timestamp default CURRENT_TIMESTAMP not null
);

comment on table account is '测试数据：包含16个账户，涵盖正常用户、潜在受害人、可疑收款方、中转账户、企业账户等各种场景';

comment on column account.id is '必要：账户唯一标识，自增主键';

comment on column account.account_no is '必要：银行账户号码，用户操作账号，唯一';

comment on column account.holder_name is '必要：账户持有人姓名，用于身份关联';

comment on column account.id_card_no is '必要：持有人身份证号，唯一标识自然人';

comment on column account.phone is '必要：绑定手机号，关联手机银行登录';

comment on column account.account_type is '必要：账户类型（1=个人，2=企业）';

comment on column account.status is '必要：账户状态（1=正常，2=可疑，3=冻结，4=注销）';

comment on column account.create_time is '必要：开户时间，系统自动记录';

alter table account
    owner to postgres;

create index idx_account_status_type
    on account (status, account_type);

create index idx_account_type
    on account (account_type);

create index idx_status
    on account (status);

create index idx_create_time
    on account (create_time);

create index idx_phone
    on account (phone);

create index idx_id_card
    on account (id_card_no);

create index idx_type_status
    on account (account_type, status);

create index idx_account_phone
    on account (phone);

create index idx_account_id_card
    on account (id_card_no);

create index idx_account_no
    on account (account_no);

create index idx_account_status
    on account (status);

----------------------------------------------
-- auto-generated definition
create table analysis_indicators
(
    indicator_id   bigserial
        primary key,
    indicator_code varchar(50)                         not null
        unique,
    indicator_name varchar(100)                        not null,
    indicator_desc text                                not null,
    param_config   jsonb                               not null,
    is_core        smallint  default 1                 not null,
    is_active      smallint  default 1                 not null,
    create_time    timestamp default CURRENT_TIMESTAMP not null,
    update_time    timestamp default CURRENT_TIMESTAMP not null,
    user_type      varchar(20)
);

comment on table analysis_indicators is '分析指标配置表 - 已更新SQL模板支持基于最后交易时间的指标计算';

comment on column analysis_indicators.indicator_id is '必要：指标唯一标识，自增主键';

comment on column analysis_indicators.indicator_code is '必要：指标代码';

comment on column analysis_indicators.indicator_name is '必要：指标名称（如"大额快进快出次数"）';

comment on column analysis_indicators.indicator_desc is '必要：指标业务说明，明确计算逻辑';

comment on column analysis_indicators.param_config is '必要：指标参数（如{"大额阈值":50000,"时间窗口":120}）';

comment on column analysis_indicators.is_core is '必要：是否核心指标（1=是，0=否），区分PDF要求指标与扩展指标';

comment on column analysis_indicators.is_active is '必要：启用状态（1=启用，0=禁用）';

comment on column analysis_indicators.create_time is '必要：指标创建时间';

comment on column analysis_indicators.update_time is '必要：指标更新时间';

comment on column analysis_indicators.user_type is '用户类型：payer(付款方/受害人)、payee(收款方/可疑分子)、both(通用)';

alter table analysis_indicators
    owner to postgres;

create index idx_analysis_indicators_code
    on analysis_indicators (indicator_code);

create index idx_analysis_indicators_active
    on analysis_indicators (is_active, is_core);

-------------------------------------------------------
-- auto-generated definition
create table analysis_tasks
(
    task_id               bigserial
        primary key,
    task_name             varchar(100)                        not null,
    task_code             varchar(50)                         not null
        unique,
    indicator_ids         varchar(200)                        not null,
    rule_config           jsonb                               not null,
    time_range            integer                             not null,
    start_time            timestamp                           not null,
    end_time              timestamp,
    task_status           smallint  default 0                 not null,
    progress              integer   default 0                 not null,
    analyzed_accounts     integer   default 0,
    result_count          integer   default 0,
    create_time           timestamp default CURRENT_TIMESTAMP not null,
    update_time           timestamp default CURRENT_TIMESTAMP not null,
    task_time_range       integer                             not null,
    indicator_time_window integer                             not null
);

comment on table analysis_tasks is '测试数据：包含4种不同严格程度的检测任务，从宽松到严格，支持不同业务场景的风险检测需求';

comment on column analysis_tasks.task_id is '必要：任务唯一标识，自增主键';

comment on column analysis_tasks.task_name is '必要：任务名称（如"2024Q2涉诈排查"），便于识别';

comment on column analysis_tasks.task_code is '必要：任务编码（如"TASK_202406_001"），代码中唯一标识';

comment on column analysis_tasks.indicator_ids is '必要：参与任务的指标ID列表（如"1,2,3"）';

comment on column analysis_tasks.rule_config is '必要：规则配置（如{"逻辑":"A>0且B>0且C=0"}）';

comment on column analysis_tasks.time_range is '必要：分析时间范围（天）';

comment on column analysis_tasks.start_time is '必要：任务开始时间';

comment on column analysis_tasks.end_time is '必要：任务完成时间（NULL=未完成）';

comment on column analysis_tasks.task_status is '必要：任务状态（0=待执行，1=执行中，2=完成，3=失败）';

comment on column analysis_tasks.progress is '可选：执行进度（0-100），前端展示进度';

comment on column analysis_tasks.analyzed_accounts is '可选：分析账户总数';

comment on column analysis_tasks.result_count is '可选：风险账户总数（任务完成后更新）';

comment on column analysis_tasks.create_time is '必要：任务创建时间';

comment on column analysis_tasks.update_time is '必要：任务更新时间';

comment on column analysis_tasks.task_time_range is '任务筛选时间范围（天）- 决定分析哪些账户';

comment on column analysis_tasks.indicator_time_window is '指标计算时间窗口（天）- 从最后交易时间往前推多少天';

alter table analysis_tasks
    owner to postgres;

create index idx_analysis_tasks_status
    on analysis_tasks (task_status);

----------------------------------------------------
-- auto-generated definition
create table login_record
(
    record_id  bigserial
        primary key,
    account_id bigint      not null
        references account
            on delete restrict,
    login_time timestamp   not null,
    login_ip   varchar(50) not null
);

alter table login_record
    owner to postgres;

create index idx_login_account
    on login_record (account_id);

create index idx_login_time
    on login_record (login_time desc);

-------------------------------------------------------
-- auto-generated definition
create table risk_results
(
    risk_id            bigserial
        primary key,
    task_id            bigint                              not null
        references analysis_tasks
            on delete cascade,
    account_id         bigint                              not null
        references account
            on delete restrict,
    account_role       smallint                            not null,
    matched_indicators jsonb                               not null,
    matched_rules      text                                not null,
    create_time        timestamp default CURRENT_TIMESTAMP not null,
    update_time        timestamp default CURRENT_TIMESTAMP not null,
    unique (task_id, account_id, account_role)
);

comment on table risk_results is '风险分析结果表，支撑PDF中付款方/收款方区分需求，便于多维度分析';

comment on column risk_results.risk_id is '必要：风险记录唯一标识，自增主键';

comment on column risk_results.task_id is '必要：关联任务ID，外键关联analysis_tasks表';

comment on column risk_results.account_id is '必要：风险账户ID，外键关联account表';

comment on column risk_results.account_role is '必要：账户角色（1=付款方/受害人，2=收款方/可疑账户）';

comment on column risk_results.matched_indicators is '必要：匹配指标及数值（如{"A":2,"B":1,"C":0}）';

comment on column risk_results.matched_rules is '必要：匹配规则描述，便于理解风险原因';

comment on column risk_results.create_time is '必要：记录创建时间';

comment on column risk_results.update_time is '必要：记录更新时间';

alter table risk_results
    owner to postgres;

-------------------------------------------------------
-- auto-generated definition
create table transfer_records
(
    transfer_id     bigserial
        primary key,
    from_account_id bigint         not null
        references account
            on delete restrict,
    to_account_id   bigint         not null
        references account
            on delete restrict,
    transfer_no     varchar(50)    not null
        unique,
    amount          numeric(20, 2) not null,
    transfer_time   timestamp      not null
);

alter table transfer_records
    owner to postgres;

create index idx_transfer_from_account
    on transfer_records (from_account_id);

create index idx_transfer_to_account
    on transfer_records (to_account_id);

create index idx_transfer_time
    on transfer_records (transfer_time desc);

