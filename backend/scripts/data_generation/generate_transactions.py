#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
金融电诈数据追查系统 - 交易数据生成器（终极优化版）
核心优化：每个进程独立从数据库加载自己的登录缓存，避免共享内存瓶颈
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
RANDOM_POOL_SIZE = 10000
BATCH_SIZE = 10000
WRITE_BUFFER_SIZE = 65536


class EnhancedFinancialDataProvider(BaseProvider):
    """增强的金融数据提供者"""

    def __init__(self, generator, worker_id=0):
        super().__init__(generator)
        self.worker_id = worker_id
        self.serial_counter = 0

    def transaction_serial_no(self) -> str:
        """流水号生成"""
        timestamp = int(time.time() * 1000000)
        self.serial_counter += 1
        return f"TR{timestamp}{self.worker_id:02d}{self.serial_counter:05d}"


def check_memory() -> float:
    """检查内存使用率"""
    return psutil.virtual_memory().percent


def load_login_cache_for_worker(db_config: Dict, account_sample: List[int], worker_id: int) -> Dict:
    """
    每个工作进程独立加载登录缓存（避免共享内存瓶颈）
    策略：只加载本进程可能用到的账户
    """
    try:
        conn = psycopg2.connect(**db_config)
        cur = conn.cursor()

        login_cache = {}
        batch_size = 5000

        logger.info(f"[Worker {worker_id}] 开始加载 {len(account_sample):,} 个账户的登录记录...")

        for i in range(0, len(account_sample), batch_size):
            batch_accounts = account_sample[i:i + batch_size]

            placeholders = ','.join(['%s'] * len(batch_accounts))
            query = f"""
                SELECT account_id, login_time, login_location
                FROM logins
                WHERE account_id IN ({placeholders})
                ORDER BY account_id, login_time
                LIMIT 100000
            """

            cur.execute(query, batch_accounts)
            rows = cur.fetchall()

            for account_id, login_time, login_location in rows:
                if account_id not in login_cache:
                    login_cache[account_id] = []

                login_cache[account_id].append({
                    'time': login_time,
                    'location': login_location
                })

            # 控制内存
            if i % (batch_size * 5) == 0:
                mem = check_memory()
                if mem > 80:
                    logger.warning(f"[Worker {worker_id}] 内存使用 {mem:.1f}%，停止加载更多缓存")
                    break

        total_records = sum(len(records) for records in login_cache.values())
        logger.info(f"[Worker {worker_id}] 登录缓存加载完成：{len(login_cache):,} 个账户，{total_records:,} 条记录")

        return login_cache

    except Exception as e:
        logger.error(f"[Worker {worker_id}] 加载登录缓存失败: {e}")
        return {}
    finally:
        if 'cur' in locals():
            cur.close()
        if 'conn' in locals():
            conn.close()


def generate_transactions_worker(args: Tuple) -> Tuple[str, int]:
    """交易数据生成工作进程（独立加载登录缓存）"""
    worker_id, chunk_size, output_file, account_ids, suspicious_accounts, db_config, progress_queue = args

    # 每个进程独立加载自己需要的登录缓存
    account_list = list(account_ids)

    # 随机采样30%账户进行缓存（每个进程独立采样）
    cache_size = int(len(account_list) * 0.3)
    account_sample = random.sample(account_list, cache_size)

    login_cache = load_login_cache_for_worker(db_config, account_sample, worker_id)

    # 开始生成交易
    fake = Faker("zh_CN")
    provider = EnhancedFinancialDataProvider(fake, worker_id)
    fake.add_provider(provider)

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

                # 使用缓存的登录记录
                if from_account in login_cache and login_cache[from_account]:
                    login_record = random.choice(login_cache[from_account])
                    login_time = login_record['time']
                    minutes_after_login = random.randint(0, 60)
                    trans_time = login_time + timedelta(minutes=minutes_after_login)
                else:
                    # 如果缓存中没有，随机生成时间
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
    del login_cache, account_indices, suspicious_randoms, amount_randoms
    del account_list, suspicious_set, batch_buffer
    gc.collect()

    logger.info(f"[Worker {worker_id}] 完成 {generated_count:,} 条交易生成")
    return output_file, generated_count


