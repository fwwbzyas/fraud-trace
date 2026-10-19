#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
金融电诈数据追查系统 - 登录数据生成器（独立版）
专注于登录数据的高效生成和导入
"""

from database_config import get_db_config

import os
import csv
import multiprocessing as mp
import random
import time
import psutil
import gc
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
MEMORY_WARNING_THRESHOLD = 95
RANDOM_POOL_SIZE = 5000
BATCH_SIZE = 5000
WRITE_BUFFER_SIZE = 65536


class EnhancedFinancialDataProvider(BaseProvider):
    """增强的金融数据提供者"""

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


def generate_logins_worker(args: Tuple) -> Tuple[str, int]:
    """登录数据生成工作进程"""
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

    generated_count = 0
    login_id = worker_id * chunk_size + 1
    last_update = 0

    # 随机数池
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
                    del account_indices, time_randoms, suspicious_randoms, device_randoms
                    gc.collect()

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

                login_id += 1
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

    del account_indices, time_randoms, suspicious_randoms, device_randoms
    del account_list, suspicious_set, batch_buffer
    gc.collect()

    return output_file, generated_count


class LoginDataGenerator:
    """登录数据生成器"""

    def __init__(self, db_config: Dict, login_count: int = 50_000, suspicious_ratio: float = 0.05):
        self.db_config = db_config
        self.login_count = login_count
        self.suspicious_ratio = suspicious_ratio

        total_memory_gb = psutil.virtual_memory().total / (1024 ** 3)
        if total_memory_gb <= 12:
            self.process_count = min(6, mp.cpu_count())
        elif total_memory_gb <= 16:
            self.process_count = min(8, mp.cpu_count())
        else:
            self.process_count = min(10, mp.cpu_count())

        logger.info(f"🚀 检测到内存: {total_memory_gb:.1f} GB")
        logger.info(f"⚙️  使用 {self.process_count} 个进程")

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

    def create_login_table(self):
        """创建登录表"""
        try:
            conn = psycopg2.connect(**self.db_config)
            cur = conn.cursor()

            cur.execute("DROP TABLE IF EXISTS logins CASCADE;")
            conn.commit()

            cur.execute("""
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
            logger.info("✅ 登录表创建完成")

        except Exception as e:
            logger.error(f"❌ 创建表出错: {e}")
            raise
        finally:
            if 'cur' in locals():
                cur.close()
            if 'conn' in locals():
                conn.close()

    def generate_logins_parallel(self, account_ids: List[int], suspicious_accounts: Set[int]):
        """并行生成登录数据"""
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

        with tqdm(total=self.login_count, desc="生成登录", unit="条",
                  bar_format='{l_bar}{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}, {rate_fmt} {postfix}]',
                  mininterval=1.0) as pbar:

            with ProcessPoolExecutor(max_workers=self.process_count) as executor:
                futures = [executor.submit(generate_logins_worker, task) for task in tasks]

                completed_count = 0
                last_mem_check = time.time()

                while completed_count < self.login_count:
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
        logger.info(f"✅ 登录数据生成完成，耗时: {generation_time:.2f}秒")

        gc.collect()

        completed_files = [f"{output_dir}/logins_{i:02d}.csv" for i in range(self.process_count)]
        self._bulk_import_logins(completed_files)

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
                        gc.collect()

            logger.info("📇 创建索引...")
            cur.execute("""
                CREATE INDEX IF NOT EXISTS idx_logins_account ON logins(account_id);
                CREATE INDEX IF NOT EXISTS idx_logins_time ON logins(login_time);
                CREATE INDEX IF NOT EXISTS idx_logins_account_time ON logins(account_id, login_time);
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


def main():
    """主函数"""
    db_config = get_db_config("formdata")

    login_count = 100_000_000  # 1亿登录
    suspicious_ratio = 0.1

    logger.info("=" * 60)
    logger.info("🚀 登录数据生成器")
    logger.info("=" * 60)
    logger.info(f"💾 系统内存: {psutil.virtual_memory().total / 1024 ** 3:.1f} GB")
    logger.info(f"📈 目标登录数: {login_count:,}")
    logger.info(f"⚠️  可疑比例: {suspicious_ratio:.1%}")
    logger.info("=" * 60)

    generator = LoginDataGenerator(db_config, login_count, suspicious_ratio)

    try:
        total_start = time.time()

        generator.create_login_table()
        account_ids, suspicious_accounts = generator.get_account_ids()

        if not account_ids:
            logger.error("❌ 未获取到账户ID")
            return

        generator.generate_logins_parallel(account_ids, suspicious_accounts)

        total_time = time.time() - total_start

        logger.info("=" * 60)
        logger.info(f"✅ 登录数据生成完成！总耗时: {total_time:.2f}秒 ({total_time / 60:.1f}分钟)")
        logger.info(f"⚡ 平均速度: {login_count / total_time:,.0f} 条/秒")
        logger.info(f"💾 最终内存使用: {check_memory():.1f}%")
        logger.info("=" * 60)

    except Exception as e:
        logger.error(f"❌ 程序执行出错: {e}")
        import traceback
        traceback.print_exc()
        raise
    finally:
        gc.collect()


if __name__ == "__main__":
    main()