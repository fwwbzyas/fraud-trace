#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
银行反欺诈系统 - 完整数据生成脚本
生成 30万账户 + 1000万登录记录 + 2000万转账记录

数据规模：
- 账户：300,000 (30万)
  - 受害人：9,000 (9千，3%)
  - 可疑分子：6,000 (6千，2%)
  - 正常账户：285,000 (28.5万，95%)
  
- 登录记录：10,020,000 (1002万)
  - 受害人：90万 (每人100条)
  - 正常账户：912万 (每人32条)
  
- 转账记录：20,000,000 (2000万)
  - A指标：大额快进快出
  - B指标：登录后快速转账
  - C指标：可疑分子累计入账
  - 正常转账：填充到2000万

预计耗时：20-40分钟（取决于硬件配置）
"""

from database_config import get_db_config

import random
import datetime
import os
import csv
import time
from faker import Faker
import psycopg2
from psycopg2.extras import execute_values
from tqdm import tqdm

# 初始化
fake = Faker('zh_CN')
fake.seed_instance(12345)
random.seed(12345)

# ========================================================================
# 数据库配置（请根据实际情况修改）
# ========================================================================

DB_CONFIG = get_db_config("banksys")

# ========================================================================
# 数据量配置
# ========================================================================

NUM_ACCOUNTS = 300_000        # 30万账户
NUM_LOGINS = 10_000_000       # 1000万登录（实际会生成约1002万）
NUM_TRANSFERS = 20_000_000    # 2000万转账

# 临时目录
TEMP_DIR = './temp_csv'

# ========================================================================
# 中国城市列表
# ========================================================================

CHINA_LOCATIONS = [
    '北京市东城区', '北京市西城区', '北京市朝阳区', '北京市海淀区', '北京市丰台区',
    '上海市黄浦区', '上海市浦东新区', '上海市徐汇区', '上海市长宁区', '上海市静安区',
    '广东省广州市', '广东省深圳市', '广东省珠海市', '广东省东莞市', '广东省佛山市',
    '浙江省杭州市', '浙江省宁波市', '浙江省温州市', '浙江省嘉兴市', '浙江省绍兴市',
    '江苏省南京市', '江苏省苏州市', '江苏省无锡市', '江苏省常州市', '江苏省南通市',
    '四川省成都市', '四川省绵阳市', '重庆市渝中区', '重庆市江北区', '重庆市南岸区',
    '湖北省武汉市', '湖北省宜昌市', '湖南省长沙市', '湖南省株洲市', '湖南省湘潭市',
    '陕西省西安市', '陕西省咸阳市', '河南省郑州市', '河南省洛阳市', '河南省开封市',
    '山东省济南市', '山东省青岛市', '山东省烟台市', '山东省潍坊市', '山东省淄博市',
    '辽宁省沈阳市', '辽宁省大连市', '辽宁省鞍山市', '福建省福州市', '福建省厦门市',
    '天津市和平区', '天津市河西区', '天津市南开区', '河北省石家庄市', '河北省唐山市',
    '安徽省合肥市', '安徽省芜湖市', '江西省南昌市', '江西省赣州市', '广西壮族自治区南宁市'
]


# ========================================================================
# 工具函数
# ========================================================================

def ensure_temp_dir():
    """确保临时目录存在"""
    if not os.path.exists(TEMP_DIR):
        os.makedirs(TEMP_DIR)


def generate_unique_phone(seq_num):
    """生成唯一手机号"""
    prefix = '1'
    second_digit = str((seq_num % 7) + 3)
    remaining = f"{seq_num:09d}"
    return prefix + second_digit + remaining


def generate_unique_id_card(seq_num):
    """生成唯一身份证号"""
    area_codes = ['110101', '310101', '440101', '440305', '330101', '500101']
    area_code = area_codes[seq_num % 6]
    unique_part = f"{seq_num:012d}"
    return area_code + unique_part


def generate_location():
    """生成中国城市地址"""
    return random.choice(CHINA_LOCATIONS)


# ========================================================================
# 阶段1：生成账户数据
# ========================================================================

def generate_accounts_csv():
    """生成账号CSV文件"""
    print("\n" + "="*70)
    print("📦 阶段1：生成账户CSV文件（300万账户）")
    print("="*70)
    
    ensure_temp_dir()
    
    num_victims = int(NUM_ACCOUNTS * 0.03)    # 9万受害人（3%）
    num_suspects = int(NUM_ACCOUNTS * 0.02)   # 6万可疑分子（2%）
    num_normals = NUM_ACCOUNTS - num_victims - num_suspects  # 285万正常账户
    
    print(f"受害人：{num_victims:,} | 可疑分子：{num_suspects:,} | 正常账户：{num_normals:,}")
    
    csv_file = f"{TEMP_DIR}/accounts.csv"
    account_counter = 0
    
    victim_accounts = []
    suspect_accounts = []
    
    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        
        # 1. 受害人
        print("\n生成受害人...")
        victim_configs = [
            (num_victims // 4, 5, 10),
            (num_victims // 4, 10, 15),
            (num_victims // 4, 15, 20),
            (num_victims - (num_victims // 4) * 3, 20, 25),
        ]
        
        for count, a_times, b_times in tqdm(victim_configs, desc="受害人"):
            for _ in range(count):
                account_no = f"VIC{account_counter:07d}"
                writer.writerow([
                    account_no,
                    fake.name(),
                    generate_unique_id_card(account_counter),
                    generate_unique_phone(account_counter),
                    1,  # account_type
                    0,  # status
                    (datetime.datetime.now() - datetime.timedelta(days=random.randint(365, 1095))).strftime('%Y-%m-%d %H:%M:%S')
                ])
                victim_accounts.append({
                    'account_no': account_no,
                    'seq': account_counter,
                    'a_times': a_times,
                    'b_times': b_times
                })
                account_counter += 1
        
        # 2. 可疑分子
        print("\n生成可疑分子...")
        suspect_configs = [
            (num_suspects // 4, 50000, 100000),
            (num_suspects // 4, 100000, 200000),
            (num_suspects // 4, 200000, 500000),
            (num_suspects - (num_suspects // 4) * 3, 500000, 1000000),
        ]
        
        for count, min_amount, max_amount in tqdm(suspect_configs, desc="可疑分子"):
            for _ in range(count):
                account_no = f"SUS{account_counter:07d}"
                writer.writerow([
                    account_no,
                    fake.name(),
                    generate_unique_id_card(account_counter),
                    generate_unique_phone(account_counter),
                    1,
                    1,  # status=1（可疑）
                    (datetime.datetime.now() - datetime.timedelta(days=random.randint(365, 1095))).strftime('%Y-%m-%d %H:%M:%S')
                ])
                suspect_accounts.append({
                    'account_no': account_no,
                    'seq': account_counter,
                    'total_income': random.uniform(min_amount, max_amount)
                })
                account_counter += 1
        
        # 3. 正常账户
        print("\n生成正常账户...")
        for _ in tqdm(range(num_normals), desc="正常账户"):
            account_no = f"NOR{account_counter:07d}"
            writer.writerow([
                account_no,
                fake.name(),
                generate_unique_id_card(account_counter),
                generate_unique_phone(account_counter),
                1,
                0,
                (datetime.datetime.now() - datetime.timedelta(days=random.randint(365, 1095))).strftime('%Y-%m-%d %H:%M:%S')
            ])
            account_counter += 1
    
    print(f"✅ CSV文件已生成：{csv_file}")
    print(f"   文件大小：{os.path.getsize(csv_file) / (1024*1024):.2f} MB")
    return csv_file, victim_accounts, suspect_accounts


def import_accounts_csv(csv_file):
    """使用COPY命令导入账户"""
    print("\n💾 使用COPY命令导入账户（极速）...")
    
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    
    # 临时禁用约束和索引
    print("⚙️  临时禁用约束和索引...")
    cur.execute("ALTER TABLE account DROP CONSTRAINT IF EXISTS account_phone_key;")
    cur.execute("ALTER TABLE account DROP CONSTRAINT IF EXISTS account_id_card_no_key;")
    cur.execute("ALTER TABLE account DROP CONSTRAINT IF EXISTS account_account_no_key;")
    cur.execute("DROP INDEX IF EXISTS idx_account_no;")
    cur.execute("DROP INDEX IF EXISTS idx_account_status;")
    conn.commit()
    
    # COPY导入
    print("📥 COPY导入中...")
    with open(csv_file, 'r', encoding='utf-8') as f:
        cur.copy_from(
            f,
            'account',
            sep=',',
            columns=('account_no', 'holder_name', 'id_card_no', 'phone', 'account_type', 'status', 'create_time')
        )
    conn.commit()
    
    # 重建约束和索引
    print("⚙️  重建约束和索引...")
    cur.execute("ALTER TABLE account ADD CONSTRAINT account_account_no_key UNIQUE (account_no);")
    cur.execute("ALTER TABLE account ADD CONSTRAINT account_phone_key UNIQUE (phone);")
    cur.execute("ALTER TABLE account ADD CONSTRAINT account_id_card_no_key UNIQUE (id_card_no);")
    cur.execute("CREATE INDEX idx_account_no ON account(account_no);")
    cur.execute("CREATE INDEX idx_account_status ON account(status);")
    conn.commit()
    
    # 获取ID映射
    print("📋 获取账户ID映射...")
    cur.execute("SELECT account_no, id FROM account")
    account_id_map = dict(cur.fetchall())
    
    cur.close()
    conn.close()
    
    print(f"✅ 账户导入完成：{len(account_id_map):,} 个")
    return account_id_map


# ========================================================================
# 阶段2：生成登录记录数据
# ========================================================================

def generate_logins_csv(victim_accounts, account_id_map):
    """生成登录记录CSV"""
    print("\n" + "="*70)
    print("📦 阶段2：生成登录记录CSV文件（1.011亿条）")
    print("="*70)
    
    csv_file = f"{TEMP_DIR}/logins.csv"
    
    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        
        # 受害人登录（每人100条，总计90万）
        print("生成受害人登录（每人100条）...")
        for victim in tqdm(victim_accounts, desc="受害人登录"):
            account_id = account_id_map[victim['account_no']]
            login_times_for_b = []
            
            for i in range(100):  # 精确控制：9000×100=90万
                login_time = datetime.datetime.now() - datetime.timedelta(
                    days=random.randint(1, 30),
                    hours=random.randint(0, 23),
                    minutes=random.randint(0, 59),
                    seconds=random.randint(0, 59)
                )
                
                writer.writerow([
                    account_id,
                    login_time.strftime('%Y-%m-%d %H:%M:%S'),
                    generate_location()
                ])
                
                if i < victim['b_times']:
                    login_times_for_b.append(login_time)
            
            victim['login_times'] = login_times_for_b
            victim['id'] = account_id
        
        # 正常账户登录（每人32条，总计912万）
        print("\n生成正常账户登录（每人32条）...")
        normal_accounts = [acc_no for acc_no in account_id_map.keys() if acc_no.startswith('NOR')]
        logins_per_normal = 32  # 精确控制：28.5万×32=912万
        
        for account_no in tqdm(normal_accounts, desc="正常账户登录"):
            account_id = account_id_map[account_no]
            
            for _ in range(logins_per_normal):
                login_time = datetime.datetime.now() - datetime.timedelta(
                    days=random.randint(1, 60),
                    hours=random.randint(0, 23),
                    minutes=random.randint(0, 59),
                    seconds=random.randint(0, 59)
                )
                
                writer.writerow([
                    account_id,
                    login_time.strftime('%Y-%m-%d %H:%M:%S'),
                    generate_location()
                ])
    
    print(f"✅ CSV文件已生成：{csv_file}")
    print(f"   文件大小：{os.path.getsize(csv_file) / (1024*1024):.2f} MB")
    return csv_file


def import_logins_csv(csv_file):
    """导入登录记录（超快版）"""
    print("\n💾 超快批量导入登录记录...")
    
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    
    # 临时禁用索引和触发器
    print("⚙️  临时删除索引和触发器...")
    cur.execute("DROP INDEX IF EXISTS idx_login_account_time;")
    cur.execute("DROP INDEX IF EXISTS idx_login_time;")
    cur.execute("ALTER TABLE login_record DISABLE TRIGGER ALL;")
    conn.commit()
    
    # 超快批量插入
    print("📥 超快批量插入...")
    batch_size = 100000  # 10万条一批
    batch_data = []
    total_rows = 0
    
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        for row in reader:
            batch_data.append((int(row[0]), row[1], row[2]))
            
            if len(batch_data) >= batch_size:
                execute_values(cur, 
                    "INSERT INTO login_record (account_id, login_time, login_ip) VALUES %s",
                    batch_data, page_size=10000)
                total_rows += len(batch_data)
                # 每100万条commit一次
                if total_rows % 1000000 == 0:
                    conn.commit()
                    print(f"   已插入: {total_rows:,} 条 ({total_rows/10020000*100:.1f}%) [已提交]", end='\r')
                else:
                    print(f"   已插入: {total_rows:,} 条 ({total_rows/10020000*100:.1f}%)", end='\r')
                batch_data = []
        
        if batch_data:
            execute_values(cur, 
                "INSERT INTO login_record (account_id, login_time, login_ip) VALUES %s",
                batch_data, page_size=10000)
            total_rows += len(batch_data)
    
    conn.commit()
    print(f"\n✅ 共插入: {total_rows:,} 条登录记录")
    
    # 暂时只恢复触发器，索引留到最后统一重建
    print("⚙️  恢复触发器...")
    cur.execute("ALTER TABLE login_record ENABLE TRIGGER ALL;")
    conn.commit()
    
    # ⚠️ 注意：索引将在所有数据生成完成后统一重建，以提高整体性能
    print("   ℹ️  登录记录索引将在所有数据生成完成后统一重建")
    
    cur.close()
    conn.close()
    
    print("✅ 登录记录导入完成")


# ========================================================================
# 阶段3：生成转账记录数据
# ========================================================================

def generate_transfers_csv(victim_accounts, suspect_accounts, account_id_map):
    """生成转账记录CSV文件"""
    print("\n" + "="*70)
    print("📦 阶段3：生成转账记录CSV（2000万条）")
    print("="*70)
    
    csv_file = f"{TEMP_DIR}/transfers.csv"
    transfer_counter = 0
    
    # 为账户添加ID
    for suspect in suspect_accounts:
        suspect['id'] = account_id_map[suspect['account_no']]
    
    normal_account_ids = [account_id_map[no] for no in account_id_map.keys() if no.startswith('NOR')]
    
    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        
        # 1. 生成A指标转账（大额快进快出）
        print("\n1️⃣ 生成A指标转账（大额快进快出）...")
        for victim in tqdm(victim_accounts, desc="A指标"):
            a_times = victim['a_times']
            
            for _ in range(a_times):
                suspect = random.choice(suspect_accounts)
                large_amount = random.uniform(50000, 200000)
                
                base_time = datetime.datetime.now() - datetime.timedelta(
                    days=random.randint(1, 30),
                    hours=random.randint(0, 23),
                    minutes=random.randint(0, 59)
                )
                
                # 快进：正常账户 → 受害人
                from_id = random.choice(normal_account_ids)
                writer.writerow([
                    from_id,
                    victim['id'],
                    f"TXN{transfer_counter:012d}",
                    large_amount,
                    base_time.strftime('%Y-%m-%d %H:%M:%S')
                ])
                transfer_counter += 1
                
                # 快出：受害人 → 可疑分子（2分钟内）
                quick_out_time = base_time + datetime.timedelta(seconds=random.randint(10, 120))
                writer.writerow([
                    victim['id'],
                    suspect['id'],
                    f"TXN{transfer_counter:012d}",
                    large_amount * random.uniform(0.95, 0.99),
                    quick_out_time.strftime('%Y-%m-%d %H:%M:%S')
                ])
                transfer_counter += 1
        
        print(f"✅ A指标转账生成完成：约{transfer_counter:,}笔")
    
    # 2. 生成B指标转账（登录后转账）
    print("\n2️⃣ 生成B指标转账（登录后快速转账）...")
    for victim in tqdm(victim_accounts, desc="B指标"):
        if 'login_times' in victim and victim['login_times']:
            for login_time in victim['login_times']:
                suspect = random.choice(suspect_accounts)
                large_amount = random.uniform(50000, 150000)
                quick_transfer_time = login_time + datetime.timedelta(seconds=random.randint(30, 300))
                
                batch_data.append((
                    victim['id'],
                    suspect['id'],
                    f"TXN{transfer_counter:012d}",
                    large_amount,
                    quick_transfer_time
                ))
                transfer_counter += 1
                
                # 批量插入
                if len(batch_data) >= batch_size:
                    execute_values(cur,
                        "INSERT INTO transfer_records (from_account_id, to_account_id, transfer_no, amount, transfer_time) VALUES %s",
                        batch_data, page_size=10000)
                    total_inserted += len(batch_data)
                    if total_inserted % 1000000 == 0:
                        conn.commit()
                        print(f"   已插入: {total_inserted:,} 条 [{total_inserted/NUM_TRANSFERS*100:.2f}%] [已提交]", end='\r')
                    batch_data = []
    
    print(f"\n✅ B指标转账生成完成")
    
    # 3. 生成C指标转账（可疑分子入账）
    print("\n3️⃣ 生成C指标转账（可疑分子累计入账）...")
    for suspect in tqdm(suspect_accounts, desc="C指标"):
        total_income = suspect['total_income']
        num_transfers = random.randint(10, 30)
        
        # 拆分金额
        amounts = []
        remaining = total_income
        for i in range(num_transfers - 1):
            amount = random.uniform(remaining * 0.05, remaining * 0.3)
            amounts.append(amount)
            remaining -= amount
        amounts.append(remaining)
        
        for amount in amounts:
            from_id = random.choice(normal_account_ids)
            transfer_time = datetime.datetime.now() - datetime.timedelta(
                days=random.randint(1, 30),
                hours=random.randint(0, 23),
                minutes=random.randint(0, 59)
            )
            
            batch_data.append((
                from_id,
                suspect['id'],
                f"TXN{transfer_counter:012d}",
                amount,
                transfer_time
            ))
            transfer_counter += 1
            
            # 批量插入
            if len(batch_data) >= batch_size:
                execute_values(cur,
                    "INSERT INTO transfer_records (from_account_id, to_account_id, transfer_no, amount, transfer_time) VALUES %s",
                    batch_data, page_size=10000)
                total_inserted += len(batch_data)
                if total_inserted % 1000000 == 0:
                    conn.commit()
                    print(f"   已插入: {total_inserted:,} 条 [{total_inserted/NUM_TRANSFERS*100:.2f}%] [已提交]", end='\r')
                batch_data = []
    
    print(f"\n✅ C指标转账生成完成")
    
    # 4. 生成正常转账（填充到2亿）
    remaining = NUM_TRANSFERS - transfer_counter
    print(f"\n4️⃣ 生成正常转账（填充到2亿）...")
    print(f"   需要生成：{remaining:,}笔")
    
    for i in tqdm(range(remaining), desc="正常转账"):
        from_id = random.choice(normal_account_ids)
        to_id = random.choice(normal_account_ids)
        while to_id == from_id:
            to_id = random.choice(normal_account_ids)
        
        amount = random.uniform(100, 10000)
        transfer_time = datetime.datetime.now() - datetime.timedelta(
            days=random.randint(1, 60),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59)
        )
        
        batch_data.append((
            from_id,
            to_id,
            f"TXN{transfer_counter:012d}",
            amount,
            transfer_time
        ))
        transfer_counter += 1
        
        # 批量插入
        if len(batch_data) >= batch_size:
            execute_values(cur,
                "INSERT INTO transfer_records (from_account_id, to_account_id, transfer_no, amount, transfer_time) VALUES %s",
                batch_data, page_size=10000)
            total_inserted += len(batch_data)
            if total_inserted % 1000000 == 0:
                conn.commit()
                print(f"\r   已插入: {total_inserted:,} 条 [{total_inserted/NUM_TRANSFERS*100:.2f}%] [已提交]", end='')
            batch_data = []
    
    # 插入剩余数据
    if batch_data:
        execute_values(cur,
            "INSERT INTO transfer_records (from_account_id, to_account_id, transfer_no, amount, transfer_time) VALUES %s",
            batch_data, page_size=10000)
        total_inserted += len(batch_data)
    
    conn.commit()
    print(f"\n\n✅ 正常转账生成完成，总计：{total_inserted:,}笔")
    
    # 恢复触发器
    print("\n⚙️  恢复触发器...")
    cur.execute("ALTER TABLE transfer_records ENABLE TRIGGER ALL;")
    conn.commit()
    
    # ⚠️ 注意：转账索引将在所有数据生成完成后统一重建，以提高整体性能
    print("   ℹ️  转账记录索引将在所有数据生成完成后统一重建")
    
    cur.close()
    conn.close()
    
    print("\n✅ 转账记录生成完成")


# ========================================================================
# 清理和统计
# ========================================================================

def cleanup_temp_files():
    """清理临时文件"""
    print("\n🗑️  清理临时CSV文件...")
    import shutil
    if os.path.exists(TEMP_DIR):
        shutil.rmtree(TEMP_DIR)
    print("✅ 清理完成")


def print_final_statistics():
    """打印最终统计"""
    print("\n" + "="*70)
    print("📊 最终数据统计")
    print("="*70)
    
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    
    cur.execute("SELECT COUNT(*) FROM account")
    print(f"账户总数：{cur.fetchone()[0]:,}")
    
    cur.execute("SELECT COUNT(*) FROM account WHERE status = 1")
    print(f"可疑账户：{cur.fetchone()[0]:,}")
    
    cur.execute("SELECT COUNT(*) FROM login_record")
    print(f"登录记录：{cur.fetchone()[0]:,}")
    
    cur.execute("SELECT COUNT(*) FROM transfer_records")
    print(f"转账记录：{cur.fetchone()[0]:,}")
    
    cur.execute("SELECT pg_size_pretty(pg_database_size('banksys'));")
    print(f"数据库大小：{cur.fetchone()[0]}")
    
    cur.close()
    conn.close()


def rebuild_all_indexes():
    """统一重建所有索引（性能优化）"""
    print("\n" + "="*70)
    print("🔨 统一重建所有索引")
    print("="*70)
    print("⚠️  这一步可能需要10-20分钟，请耐心等待...")
    
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    
    try:
        # 登录记录索引
        print("\n1️⃣ 重建登录记录索引...")
        print("   - idx_login_record_login_time")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_login_record_login_time ON login_record(login_time);")
        conn.commit()
        
        print("   - idx_login_record_login_ip")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_login_record_login_ip ON login_record(login_ip);")
        conn.commit()
        
        # 转账记录索引（如果还没创建）
        print("\n2️⃣ 检查转账记录索引...")
        print("   - idx_transfer_from_time")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_transfer_from_time ON transfer_records(from_account_id, transfer_time);")
        conn.commit()
        
        print("   - idx_transfer_to_time")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_transfer_to_time ON transfer_records(to_account_id, transfer_time);")
        conn.commit()
        
        print("   - idx_transfer_time")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_transfer_time ON transfer_records(transfer_time);")
        conn.commit()
        
        # 账户索引（确保存在）
        print("\n3️⃣ 检查账户索引...")
        print("   - idx_account_no")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_account_no ON account(account_no);")
        conn.commit()
        
        print("   - idx_account_status")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_account_status ON account(status);")
        conn.commit()
        
        print("\n✅ 所有索引重建完成！")
        
    except Exception as e:
        print(f"\n⚠️  索引重建失败：{e}")
        print("   可以稍后手动执行索引重建SQL")
        
    finally:
        cur.close()
        conn.close()


# ========================================================================
# 主函数
# ========================================================================

def main():
    """主函数"""
    start_time = time.time()
    
    print("\n" + "🚀 "*30)
    print("银行反欺诈系统 - 完整数据生成脚本")
    print("🚀 "*30)
    print(f"\n生成目标：")
    print(f"  - 账户：{NUM_ACCOUNTS:,} (30万)")
    print(f"  - 登录：~{NUM_LOGINS/1000000:.0f}百万 (约1002万)")
    print(f"  - 转账：{NUM_TRANSFERS:,} (2000万)")
    print(f"\n预计耗时：20-40分钟")
    print("\n" + "="*70)
    
    confirm = input("\n⚠️  确认开始生成？这将清空现有数据！(yes/no): ")
    if confirm.lower() != 'yes':
        print("❌ 已取消")
        return
    
    try:
        # 清空数据
        print("\n🗑️  清空现有数据...")
        conn = psycopg2.connect(**DB_CONFIG)
        cur = conn.cursor()
        cur.execute("TRUNCATE TABLE risk_results CASCADE")
        cur.execute("TRUNCATE TABLE login_record CASCADE")
        cur.execute("TRUNCATE TABLE transfer_records CASCADE")
        cur.execute("TRUNCATE TABLE account CASCADE")
        cur.execute("TRUNCATE TABLE analysis_tasks CASCADE")
        conn.commit()
        cur.close()
        conn.close()
        print("✅ 清空完成")
        
        # 阶段1：账户
        accounts_csv, victim_accounts, suspect_accounts = generate_accounts_csv()
        account_id_map = import_accounts_csv(accounts_csv)
        
        # 阶段2：登录
        logins_csv = generate_logins_csv(victim_accounts, account_id_map)
        import_logins_csv(logins_csv)
        
        # 阶段3：转账
        generate_transfers(victim_accounts, suspect_accounts, account_id_map)
        
        # 阶段4：统一重建所有索引
        rebuild_all_indexes()
        
        # 清理临时文件
        cleanup_temp_files()
        
        # 统计
        print_final_statistics()
        
        elapsed = time.time() - start_time
        hours = int(elapsed // 3600)
        minutes = int((elapsed % 3600) // 60)
        
        print("\n" + "="*70)
        print(f"🎉 全部完成！总耗时：{hours}小时{minutes}分钟")
        print("="*70)
        print("\n💡 提示：")
        print("  - 数据已生成完毕，可以启动系统进行测试")
        print("  - 首次查询可能较慢，后续会因为索引而加快")
        print("  - 如需重新生成，请再次运行本脚本")
        print()
        
    except Exception as e:
        print(f"\n❌ 错误：{e}")
        import traceback
        traceback.print_exc()
        cleanup_temp_files()


if __name__ == '__main__':
    main()

"""
银行反欺诈系统 - 完整数据生成脚本
生成 30万账户 + 1000万登录记录 + 2000万转账记录

