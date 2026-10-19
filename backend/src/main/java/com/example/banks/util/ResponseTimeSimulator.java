package com.example.banks.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 响应时间模拟器
 * 用于压测接口模拟真实的数据库查询延迟
 */
public class ResponseTimeSimulator {

    private static final Logger logger = LoggerFactory.getLogger(ResponseTimeSimulator.class);

    /**
     * 模拟快速查询（简单查询）
     * 延迟范围：50-150ms
     */
    public static void simulateFastQuery() {
        simulateDelay(50, 150);
    }

    /**
     * 模拟普通查询（中等复杂度）
     * 延迟范围：100-300ms
     */
    public static void simulateNormalQuery() {
        simulateDelay(100, 300);
    }

    /**
     * 模拟慢查询（复杂查询、聚合统计）
     * 延迟范围：200-500ms
     */
    public static void simulateSlowQuery() {
        simulateDelay(200, 500);
    }

    /**
     * 模拟超慢查询（大数据分析）
     * 延迟范围：500-1500ms
     */
    public static void simulateVerySlowQuery() {
        simulateDelay(500, 1500);
    }

    /**
     * 自定义延迟范围
     * @param minMs 最小延迟（毫秒）
     * @param maxMs 最大延迟（毫秒）
     */
    public static void simulateDelay(int minMs, int maxMs) {
        if (minMs < 0 || maxMs < minMs) {
            logger.warn("无效的延迟参数: min={}, max={}", minMs, maxMs);
            return;
        }

        int delay = ThreadLocalRandom.current().nextInt(minMs, maxMs + 1);

        try {
            logger.debug("模拟响应延迟: {}ms", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            logger.error("延迟模拟被中断", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模拟网络抖动（偶发性延迟）
     * 90%概率快速响应，10%概率慢速响应
     */
    public static void simulateNetworkJitter() {
        Random random = ThreadLocalRandom.current();
        if (random.nextDouble() < 0.9) {
            // 90% 快速响应
            simulateDelay(30, 100);
        } else {
            // 10% 慢速响应（网络抖动）
            simulateDelay(500, 1000);
        }
    }

    /**
     * 根据数据量模拟响应时间
     * @param dataSize 数据量大小
     */
    public static void simulateByDataSize(int dataSize) {
        if (dataSize < 100) {
            simulateFastQuery();
        } else if (dataSize < 1000) {
            simulateNormalQuery();
        } else if (dataSize < 10000) {
            simulateSlowQuery();
        } else {
            simulateVerySlowQuery();
        }
    }

    /**
     * 模拟真实查询（压测专用）
     * 85%概率：100-1000ms（正常响应）
     * 15%概率：1000-2000ms（慢查询）
     */
    public static void simulateRealisticQuery() {
        Random random = ThreadLocalRandom.current();
        if (random.nextDouble() < 0.85) {
            // 85% 正常响应
            simulateDelay(100, 1000);
        } else {
            // 15% 慢查询
            simulateDelay(1000, 2000);
        }
    }
}

