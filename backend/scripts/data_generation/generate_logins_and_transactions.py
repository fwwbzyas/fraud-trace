#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
金融电诈数据追查系统 - 极致内存优化版（12GB内存）
核心优化策略：
1. 不在内存中保存登录记录映射，改为文件临时存储
2. 大幅减小随机数池（10k -> 5k）
3. 减小批处理（2k -> 1k）
4. 减少并发进程数（根据内存动态调整）
5. 每次生成后立即释放内存
6. 交易生成时从文件读取登录记录，避免大字典
"""

from database_config import get_db_config

import os
import csv
import multiprocessing as mp
import random
import time
import psutil
import gc
import tempfile
import pickle
from datetime import datetime, timedelta
import logging
from typing import List, Dict, Tuple, Set
import psycopg2
import numpy as np
from concurrent.futures import ProcessPoolExecutor
from faker import Faker
from faker.providers import BaseProvider
from tqdm import tqdm

# 配置日志
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# 内存优化配置
MEMORY_SAFE_THRESHOLD = 80  # 安全阈值70%
MEMORY_WARNING_THRESHOLD = 90  # 警告阈值80%
RANDOM_POOL_SIZE = 20000  # 减小随机池
BATCH_SIZE = 20000  # 减小批次
WRITE_BUFFER_SIZE = 65536  # 减小写入缓冲


class EnhancedFinancialDataProvider(BaseProvider):
    """增强的金融数据提供者（极致优化版）"""

    transaction_types = {"TRANSFER": {"weight": 100, "name": "转账"}}
    device_distribution = {"Mobile": 70, "PC": 20, "Tablet": 8, "ATM": 2}

    cities_distribution = {
        "北京市": 100, "上海市": 100, "深圳市": 90, "广州市": 85,
        "杭州市": 75, "成都市": 70, "重庆市": 65, "武汉市": 60,
    }

    _cities_list = list(cities_distribution.keys())
    _cities_weights = list(cities_distribution.values())
    _devices_list = list(device_distribution.keys())
    _devices_weights = list(device_distribution.values())

    city_locations = {
        "北京市": ["北京市-朝阳区", "北京市-海淀区", "北京市-丰台区"],
        "上海市": ["上海市-浦东新区", "上海市-静安区", "上海市-徐汇区"],
        "深圳市": ["广东省-深圳市-福田区", "广东省-深圳市-南山区"],
        "广州市": ["广东省-广州市-天河区", "广东省-广州市-越秀区"],
        "杭州市": ["浙江省-杭州市-西湖区", "浙江省-杭州市-滨江区"],
        "成都市": ["四川省-成都市-锦江区", "四川省-成都市-武侯区"],
        "重庆市": ["重庆市-渝中区", "重庆市-江北区"],
        "武汉市": ["湖北省-武汉市-武昌区", "湖北省-武汉市-洪山区"],
    }

    def __init__(self, generator, worker_id=0):
        super().__init__(generator)
        self.worker_id = worker_id
        self.serial_counter = 0

    def transaction_serial_no(self) -> str:
        """流水号生成"""
        timestamp = int(time.time() * 1000000)
        self.serial_counter += 1
        return f"TR{timestamp}{self.worker_id:02d}{self.serial_counter:05d}"

    def weighted_city(self) -> str:
        return random.choices(self._cities_list, weights=self._cities_weights)[0]

    def weighted_device(self) -> str:
        return random.choices(self._devices_list, weights=self._devices_weights)[0]

    def location_for_city(self, city: str) -> str:
        locations = self.city_locations.get(city, [f"{city}-市区"])
        return random.choice(locations)


def check_memory() -> float:
    """检查内存使用率"""
    return psutil.virtual_memory().percent


def generate_logins_worker_optimized(args: Tuple) -> Tuple[str, int, str]:
    """登录数据生成 - 返回临时文件路径而非内存字典"""
    worker_id, chunk_size, output_file, account_ids, suspicious_accounts, progress_queue = args

    fake = Faker("zh_CN")
    provider = EnhancedFinancialDataProvider(fake, worker_id)
    fake.add_provider(provider)

    end_date = datetime.now()
    start_date = end_date - timedelta(days=365)
    date_range_seconds = int((end_date - start_date).total_seconds())

    account_list = list(account_ids)
    suspicious_set = set(suspicious_accounts)

    update_interval = max(500, chunk_size // 100)
    batch_buffer = []

    # 使用临时文件保存登录记录，不在内存中累积
    temp_login_file = tempfile.NamedTemporaryFile(mode='w', delete=False,
                                                  suffix=f'_login_map_{worker_id}.csv',
                                                  encoding='utf-8')
    temp_login_writer = csv.writer(temp_login_file)
    temp_login_writer.writerow(['account_id', 'login_time', 'login_location'])

    generated_count = 0
    login_id = worker_id * chunk_size + 1
    last_update = 0

    # 小随机数池
    random_pool_size = RANDOM_POOL_SIZE
    account_indices = np.random.randint(0, len(account_list), size=random_pool_size)
    time_randoms = np.random.randint(0, date_range_seconds, size=random_pool_size)
    suspicious_randoms = np.random.random(random_pool_size)
    device_randoms = np.random.random(random_pool_size)
    pool_idx = 0

    with open(output_file, "w", newline="", encoding="utf-8", buffering=WRITE_BUFFER_SIZE) as f:
        writer = csv.writer(f)
        writer.writerow([
            "id", "account_id", "login_time", "login_location", "device_type",
            "os_type", "is_suspicious"
        ])

        while generated_count < chunk_size:
            batch_to_generate = min(BATCH_SIZE, chunk_size - generated_count)

            for _ in range(batch_to_generate):
                if pool_idx >= random_pool_size:
                    # 立即释放旧数组
                    del account_indices, time_randoms, suspicious_randoms, device_randoms
                    gc.collect()

                    # 重新生成
                    account_indices = np.random.randint(0, len(account_list), size=random_pool_size)
                    time_randoms = np.random.randint(0, date_range_seconds, size=random_pool_size)
                    suspicious_randoms = np.random.random(random_pool_size)
                    device_randoms = np.random.random(random_pool_size)
                    pool_idx = 0

                account_id = account_list[account_indices[pool_idx]]
                login_time = start_date + timedelta(seconds=int(time_randoms[pool_idx]))

                login_city = provider.weighted_city()
                login_location = provider.location_for_city(login_city)
                device_type = provider.weighted_device()

                if device_type == "Mobile":
                    os_type = "iOS" if device_randoms[pool_idx] < 0.35 else "Android"
                elif device_type == "PC":
                    rand = device_randoms[pool_idx]
                    os_type = "Windows" if rand < 0.8 else ("macOS" if rand < 0.98 else "Linux")
                elif device_type == "Tablet":
                    os_type = "iOS" if device_randoms[pool_idx] < 0.6 else "Android"
                else:
                    os_type = "Embedded"

                is_suspicious = (account_id in suspicious_set and suspicious_randoms[pool_idx] < 0.25)

                batch_buffer.append([
                    login_id, account_id, login_time.isoformat(), login_location,
                    device_type, os_type, is_suspicious
                ])

                # 写入临时登录映射文件
                temp_login_writer.writerow([account_id, login_time.isoformat(), login_location])

                login_id += 1
                generated_count += 1
                pool_idx += 1

                if generated_count - last_update >= update_interval:
                    progress_queue.put(generated_count - last_update)
                    last_update = generated_count

            writer.writerows(batch_buffer)
            batch_buffer.clear()

            # 定期检查内存
            if generated_count % 10000 == 0:
                mem = check_memory()
                if mem > MEMORY_WARNING_THRESHOLD:
                    gc.collect()

        if generated_count > last_update:
            progress_queue.put(generated_count - last_update)

    temp_login_file.close()

    # 清理内存
    del account_indices, time_randoms, suspicious_randoms, device_randoms
    del account_list, suspicious_set, batch_buffer
    gc.collect()

    return output_file, generated_count, temp_login_file.name


def generate_transactions_worker_optimized(args: Tuple) -> Tuple[str, int]:
    """交易数据生成 - 从临时文件读取登录记录"""
    worker_id, chunk_size, output_file, account_ids, suspicious_accounts, login_map_files, progress_queue = args

    fake = Faker("zh_CN")
    provider = EnhancedFinancialDataProvider(fake, worker_id)
    fake.add_provider(provider)

    # 分批次加载登录记录，避免一次性加载
    def load_login_records_lazy(files, target_account):
        """懒加载：只在需要时读取特定账户的登录记录"""
        for file_path in files:
            if not os.path.exists(file_path):
                continue
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    reader = csv.DictReader(f)
                    records = []
                    for row in reader:
                        if row['account_id'] == target_account:
                            records.append({
                                'time': datetime.fromisoformat(row['login_time']),
                                'location': row['login_location']
                            })
                    if records:
                        return records
            except Exception as e:
                logger.warning(f"读取登录文件失败 {file_path}: {e}")
        return None

    account_list = list(account_ids)
    suspicious_set = set(suspicious_accounts)

    update_interval = max(500, chunk_size // 100)
    batch_buffer = []
    generated_count = 0
    transaction_id = worker_id * chunk_size + 1
    last_update = 0

    random_pool_size = RANDOM_POOL_SIZE
    account_indices = np.random.randint(0, len(account_list), size=random_pool_size * 2)
    suspicious_randoms = np.random.random(random_pool_size)
    amount_randoms = np.random.random(random_pool_size)
    pool_idx = 0

    end_date = datetime.now()
    start_date = end_date - timedelta(days=365)
    date_range_seconds = int((end_date - start_date).total_seconds())

    with open(output_file, "w", newline="", encoding="utf-8", buffering=WRITE_BUFFER_SIZE) as f:
        writer = csv.writer(f)
        writer.writerow([
            "id", "from_account_id", "to_account_id", "serial_no", "amount",
            "transaction_type", "status", "trans_time", "is_suspicious", "suspicious_type"
        ])

        while generated_count < chunk_size:
            batch_to_generate = min(BATCH_SIZE, chunk_size - generated_count)

            for _ in range(batch_to_generate):
                if pool_idx >= random_pool_size:
                    del account_indices, suspicious_randoms, amount_randoms
                    gc.collect()

                    account_indices = np.random.randint(0, len(account_list), size=random_pool_size * 2)
                    suspicious_randoms = np.random.random(random_pool_size)
                    amount_randoms = np.random.random(random_pool_size)
                    pool_idx = 0

                from_idx = account_indices[pool_idx * 2]
                to_idx = account_indices[pool_idx * 2 + 1]

                from_account = account_list[from_idx]
                to_account = account_list[to_idx]

                while to_account == from_account:
                    to_idx = random.randint(0, len(account_list) - 1)
                    to_account = account_list[to_idx]

                is_suspicious = from_account in suspicious_set

                # 懒加载登录记录（只在需要时读取）
                login_records = load_login_records_lazy(login_map_files, from_account)

                if login_records:
                    login_record = random.choice(login_records)
                    login_time = login_record['time']
                    minutes_after_login = random.randint(0, 60)
                    trans_time = login_time + timedelta(minutes=minutes_after_login)
                else:
                    random_seconds = random.randint(0, date_range_seconds)
                    trans_time = start_date + timedelta(seconds=random_seconds)

                if is_suspicious and suspicious_randoms[pool_idx] < 1:
                    patterns = ["A", "B", "C", "D", "E", "F"]
                    if amount_randoms[pool_idx] < 0.3:
                        suspicious_type = "".join(random.sample(patterns, k=2))
                    else:
                        suspicious_type = random.choice(patterns)

                    if suspicious_type in ["A", "D"]:
                        amount = round(50000 + amount_randoms[pool_idx] * 250000, 2)
                    elif suspicious_type == "C":
                        amount = round(45000 + amount_randoms[pool_idx] * 4999, 2)
                    else:
                        amount = round(30000 + amount_randoms[pool_idx] * 120000, 2)

                    is_suspicious_flag = True
                else:
                    amount = round(min(np.random.lognormal(7.5, 1.5), 500000), 2)
                    is_suspicious_flag = False
                    suspicious_type = "N"

                serial_no = provider.transaction_serial_no()
                status = "SUCCESS" if random.random() < 0.99 else "FAILED"

                batch_buffer.append([
                    transaction_id, from_account, to_account, serial_no, amount,
                    "TRANSFER", status, trans_time.isoformat(), is_suspicious_flag, suspicious_type
                ])

                transaction_id += 1
                generated_count += 1
                pool_idx += 1

                if generated_count - last_update >= update_interval:
                    progress_queue.put(generated_count - last_update)
                    last_update = generated_count

            writer.writerows(batch_buffer)
            batch_buffer.clear()

            if generated_count % 10000 == 0:
                mem = check_memory()
                if mem > MEMORY_WARNING_THRESHOLD:
                    gc.collect()

        if generated_count > last_update:
            progress_queue.put(generated_count - last_update)

    # 清理
    del account_indices, suspicious_randoms, amount_randoms
    del account_list, suspicious_set, batch_buffer
    gc.collect()

    return output_file, generated_count


class OptimizedFinancialDataGenerator:
    """极致内存优化的数据生成器"""

    def __init__(self, db_config: Dict,
                 transaction_count: int = 100_000,
                 login_count: int = 50_000,
                 suspicious_ratio: float = 0.05):

        self.db_config = db_config
        self.transaction_count = transaction_count
        self.login_count = login_count
        self.suspicious_ratio = suspicious_ratio

        # 根据内存动态调整进程数
        total_memory_gb = psutil.virtual_memory().total / (1024 ** 3)
        if total_memory_gb <= 12:
            self.process_count = min(5, mp.cpu_count())  # 12GB内存最多4进程
        elif total_memory_gb <= 16:
            self.process_count = min(8, mp.cpu_count())
        else:
            self.process_count = min(8, mp.cpu_count())

        logger.info(f"🚀 检测到内存: {total_memory_gb:.1f} GB")
        logger.info(f"⚙️  使用 {self.process_count} 个进程（内存优化模式）")
        logger.info(f"📊 批次大小: {BATCH_SIZE}, 随机池: {RANDOM_POOL_SIZE}")

    def get_account_ids(self) -> Tuple[List[int], Set[int]]:
        """获取账户ID"""
        try:
            conn = psycopg2.connect(**self.db_config)
            cur = conn.cursor()
            cur.execute("SELECT account_no FROM enhanced_account")
            account_ids = [row[0] for row in cur.fetchall()]

            num_suspicious = int(len(account_ids) * self.suspicious_ratio)
            suspicious_accounts = set(random.sample(account_ids, num_suspicious))

            logger.info(f"✅ 获取 {len(account_ids):,} 个账户ID，{len(suspicious_accounts):,} 个可疑账户")
            return account_ids, suspicious_accounts

        except Exception as e:
            logger.error(f"❌ 获取账户ID出错: {e}")
            raise
        finally:
            if 'cur' in locals():
                cur.close()
            if 'conn' in locals():
                conn.close()

    def create_financial_tables(self):
        """创建表结构"""
        try:
            conn = psycopg2.connect(**self.db_config)
            cur = conn.cursor()

            cur.execute("DROP TABLE IF EXISTS transactions1 CASCADE; DROP TABLE IF EXISTS logins CASCADE;")
            conn.commit()

            cur.execute("""
            CREATE TABLE transactions1 (
                id BIGINT PRIMARY KEY,
                from_account_id VARCHAR(25) NOT NULL,
                to_account_id VARCHAR(25) NOT NULL,
                serial_no VARCHAR(50) UNIQUE NOT NULL,
                amount DECIMAL(15,2) NOT NULL,
                transaction_type VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL,
                trans_time TIMESTAMP NOT NULL,
                is_suspicious BOOLEAN DEFAULT FALSE,
                suspicious_type VARCHAR(10) DEFAULT 'N',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE logins (
                id BIGINT PRIMARY KEY,
                account_id VARCHAR(25) NOT NULL,
                login_time TIMESTAMP NOT NULL,
                login_location VARCHAR(100) NOT NULL,
                device_type VARCHAR(20) NOT NULL,
                os_type VARCHAR(20) NOT NULL,
                is_suspicious BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """)

            conn.commit()
            logger.info("✅ 数据表创建完成")

        except Exception as e:
            logger.error(f"❌ 创建表出错: {e}")
            raise
        finally:
            if 'cur' in locals():
                cur.close()
            if 'conn' in locals():
                conn.close()

    def generate_logins_parallel(self, account_ids: List[int],
                                 suspicious_accounts: Set[int]) -> List[str]:
        """并行生成登录数据 - 返回临时文件列表"""
        logger.info(f"📊 开始生成 {self.login_count:,} 条登录记录...")

        output_dir = "generated_financial_data/login"
        os.makedirs(output_dir, exist_ok=True)

        chunk_size = self.login_count // self.process_count
        tasks = []

        manager = mp.Manager()
        progress_queue = manager.Queue()

        for i in range(self.process_count):
            count = chunk_size if i < self.process_count - 1 else (self.login_count - i * chunk_size)
            if count > 0:
                output_file = f"{output_dir}/logins_{i:02d}.csv"
                tasks.append((i, count, output_file, account_ids, suspicious_accounts, progress_queue))

        start_time = time.time()
        login_map_files = []

        with tqdm(total=self.login_count, desc="生成登录", unit="条",
                  bar_format='{l_bar}{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}, {rate_fmt} {postfix}]',
                  mininterval=1.0) as pbar:

            with ProcessPoolExecutor(max_workers=self.process_count) as executor:
                futures = [executor.submit(generate_logins_worker_optimized, task) for task in tasks]

                completed_count = 0
                last_mem_check = time.time()

                while completed_count < self.login_count:
                    try:
                        increment = progress_queue.get(timeout=0.5)
                        completed_count += increment
                        pbar.update(increment)

                        # 降低内存检查频率
                        if time.time() - last_mem_check > 2:
                            elapsed = time.time() - start_time
                            speed = completed_count / elapsed if elapsed > 0 else 0
                            mem_usage = check_memory()
                            pbar.set_postfix({
                                '速度': f'{speed:,.0f}条/秒',
                                '内存': f'{mem_usage:.1f}%'
                            })
                            last_mem_check = time.time()

                            # 内存告警
                            if mem_usage > MEMORY_WARNING_THRESHOLD:
                                logger.warning(f"⚠️  内存使用过高: {mem_usage:.1f}%")
                                gc.collect()

                    except:
                        if all(f.done() for f in futures):
                            break

                for future in futures:
                    file_path, count, temp_file = future.result()
                    login_map_files.append(temp_file)

        generation_time = time.time() - start_time
        logger.info(f"✅ 登录数据生成完成，耗时: {generation_time:.2f}秒")
        logger.info(f"📋 登录映射文件: {len(login_map_files)} 个")

        # 强制垃圾回收
        gc.collect()

        completed_files = [f"{output_dir}/logins_{i:02d}.csv" for i in range(self.process_count)]
        self._bulk_import_logins(completed_files)

        return login_map_files

    def generate_transactions_parallel(self, account_ids: List[int],
                                       suspicious_accounts: Set[int],
                                       login_map_files: List[str]):
        """并行生成交易数据"""
        logger.info(f"📊 开始生成 {self.transaction_count:,} 条交易记录...")

        output_dir = "generated_financial_data/transactions"
        os.makedirs(output_dir, exist_ok=True)

        chunk_size = self.transaction_count // self.process_count
        tasks = []

        manager = mp.Manager()
        progress_queue = manager.Queue()

        for i in range(self.process_count):
            count = chunk_size if i < self.process_count - 1 else (self.transaction_count - i * chunk_size)
            if count > 0:
                output_file = f"{output_dir}/transactions_{i:02d}.csv"
                tasks.append((i, count, output_file, account_ids, suspicious_accounts, login_map_files, progress_queue))

        start_time = time.time()

        with tqdm(total=self.transaction_count, desc="生成交易", unit="条",
                  bar_format='{l_bar}{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}, {rate_fmt} {postfix}]',
                  mininterval=1.0) as pbar:

            with ProcessPoolExecutor(max_workers=self.process_count) as executor:
                futures = [executor.submit(generate_transactions_worker_optimized, task) for task in tasks]

                completed_count = 0
                last_mem_check = time.time()

                while completed_count < self.transaction_count:
                    try:
                        increment = progress_queue.get(timeout=0.5)
                        completed_count += increment
                        pbar.update(increment)

                        if time.time() - last_mem_check > 2:
                            elapsed = time.time() - start_time
                            speed = completed_count / elapsed if elapsed > 0 else 0
                            mem_usage = check_memory()
                            pbar.set_postfix({
                                '速度': f'{speed:,.0f}条/秒',
                                '内存': f'{mem_usage:.1f}%'
                            })
                            last_mem_check = time.time()

                            if mem_usage > MEMORY_WARNING_THRESHOLD:
                                logger.warning(f"⚠️  内存使用过高: {mem_usage:.1f}%")
                                gc.collect()

                    except:
                        if all(f.done() for f in futures):
                            break

                for future in futures:
                    future.result()

        generation_time = time.time() - start_time
        logger.info(f"✅ 交易数据生成完成，耗时: {generation_time:.2f}秒")

        # 清理临时文件
        for temp_file in login_map_files:
            try:
                if os.path.exists(temp_file):
                    os.remove(temp_file)
            except Exception as e:
                logger.warning(f"清理临时文件失败 {temp_file}: {e}")

        gc.collect()

        completed_files = [f"{output_dir}/transactions_{i:02d}.csv" for i in range(self.process_count)]
        self._bulk_import_transactions(completed_files)

    def _bulk_import_logins(self, csv_files: List[str]):
        """批量导入登录数据"""
        logger.info("📥 开始导入登录数据...")

        try:
            conn = psycopg2.connect(**self.db_config)
            cur = conn.cursor()

            start_time = time.time()

            with tqdm(total=len(csv_files), desc="导入登录文件", unit="文件") as pbar:
                for csv_file in csv_files:
                    if os.path.exists(csv_file):
                        with open(csv_file, "r", encoding="utf-8") as f:
                            next(f)
                            cur.copy_expert("""
                                COPY logins (id, account_id, login_time, login_location, 
                                           device_type, os_type, is_suspicious)
                                FROM STDIN WITH CSV
                            """, f)
                            conn.commit()
                        pbar.update(1)

                        # 每导入一个文件就回收内存
                        gc.collect()

            logger.info("📇 创建索引...")
            cur.execute("""
                CREATE INDEX IF NOT EXISTS idx_logins_account ON logins(account_id);
                CREATE INDEX IF NOT EXISTS idx_logins_time ON logins(login_time);
            """)
            conn.commit()

            import_time = time.time() - start_time
            logger.info(f"✅ 登录数据导入完成，耗时 {import_time:.2f}秒")

        except Exception as e:
            logger.error(f"❌ 导入出错: {e}")
            if 'conn' in locals():
                conn.rollback()
            raise
        finally:
            if 'cur' in locals():
                cur.close()
            if 'conn' in locals():
                conn.close()

    def _bulk_import_transactions(self, csv_files: List[str]):
        """批量导入交易数据"""
        logger.info("📥 开始导入交易数据...")

        try:
            conn = psycopg2.connect(**self.db_config)
            cur = conn.cursor()

            start_time = time.time()

            with tqdm(total=len(csv_files), desc="导入交易文件", unit="文件") as pbar:
                for csv_file in csv_files:
                    if os.path.exists(csv_file):
                        with open(csv_file, "r", encoding="utf-8") as f:
                            next(f)
                            cur.copy_expert("""
                                COPY transactions1 (id, from_account_id, to_account_id, serial_no, 
                                                 amount, transaction_type, status, trans_time, 
                                                 is_suspicious, suspicious_type)
                                FROM STDIN WITH CSV
                            """, f)
                            conn.commit()
                        pbar.update(1)
                        gc.collect()

            logger.info("📇 创建索引...")
            cur.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_from ON transactions1(from_account_id);
                CREATE INDEX IF NOT EXISTS idx_transactions_to ON transactions1(to_account_id);
                CREATE INDEX IF NOT EXISTS idx_transactions_time ON transactions1(trans_time);
                CREATE INDEX IF NOT EXISTS idx_transactions_suspicious ON transactions1(is_suspicious);
            """)
            conn.commit()

            import_time = time.time() - start_time
            logger.info(f"✅ 交易数据导入完成，耗时 {import_time:.2f}秒")

            # 更新可疑账户状态
            self._update_suspicious_account_status(cur, conn)

        except Exception as e:
            logger.error(f"❌ 导入出错: {e}")
            if 'conn' in locals():
                conn.rollback()
            raise
        finally:
            if 'cur' in locals():
                cur.close()
            if 'conn' in locals():
                conn.close()
            gc.collect()

    def _update_suspicious_account_status(self, cur, conn):
        """更新可疑账户状态"""
        logger.info("🔄 正在更新可疑账户状态...")

        try:
            cur.execute("""
                SELECT COUNT(DISTINCT account_no) 
                FROM enhanced_account 
                WHERE account_no IN (
                    SELECT DISTINCT from_account_id FROM transactions1 WHERE is_suspicious = true
                    UNION
                    SELECT DISTINCT to_account_id FROM transactions1 WHERE is_suspicious = true
                )
            """)
            affected_count = cur.fetchone()[0]

            if affected_count == 0:
                logger.info("⚠️ 没有找到涉及可疑交易的账户")
                return

            # 分批更新，避免锁表时间过长
            cur.execute("""
                UPDATE enhanced_account 
                SET status = 0, updated_at = CURRENT_TIMESTAMP
                WHERE account_no IN (
                    SELECT DISTINCT from_account_id FROM transactions1 WHERE is_suspicious = true
                )
            """)
            from_count = cur.rowcount
            conn.commit()

            cur.execute("""
                UPDATE enhanced_account 
                SET status = 0, updated_at = CURRENT_TIMESTAMP
                WHERE account_no IN (
                    SELECT DISTINCT to_account_id FROM transactions1 WHERE is_suspicious = true
                )
            """)
            to_count = cur.rowcount
            conn.commit()

            logger.info(f"✅ 账户状态更新完成:")
            logger.info(f"   - 可疑转出账户: {from_count:,} 个")
            logger.info(f"   - 可疑转入账户: {to_count:,} 个")
            logger.info(f"   - 总计受影响: {affected_count:,} 个")

        except Exception as e:
            logger.error(f"❌ 更新账户状态失败: {e}")
            conn.rollback()
            raise


def main():
    """主函数"""
    db_config = get_db_config("formdata")

    # 根据12GB内存调整规模
    # 建议：12GB内存下，单次生成不超过5000万交易和2500万登录
    transaction_count = 200_000  # 5000万交易
    login_count = 10_000  # 2500万登录
    suspicious_ratio = 0.1

    logger.info("=" * 60)
    logger.info("🚀 极致内存优化金融数据生成器（12GB内存版）")
    logger.info("=" * 60)
    logger.info(f"💾 系统内存: {psutil.virtual_memory().total / 1024 ** 3:.1f} GB")
    logger.info(f"📈 目标交易数: {transaction_count:,}")
    logger.info(f"📈 目标登录数: {login_count:,}")
    logger.info(f"⚠️  可疑比例: {suspicious_ratio:.1%}")
    logger.info(f"⚙️  批次大小: {BATCH_SIZE}, 随机池: {RANDOM_POOL_SIZE}")
    logger.info("=" * 60)

    generator = OptimizedFinancialDataGenerator(
        db_config, transaction_count, login_count, suspicious_ratio
    )

    try:
        total_start = time.time()

        generator.create_financial_tables()
        account_ids, suspicious_accounts = generator.get_account_ids()

        if not account_ids:
            logger.error("❌ 未获取到账户ID")
            return

        # 生成登录数据，返回临时文件列表
        login_map_files = generator.generate_logins_parallel(account_ids, suspicious_accounts)

        # 释放内存
        logger.info("🧹 释放内存...")
        gc.collect()

        # 生成交易数据
        generator.generate_transactions_parallel(account_ids, suspicious_accounts, login_map_files)

        total_time = time.time() - total_start

        logger.info("=" * 60)
        logger.info(f"✅ 全部完成！总耗时: {total_time:.2f}秒 ({total_time / 60:.1f}分钟)")
        logger.info(f"⚡ 平均速度: {(transaction_count + login_count) / total_time:,.0f} 条/秒")
        logger.info(f"📊 交易速度: {transaction_count / total_time:,.0f} 条/秒")
        logger.info(f"📊 登录速度: {login_count / total_time:,.0f} 条/秒")
        logger.info(f"💾 最终内存使用: {check_memory():.1f}%")
        logger.info("=" * 60)

    except Exception as e:
        logger.error(f"❌ 程序执行出错: {e}")
        import traceback
        traceback.print_exc()
        raise
    finally:
        # 最终清理
        gc.collect()


if __name__ == "__main__":
    main()