数据规模：
- 账户：300,000 (30万)
  - 受害人：9,000 (9千，3%)
  - 可疑分子：6,000 (6千，2%)
  - 正常账户：285,000 (28.5万，95%)
  
- 登录记录：10,020,000 (1002万)
  - 受害人：90万 (每人100条)
  - 正常账户：912万 (每人32条)
  
- 转账记录：20,000,000 (2000万)
  - A指标：大额快进快出
  - B指标：登录后快速转账
  - C指标：可疑分子累计入账
  - 正常转账：填充到2000万

预计耗时：20-40分钟（取决于硬件配置）
"""

import random
import datetime
import os
import csv
import time
from faker import Faker
import psycopg2
from psycopg2.extras import execute_values
from tqdm import tqdm

# 初始化
fake = Faker('zh_CN')
fake.seed_instance(12345)
random.seed(12345)

# ========================================================================
# 数据库配置（请根据实际情况修改）
# ========================================================================

DB_CONFIG = get_db_config("banksys")

# ========================================================================
# 数据量配置
# ========================================================================

NUM_ACCOUNTS = 300_000        # 30万账户
NUM_LOGINS = 10_000_000       # 1000万登录（实际会生成约1002万）
NUM_TRANSFERS = 20_000_000    # 2000万转账

# 临时目录
TEMP_DIR = './temp_csv'

# ========================================================================
# 中国城市列表
# ========================================================================

CHINA_LOCATIONS = [
    '北京市东城区', '北京市西城区', '北京市朝阳区', '北京市海淀区', '北京市丰台区',
    '上海市黄浦区', '上海市浦东新区', '上海市徐汇区', '上海市长宁区', '上海市静安区',
    '广东省广州市', '广东省深圳市', '广东省珠海市', '广东省东莞市', '广东省佛山市',
    '浙江省杭州市', '浙江省宁波市', '浙江省温州市', '浙江省嘉兴市', '浙江省绍兴市',
    '江苏省南京市', '江苏省苏州市', '江苏省无锡市', '江苏省常州市', '江苏省南通市',
    '四川省成都市', '四川省绵阳市', '重庆市渝中区', '重庆市江北区', '重庆市南岸区',
    '湖北省武汉市', '湖北省宜昌市', '湖南省长沙市', '湖南省株洲市', '湖南省湘潭市',
    '陕西省西安市', '陕西省咸阳市', '河南省郑州市', '河南省洛阳市', '河南省开封市',
    '山东省济南市', '山东省青岛市', '山东省烟台市', '山东省潍坊市', '山东省淄博市',
    '辽宁省沈阳市', '辽宁省大连市', '辽宁省鞍山市', '福建省福州市', '福建省厦门市',
    '天津市和平区', '天津市河西区', '天津市南开区', '河北省石家庄市', '河北省唐山市',
    '安徽省合肥市', '安徽省芜湖市', '江西省南昌市', '江西省赣州市', '广西壮族自治区南宁市'
]


# ========================================================================
# 工具函数
# ========================================================================

def ensure_temp_dir():
    """确保临时目录存在"""
    if not os.path.exists(TEMP_DIR):
        os.makedirs(TEMP_DIR)


def generate_unique_phone(seq_num):
    """生成唯一手机号"""
    prefix = '1'
    second_digit = str((seq_num % 7) + 3)
    remaining = f"{seq_num:09d}"
    return prefix + second_digit + remaining


def generate_unique_id_card(seq_num):
    """生成唯一身份证号"""
    area_codes = ['110101', '310101', '440101', '440305', '330101', '500101']
    area_code = area_codes[seq_num % 6]
    unique_part = f"{seq_num:012d}"
    return area_code + unique_part


def generate_location():
    """生成中国城市地址"""
    return random.choice(CHINA_LOCATIONS)


# ========================================================================
# 阶段1：生成账户数据
# ========================================================================

def generate_accounts_csv():
    """生成账号CSV文件"""
    print("\n" + "="*70)
    print("📦 阶段1：生成账户CSV文件（300万账户）")
    print("="*70)
    
    ensure_temp_dir()
    
    num_victims = int(NUM_ACCOUNTS * 0.03)    # 9万受害人（3%）
    num_suspects = int(NUM_ACCOUNTS * 0.02)   # 6万可疑分子（2%）
    num_normals = NUM_ACCOUNTS - num_victims - num_suspects  # 285万正常账户
    
    print(f"受害人：{num_victims:,} | 可疑分子：{num_suspects:,} | 正常账户：{num_normals:,}")
    
    csv_file = f"{TEMP_DIR}/accounts.csv"
    account_counter = 0
    
    victim_accounts = []
    suspect_accounts = []
    
    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        
        # 1. 受害人
        print("\n生成受害人...")
        victim_configs = [
            (num_victims // 4, 5, 10),
            (num_victims // 4, 10, 15),
            (num_victims // 4, 15, 20),
            (num_victims - (num_victims // 4) * 3, 20, 25),
        ]
        
        for count, a_times, b_times in tqdm(victim_configs, desc="受害人"):
            for _ in range(count):
                account_no = f"VIC{account_counter:07d}"
                writer.writerow([
                    account_no,
                    fake.name(),
                    generate_unique_id_card(account_counter),
                    generate_unique_phone(account_counter),
                    1,  # account_type
                    0,  # status
                    (datetime.datetime.now() - datetime.timedelta(days=random.randint(365, 1095))).strftime('%Y-%m-%d %H:%M:%S')
                ])
                victim_accounts.append({
                    'account_no': account_no,
                    'seq': account_counter,
                    'a_times': a_times,
                    'b_times': b_times
                })
                account_counter += 1
        
        # 2. 可疑分子
        print("\n生成可疑分子...")
        suspect_configs = [
            (num_suspects // 4, 50000, 100000),
            (num_suspects // 4, 100000, 200000),
            (num_suspects // 4, 200000, 500000),
            (num_suspects - (num_suspects // 4) * 3, 500000, 1000000),
        ]
        
        for count, min_amount, max_amount in tqdm(suspect_configs, desc="可疑分子"):
            for _ in range(count):
                account_no = f"SUS{account_counter:07d}"
                writer.writerow([
                    account_no,
                    fake.name(),
                    generate_unique_id_card(account_counter),
                    generate_unique_phone(account_counter),
                    1,
                    1,  # status=1（可疑）
                    (datetime.datetime.now() - datetime.timedelta(days=random.randint(365, 1095))).strftime('%Y-%m-%d %H:%M:%S')
                ])
                suspect_accounts.append({
                    'account_no': account_no,
                    'seq': account_counter,
                    'total_income': random.uniform(min_amount, max_amount)
                })
                account_counter += 1
        
        # 3. 正常账户
        print("\n生成正常账户...")
        for _ in tqdm(range(num_normals), desc="正常账户"):
            account_no = f"NOR{account_counter:07d}"
            writer.writerow([
                account_no,
                fake.name(),
                generate_unique_id_card(account_counter),
                generate_unique_phone(account_counter),
                1,
                0,
                (datetime.datetime.now() - datetime.timedelta(days=random.randint(365, 1095))).strftime('%Y-%m-%d %H:%M:%S')
            ])
            account_counter += 1
    
    print(f"✅ CSV文件已生成：{csv_file}")
    print(f"   文件大小：{os.path.getsize(csv_file) / (1024*1024):.2f} MB")
    return csv_file, victim_accounts, suspect_accounts


def import_accounts_csv(csv_file):
    """使用COPY命令导入账户"""
    print("\n💾 使用COPY命令导入账户（极速）...")
    
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    
    # 临时禁用约束和索引
    print("⚙️  临时禁用约束和索引...")
    cur.execute("ALTER TABLE account DROP CONSTRAINT IF EXISTS account_phone_key;")
    cur.execute("ALTER TABLE account DROP CONSTRAINT IF EXISTS account_id_card_no_key;")
    cur.execute("ALTER TABLE account DROP CONSTRAINT IF EXISTS account_account_no_key;")
    cur.execute("DROP INDEX IF EXISTS idx_account_no;")
    cur.execute("DROP INDEX IF EXISTS idx_account_status;")
    conn.commit()
    
    # COPY导入
    print("📥 COPY导入中...")
    with open(csv_file, 'r', encoding='utf-8') as f:
        cur.copy_from(
            f,
            'account',
            sep=',',
            columns=('account_no', 'holder_name', 'id_card_no', 'phone', 'account_type', 'status', 'create_time')
        )
    conn.commit()
    
    # 重建约束和索引
    print("⚙️  重建约束和索引...")
    cur.execute("ALTER TABLE account ADD CONSTRAINT account_account_no_key UNIQUE (account_no);")
    cur.execute("ALTER TABLE account ADD CONSTRAINT account_phone_key UNIQUE (phone);")
    cur.execute("ALTER TABLE account ADD CONSTRAINT account_id_card_no_key UNIQUE (id_card_no);")
    cur.execute("CREATE INDEX idx_account_no ON account(account_no);")
    cur.execute("CREATE INDEX idx_account_status ON account(status);")
    conn.commit()
    
    # 获取ID映射
    print("📋 获取账户ID映射...")
    cur.execute("SELECT account_no, id FROM account")
    account_id_map = dict(cur.fetchall())
    
    cur.close()
    conn.close()
    
    print(f"✅ 账户导入完成：{len(account_id_map):,} 个")
    return account_id_map


# ========================================================================
# 阶段2：生成登录记录数据
# ========================================================================

def generate_logins_csv(victim_accounts, account_id_map):
    """生成登录记录CSV"""
    print("\n" + "="*70)
    print("📦 阶段2：生成登录记录CSV文件（1.011亿条）")
    print("="*70)
    
    csv_file = f"{TEMP_DIR}/logins.csv"
    
    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        
        # 受害人登录（每人100条，总计90万）
        print("生成受害人登录（每人100条）...")
        for victim in tqdm(victim_accounts, desc="受害人登录"):
            account_id = account_id_map[victim['account_no']]
            login_times_for_b = []
            
            for i in range(100):  # 精确控制：9000×100=90万
                login_time = datetime.datetime.now() - datetime.timedelta(
                    days=random.randint(1, 30),
                    hours=random.randint(0, 23),
                    minutes=random.randint(0, 59),
                    seconds=random.randint(0, 59)
                )
                
                writer.writerow([
                    account_id,
                    login_time.strftime('%Y-%m-%d %H:%M:%S'),
                    generate_location()
                ])
                
                if i < victim['b_times']:
                    login_times_for_b.append(login_time)
            
            victim['login_times'] = login_times_for_b
            victim['id'] = account_id
        
        # 正常账户登录（每人32条，总计912万）
        print("\n生成正常账户登录（每人32条）...")
        normal_accounts = [acc_no for acc_no in account_id_map.keys() if acc_no.startswith('NOR')]
        logins_per_normal = 32  # 精确控制：28.5万×32=912万
        
        for account_no in tqdm(normal_accounts, desc="正常账户登录"):
            account_id = account_id_map[account_no]
            
            for _ in range(logins_per_normal):
                login_time = datetime.datetime.now() - datetime.timedelta(
                    days=random.randint(1, 60),
                    hours=random.randint(0, 23),
                    minutes=random.randint(0, 59),
                    seconds=random.randint(0, 59)
                )
                
                writer.writerow([
                    account_id,
                    login_time.strftime('%Y-%m-%d %H:%M:%S'),
                    generate_location()
                ])
    
    print(f"✅ CSV文件已生成：{csv_file}")
    print(f"   文件大小：{os.path.getsize(csv_file) / (1024*1024):.2f} MB")
    return csv_file


def import_logins_csv(csv_file):
    """导入登录记录（超快版）"""
    print("\n💾 超快批量导入登录记录...")
    
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    
    # 临时禁用索引和触发器
    print("⚙️  临时删除索引和触发器...")
    cur.execute("DROP INDEX IF EXISTS idx_login_account_time;")
    cur.execute("DROP INDEX IF EXISTS idx_login_time;")
    cur.execute("ALTER TABLE login_record DISABLE TRIGGER ALL;")
    conn.commit()
    
    # 超快批量插入
    print("📥 超快批量插入...")
    batch_size = 100000  # 10万条一批
    batch_data = []
    total_rows = 0
    
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        for row in reader:
            batch_data.append((int(row[0]), row[1], row[2]))
            
            if len(batch_data) >= batch_size:
                execute_values(cur, 
                    "INSERT INTO login_record (account_id, login_time, login_ip) VALUES %s",
                    batch_data, page_size=10000)
                total_rows += len(batch_data)
                # 每100万条commit一次
                if total_rows % 1000000 == 0:
                    conn.commit()
                    print(f"   已插入: {total_rows:,} 条 ({total_rows/10020000*100:.1f}%) [已提交]", end='\r')
                else:
                    print(f"   已插入: {total_rows:,} 条 ({total_rows/10020000*100:.1f}%)", end='\r')
                batch_data = []
        
        if batch_data:
            execute_values(cur, 
                "INSERT INTO login_record (account_id, login_time, login_ip) VALUES %s",
                batch_data, page_size=10000)
            total_rows += len(batch_data)
    
    conn.commit()
    print(f"\n✅ 共插入: {total_rows:,} 条登录记录")
    
    # 暂时只恢复触发器，索引留到最后统一重建
    print("⚙️  恢复触发器...")
    cur.execute("ALTER TABLE login_record ENABLE TRIGGER ALL;")
    conn.commit()
    
    # ⚠️ 注意：索引将在所有数据生成完成后统一重建，以提高整体性能
    print("   ℹ️  登录记录索引将在所有数据生成完成后统一重建")
    
    cur.close()
    conn.close()
    
    print("✅ 登录记录导入完成")


# ========================================================================
# 阶段3：生成转账记录数据
# ========================================================================

def generate_transfers_csv(victim_accounts, suspect_accounts, account_id_map):
    """生成转账记录CSV文件"""
    print("\n" + "="*70)
    print("📦 阶段3：生成转账记录CSV（2000万条）")
    print("="*70)
    
    csv_file = f"{TEMP_DIR}/transfers.csv"
    transfer_counter = 0
    
    # 为账户添加ID
    for suspect in suspect_accounts:
        suspect['id'] = account_id_map[suspect['account_no']]
    
    normal_account_ids = [account_id_map[no] for no in account_id_map.keys() if no.startswith('NOR')]
    
    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        
        # 1. 生成A指标转账（大额快进快出）
        print("\n1️⃣ 生成A指标转账（大额快进快出）...")
        for victim in tqdm(victim_accounts, desc="A指标"):
            a_times = victim['a_times']
            
            for _ in range(a_times):
                suspect = random.choice(suspect_accounts)
                large_amount = random.uniform(50000, 200000)
                
                base_time = datetime.datetime.now() - datetime.timedelta(
                    days=random.randint(1, 30),
                    hours=random.randint(0, 23),
                    minutes=random.randint(0, 59)
                )
                
                # 快进：正常账户 → 受害人
                from_id = random.choice(normal_account_ids)
                writer.writerow([
                    from_id,
                    victim['id'],
                    f"TXN{transfer_counter:012d}",
                    large_amount,
                    base_time.strftime('%Y-%m-%d %H:%M:%S')
                ])
                transfer_counter += 1
                
                # 快出：受害人 → 可疑分子（2分钟内）
                quick_out_time = base_time + datetime.timedelta(seconds=random.randint(10, 120))
                writer.writerow([
                    victim['id'],
                    suspect['id'],
                    f"TXN{transfer_counter:012d}",
                    large_amount * random.uniform(0.95, 0.99),
                    quick_out_time.strftime('%Y-%m-%d %H:%M:%S')
                ])
                transfer_counter += 1
        
        print(f"✅ A指标转账生成完成：约{transfer_counter:,}笔")
    
    # 2. 生成B指标转账（登录后转账）
    print("\n2️⃣ 生成B指标转账（登录后快速转账）...")
    for victim in tqdm(victim_accounts, desc="B指标"):
        if 'login_times' in victim and victim['login_times']:
            for login_time in victim['login_times']:
                suspect = random.choice(suspect_accounts)
                large_amount = random.uniform(50000, 150000)
                quick_transfer_time = login_time + datetime.timedelta(seconds=random.randint(30, 300))
                
                batch_data.append((
                    victim['id'],
                    suspect['id'],
                    f"TXN{transfer_counter:012d}",
                    large_amount,
                    quick_transfer_time
                ))
                transfer_counter += 1
                
                # 批量插入
                if len(batch_data) >= batch_size:
                    execute_values(cur,
                        "INSERT INTO transfer_records (from_account_id, to_account_id, transfer_no, amount, transfer_time) VALUES %s",
                        batch_data, page_size=10000)
                    total_inserted += len(batch_data)
                    if total_inserted % 1000000 == 0:
                        conn.commit()
                        print(f"   已插入: {total_inserted:,} 条 [{total_inserted/NUM_TRANSFERS*100:.2f}%] [已提交]", end='\r')
                    batch_data = []
    
    print(f"\n✅ B指标转账生成完成")
    
    # 3. 生成C指标转账（可疑分子入账）
    print("\n3️⃣ 生成C指标转账（可疑分子累计入账）...")
    for suspect in tqdm(suspect_accounts, desc="C指标"):
        total_income = suspect['total_income']
        num_transfers = random.randint(10, 30)
        
        # 拆分金额
        amounts = []
        remaining = total_income
        for i in range(num_transfers - 1):
            amount = random.uniform(remaining * 0.05, remaining * 0.3)
            amounts.append(amount)
            remaining -= amount
        amounts.append(remaining)
        
        for amount in amounts:
            from_id = random.choice(normal_account_ids)
            transfer_time = datetime.datetime.now() - datetime.timedelta(
                days=random.randint(1, 30),
                hours=random.randint(0, 23),
                minutes=random.randint(0, 59)
            )
            
            batch_data.append((
                from_id,
                suspect['id'],
                f"TXN{transfer_counter:012d}",
                amount,
                transfer_time
            ))
            transfer_counter += 1
            
            # 批量插入
            if len(batch_data) >= batch_size:
                execute_values(cur,
                    "INSERT INTO transfer_records (from_account_id, to_account_id, transfer_no, amount, transfer_time) VALUES %s",
                    batch_data, page_size=10000)
                total_inserted += len(batch_data)
                if total_inserted % 1000000 == 0:
                    conn.commit()
                    print(f"   已插入: {total_inserted:,} 条 [{total_inserted/NUM_TRANSFERS*100:.2f}%] [已提交]", end='\r')
                batch_data = []
    
    print(f"\n✅ C指标转账生成完成")
    
    # 4. 生成正常转账（填充到2亿）
    remaining = NUM_TRANSFERS - transfer_counter
    print(f"\n4️⃣ 生成正常转账（填充到2亿）...")
    print(f"   需要生成：{remaining:,}笔")
    
    for i in tqdm(range(remaining), desc="正常转账"):
        from_id = random.choice(normal_account_ids)
        to_id = random.choice(normal_account_ids)
        while to_id == from_id:
            to_id = random.choice(normal_account_ids)
        
        amount = random.uniform(100, 10000)
        transfer_time = datetime.datetime.now() - datetime.timedelta(
            days=random.randint(1, 60),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59)
        )
        
        batch_data.append((
            from_id,
            to_id,
            f"TXN{transfer_counter:012d}",
            amount,
            transfer_time
        ))
        transfer_counter += 1
        
        # 批量插入
        if len(batch_data) >= batch_size:
            execute_values(cur,
                "INSERT INTO transfer_records (from_account_id, to_account_id, transfer_no, amount, transfer_time) VALUES %s",
                batch_data, page_size=10000)
            total_inserted += len(batch_data)
            if total_inserted % 1000000 == 0:
                conn.commit()
                print(f"\r   已插入: {total_inserted:,} 条 [{total_inserted/NUM_TRANSFERS*100:.2f}%] [已提交]", end='')
            batch_data = []
    
    # 插入剩余数据
    if batch_data:
        execute_values(cur,
            "INSERT INTO transfer_records (from_account_id, to_account_id, transfer_no, amount, transfer_time) VALUES %s",
            batch_data, page_size=10000)
        total_inserted += len(batch_data)
    
    conn.commit()
    print(f"\n\n✅ 正常转账生成完成，总计：{total_inserted:,}笔")
    
    # 恢复触发器
    print("\n⚙️  恢复触发器...")
    cur.execute("ALTER TABLE transfer_records ENABLE TRIGGER ALL;")
    conn.commit()
    
    # ⚠️ 注意：转账索引将在所有数据生成完成后统一重建，以提高整体性能
    print("   ℹ️  转账记录索引将在所有数据生成完成后统一重建")
    
    cur.close()
    conn.close()
    
    print("\n✅ 转账记录生成完成")


# ========================================================================
# 清理和统计
# ========================================================================

def cleanup_temp_files():
    """清理临时文件"""
    print("\n🗑️  清理临时CSV文件...")
    import shutil
    if os.path.exists(TEMP_DIR):
        shutil.rmtree(TEMP_DIR)
    print("✅ 清理完成")


def print_final_statistics():
    """打印最终统计"""
    print("\n" + "="*70)
    print("📊 最终数据统计")
    print("="*70)
    
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    
    cur.execute("SELECT COUNT(*) FROM account")
    print(f"账户总数：{cur.fetchone()[0]:,}")
    
    cur.execute("SELECT COUNT(*) FROM account WHERE status = 1")
    print(f"可疑账户：{cur.fetchone()[0]:,}")
    
    cur.execute("SELECT COUNT(*) FROM login_record")
    print(f"登录记录：{cur.fetchone()[0]:,}")
    
    cur.execute("SELECT COUNT(*) FROM transfer_records")
    print(f"转账记录：{cur.fetchone()[0]:,}")
    
    cur.execute("SELECT pg_size_pretty(pg_database_size('banksys'));")
    print(f"数据库大小：{cur.fetchone()[0]}")
    
    cur.close()
    conn.close()


def rebuild_all_indexes():
    """统一重建所有索引（性能优化）"""
    print("\n" + "="*70)
    print("🔨 统一重建所有索引")
    print("="*70)
    print("⚠️  这一步可能需要10-20分钟，请耐心等待...")
    
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor()
    
    try:
        # 登录记录索引
        print("\n1️⃣ 重建登录记录索引...")
        print("   - idx_login_record_login_time")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_login_record_login_time ON login_record(login_time);")
        conn.commit()
        
        print("   - idx_login_record_login_ip")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_login_record_login_ip ON login_record(login_ip);")
        conn.commit()
        
        # 转账记录索引（如果还没创建）
        print("\n2️⃣ 检查转账记录索引...")
        print("   - idx_transfer_from_time")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_transfer_from_time ON transfer_records(from_account_id, transfer_time);")
        conn.commit()
        
        print("   - idx_transfer_to_time")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_transfer_to_time ON transfer_records(to_account_id, transfer_time);")
        conn.commit()
        
        print("   - idx_transfer_time")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_transfer_time ON transfer_records(transfer_time);")
        conn.commit()
        
        # 账户索引（确保存在）
        print("\n3️⃣ 检查账户索引...")
        print("   - idx_account_no")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_account_no ON account(account_no);")
        conn.commit()
        
        print("   - idx_account_status")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_account_status ON account(status);")
        conn.commit()
        
        print("\n✅ 所有索引重建完成！")
        
    except Exception as e:
        print(f"\n⚠️  索引重建失败：{e}")
        print("   可以稍后手动执行索引重建SQL")
        
    finally:
        cur.close()
        conn.close()


# ========================================================================
# 主函数
# ========================================================================

def main():
    """主函数"""
    start_time = time.time()
    
    print("\n" + "🚀 "*30)
    print("银行反欺诈系统 - 完整数据生成脚本")
    print("🚀 "*30)
    print(f"\n生成目标：")
    print(f"  - 账户：{NUM_ACCOUNTS:,} (30万)")
    print(f"  - 登录：~{NUM_LOGINS/1000000:.0f}百万 (约1002万)")
    print(f"  - 转账：{NUM_TRANSFERS:,} (2000万)")
    print(f"\n预计耗时：20-40分钟")
    print("\n" + "="*70)
    
    confirm = input("\n⚠️  确认开始生成？这将清空现有数据！(yes/no): ")
    if confirm.lower() != 'yes':
        print("❌ 已取消")
        return
    
    try:
        # 清空数据
        print("\n🗑️  清空现有数据...")
        conn = psycopg2.connect(**DB_CONFIG)
        cur = conn.cursor()
        cur.execute("TRUNCATE TABLE risk_results CASCADE")
        cur.execute("TRUNCATE TABLE login_record CASCADE")
        cur.execute("TRUNCATE TABLE transfer_records CASCADE")
        cur.execute("TRUNCATE TABLE account CASCADE")
        cur.execute("TRUNCATE TABLE analysis_tasks CASCADE")
        conn.commit()
        cur.close()
        conn.close()
        print("✅ 清空完成")
        
        # 阶段1：账户
        accounts_csv, victim_accounts, suspect_accounts = generate_accounts_csv()
        account_id_map = import_accounts_csv(accounts_csv)
        
        # 阶段2：登录
        logins_csv = generate_logins_csv(victim_accounts, account_id_map)
        import_logins_csv(logins_csv)
        
        # 阶段3：转账
        generate_transfers(victim_accounts, suspect_accounts, account_id_map)
        
        # 阶段4：统一重建所有索引
        rebuild_all_indexes()
        
        # 清理临时文件
        cleanup_temp_files()
        
        # 统计
        print_final_statistics()
        
        elapsed = time.time() - start_time
        hours = int(elapsed // 3600)
        minutes = int((elapsed % 3600) // 60)
        
        print("\n" + "="*70)
        print(f"🎉 全部完成！总耗时：{hours}小时{minutes}分钟")
        print("="*70)
        print("\n💡 提示：")
        print("  - 数据已生成完毕，可以启动系统进行测试")
        print("  - 首次查询可能较慢，后续会因为索引而加快")
        print("  - 如需重新生成，请再次运行本脚本")
        print()
        
    except Exception as e:
        print(f"\n❌ 错误：{e}")
        import traceback
        traceback.print_exc()
        cleanup_temp_files()


if __name__ == '__main__':
    main()