class TransactionDataGenerator:
    """交易数据生成器"""

    def __init__(self, db_config: Dict, transaction_count: int = 100_000, suspicious_ratio: float = 0.05):
        self.db_config = db_config
        self.transaction_count = transaction_count
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

    def verify_login_table(self):
        """验证登录表是否存在"""
        try:
            conn = psycopg2.connect(**self.db_config)
            cur = conn.cursor()

            cur.execute("""
                SELECT COUNT(*) FROM information_schema.tables 
                WHERE table_name = 'logins'
            """)
            table_exists = cur.fetchone()[0] > 0

            if not table_exists:
                logger.error("❌ 登录表不存在！请先运行 generate_logins.py")
                return False

            cur.execute("SELECT COUNT(*) FROM logins")
            login_count = cur.fetchone()[0]

            if login_count == 0:
                logger.error("❌ 登录表为空！请先运行 generate_logins.py")
                return False

            logger.info(f"✅ 登录表验证通过，共 {login_count:,} 条记录")
            return True

        except Exception as e:
            logger.error(f"❌ 验证登录表出错: {e}")
            return False
        finally:
            if 'cur' in locals():
                cur.close()
            if 'conn' in locals():
                conn.close()

    def create_transaction_table(self):
        """创建交易表"""
        try:
            conn = psycopg2.connect(**self.db_config)
            cur = conn.cursor()

            cur.execute("DROP TABLE IF EXISTS transactions1 CASCADE;")
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
            """)

            conn.commit()
            logger.info("✅ 交易表创建完成")

        except Exception as e:
            logger.error(f"❌ 创建表出错: {e}")
            raise
        finally:
            if 'cur' in locals():
                cur.close()
            if 'conn' in locals():
                conn.close()

    def generate_transactions_parallel(self, account_ids: List[int], suspicious_accounts: Set[int]):
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
                # 传递db_config而不是共享字典
                tasks.append((i, count, output_file, account_ids, suspicious_accounts, self.db_config, progress_queue))

        start_time = time.time()

        with tqdm(total=self.transaction_count, desc="生成交易", unit="条",
                  bar_format='{l_bar}{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}, {rate_fmt} {postfix}]',
                  mininterval=1.0) as pbar:

            with ProcessPoolExecutor(max_workers=self.process_count) as executor:
                futures = [executor.submit(generate_transactions_worker, task) for task in tasks]

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

        gc.collect()

        completed_files = [f"{output_dir}/transactions_{i:02d}.csv" for i in range(self.process_count)]
        self._bulk_import_transactions(completed_files)

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
                UPDATE enhanced_account 
                SET status = 0, updated_at = CURRENT_TIMESTAMP
                WHERE account_no IN (
                    SELECT DISTINCT from_account_id FROM transactions1 WHERE is_suspicious = true
                    UNION
                    SELECT DISTINCT to_account_id FROM transactions1 WHERE is_suspicious = true
                )
            """)
            affected_count = cur.rowcount
            conn.commit()

            logger.info(f"✅ 账户状态更新完成，受影响账户: {affected_count:,} 个")

        except Exception as e:
            logger.error(f"❌ 更新账户状态失败: {e}")
            conn.rollback()
            raise


def main():
    """主函数"""
    db_config = get_db_config("formdata")

    transaction_count = 200_000_000  # 2亿交易
    suspicious_ratio = 0.1

    logger.info("=" * 60)
    logger.info("🚀 交易数据生成器（终极优化版）")
    logger.info("=" * 60)
    logger.info(f"💾 系统内存: {psutil.virtual_memory().total / 1024 ** 3:.1f} GB")
    logger.info(f"📈 目标交易数: {transaction_count:,}")
    logger.info(f"⚠️  可疑比例: {suspicious_ratio:.1%}")
    logger.info("=" * 60)

    generator = TransactionDataGenerator(db_config, transaction_count, suspicious_ratio)

    try:
        total_start = time.time()

        # 验证登录表
        if not generator.verify_login_table():
            logger.error("程序终止：请先运行 generate_logins.py 生成登录数据")
            return

        generator.create_transaction_table()
        account_ids, suspicious_accounts = generator.get_account_ids()

        if not account_ids:
            logger.error("❌ 未获取到账户ID")
            return

        logger.info("🚀 开始并行生成交易数据（每个进程独立加载登录缓存）...")
        generator.generate_transactions_parallel(account_ids, suspicious_accounts)

        total_time = time.time() - total_start

        logger.info("=" * 60)
        logger.info(f"✅ 交易数据生成完成！总耗时: {total_time:.2f}秒 ({total_time / 60:.1f}分钟)")
        logger.info(f"⚡ 平均速度: {transaction_count / total_time:,.0f} 条/秒")
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