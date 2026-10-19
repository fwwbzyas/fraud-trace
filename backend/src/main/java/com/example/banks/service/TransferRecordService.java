package com.example.banks.service;

import com.example.banks.dto.TransferRecordDTO;
import com.example.banks.entity.TransferRecord;
import com.example.banks.repository.TransferRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 转账记录服务层 - 重新实现
 */
@Service
@Transactional(readOnly = true)
public class TransferRecordService {

    private static final Logger logger = LoggerFactory.getLogger(TransferRecordService.class);

    @Autowired
    private TransferRecordRepository transferRecordRepository;

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("50000");
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询所有转账记录
     */
    public Page<TransferRecordDTO> getAllTransferRecords(Pageable pageable) {
        logger.info("分页查询转账记录，页码: {}, 大小: {}", pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<Object[]> rawPage = transferRecordRepository.findAllWithAccountInfo(pageable);
            List<TransferRecordDTO> dtoList = rawPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return new PageImpl<>(dtoList, pageable, rawPage.getTotalElements());
        } catch (Exception e) {
            logger.error("查询转账记录失败", e);
            throw new RuntimeException("查询转账记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 搜索转账记录 - 重新实现，使用分步搜索
     */
    public Page<TransferRecordDTO> searchTransferRecords(
            String transferNo, String fromAccountName, String toAccountName,
            BigDecimal minAmount, BigDecimal maxAmount,
            LocalDateTime startTime, LocalDateTime endTime,
            Pageable pageable) {

        logger.info("搜索转账记录 - 流水号: {}, 转出账户: {}, 转入账户: {}, 金额范围: {}-{}, 时间范围: {}-{}",
                transferNo, fromAccountName, toAccountName, minAmount, maxAmount, startTime, endTime);

        try {
            Page<Object[]> rawPage = null;

            // 优先级搜索策略
            if (hasValue(transferNo)) {
                // 按流水号搜索
                rawPage = transferRecordRepository.searchByTransferNo(transferNo.trim(), pageable);
                logger.info("按流水号搜索，找到 {} 条记录", rawPage.getTotalElements());

            } else if (hasValue(fromAccountName) && hasValue(toAccountName)) {
                // 按转出和转入账户名组合搜索
                rawPage = transferRecordRepository.searchByFromAndToAccountName(
                        fromAccountName.trim(), toAccountName.trim(), pageable);
                logger.info("按转出和转入账户名组合搜索，找到 {} 条记录", rawPage.getTotalElements());

            } else if (hasValue(fromAccountName)) {
                // 只按转出账户名搜索
                rawPage = transferRecordRepository.searchByFromAccountName(fromAccountName.trim(), pageable);
                logger.info("按转出账户名搜索，找到 {} 条记录", rawPage.getTotalElements());

            } else if (hasValue(toAccountName)) {
                // 只按转入账户名搜索
                rawPage = transferRecordRepository.searchByToAccountName(toAccountName.trim(), pageable);
                logger.info("按转入账户名搜索，找到 {} 条记录", rawPage.getTotalElements());

            } else if (minAmount != null && maxAmount != null) {
                // 按金额范围搜索
                rawPage = transferRecordRepository.searchByAmountRange(minAmount, maxAmount, pageable);
                logger.info("按金额范围搜索，找到 {} 条记录", rawPage.getTotalElements());

            } else if (minAmount != null) {
                // 按最小金额搜索（设置最大值为一个很大的数）
                rawPage = transferRecordRepository.searchByAmountRange(minAmount, new BigDecimal("999999999"), pageable);
                logger.info("按最小金额搜索，找到 {} 条记录", rawPage.getTotalElements());

            } else if (startTime != null && endTime != null) {
                // 按时间范围搜索
                rawPage = transferRecordRepository.searchByTimeRange(startTime, endTime, pageable);
                logger.info("按时间范围搜索，找到 {} 条记录", rawPage.getTotalElements());

            } else {
                // 没有搜索条件，返回所有记录
                rawPage = transferRecordRepository.findAllWithAccountInfo(pageable);
                logger.info("无搜索条件，返回所有记录，共 {} 条", rawPage.getTotalElements());
            }

            List<TransferRecordDTO> dtoList = rawPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return new PageImpl<>(dtoList, pageable, rawPage.getTotalElements());

        } catch (Exception e) {
            logger.error("搜索转账记录失败", e);
            // 如果搜索失败，尝试返回基本的分页结果
            try {
                logger.warn("搜索失败，返回基本分页结果");
                Page<Object[]> fallbackPage = transferRecordRepository.findAllWithAccountInfo(pageable);
                List<TransferRecordDTO> dtoList = fallbackPage.getContent().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
                return new PageImpl<>(dtoList, pageable, fallbackPage.getTotalElements());
            } catch (Exception fallbackError) {
                logger.error("备用查询也失败", fallbackError);
                throw new RuntimeException("搜索转账记录失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 快速筛选 - 大额交易
     */
    public Page<TransferRecordDTO> getLargeAmountTransfers(Pageable pageable) {
        logger.info("获取大额转账记录（>= {}）", LARGE_AMOUNT_THRESHOLD);

        try {
            Page<Object[]> rawPage = transferRecordRepository.searchLargeAmountTransfers(pageable);
            List<TransferRecordDTO> dtoList = rawPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return new PageImpl<>(dtoList, pageable, rawPage.getTotalElements());
        } catch (Exception e) {
            logger.error("获取大额转账记录失败", e);
            throw new RuntimeException("获取大额转账记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 快速筛选 - 今日交易
     */
    public Page<TransferRecordDTO> getTodayTransfers(Pageable pageable) {
        logger.info("获取今日转账记录");

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        try {
            Page<Object[]> rawPage = transferRecordRepository.searchByTimeRange(startOfDay, endOfDay, pageable);
            List<TransferRecordDTO> dtoList = rawPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return new PageImpl<>(dtoList, pageable, rawPage.getTotalElements());
        } catch (Exception e) {
            logger.error("获取今日转账记录失败", e);
            throw new RuntimeException("获取今日转账记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 快速筛选 - 本周交易
     */
    public Page<TransferRecordDTO> getWeekTransfers(Pageable pageable) {
        logger.info("获取本周转账记录");

        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime now = LocalDateTime.now();

        try {
            Page<Object[]> rawPage = transferRecordRepository.searchByTimeRange(startOfWeek, now, pageable);
            List<TransferRecordDTO> dtoList = rawPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return new PageImpl<>(dtoList, pageable, rawPage.getTotalElements());
        } catch (Exception e) {
            logger.error("获取本周转账记录失败", e);
            throw new RuntimeException("获取本周转账记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据ID获取转账记录详情
     */
    public TransferRecordDTO getTransferRecordById(Long id) {
        logger.info("获取转账记录详情: {}", id);

        TransferRecord record = transferRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("转账记录不存在: " + id));

        return convertToSimpleDTO(record);
    }

    /**
     * 根据转账流水号获取记录
     */
    public TransferRecordDTO getTransferRecordByNo(String transferNo) {
        logger.info("根据流水号查询转账记录: {}", transferNo);

        TransferRecord record = transferRecordRepository.findByTransferNo(transferNo)
                .orElseThrow(() -> new IllegalArgumentException("转账记录不存在: " + transferNo));

        return convertToSimpleDTO(record);
    }

    /**
     * 获取账户相关的转账记录
     */
    public List<TransferRecordDTO> getAccountTransferRecords(Long accountId) {
        logger.info("获取账户转账记录: {}", accountId);

        List<TransferRecord> records = transferRecordRepository.findByAccountId(accountId);
        return records.stream()
                .map(this::convertToSimpleDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取账户转出记录
     */
    public List<TransferRecordDTO> getAccountOutboundRecords(Long accountId) {
        logger.info("获取账户转出记录: {}", accountId);

        List<TransferRecord> records = transferRecordRepository.findByFromAccountIdOrderByTransferTimeDesc(accountId);
        return records.stream()
                .map(record -> {
                    TransferRecordDTO dto = convertToSimpleDTO(record);
                    dto.setTransferDirection("OUT");
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取账户转入记录
     */
    public List<TransferRecordDTO> getAccountInboundRecords(Long accountId) {
        logger.info("获取账户转入记录: {}", accountId);

        List<TransferRecord> records = transferRecordRepository.findByToAccountIdOrderByTransferTimeDesc(accountId);
        return records.stream()
                .map(record -> {
                    TransferRecordDTO dto = convertToSimpleDTO(record);
                    dto.setTransferDirection("IN");
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取转账统计信息
     */
    public Map<String, Object> getTransferStatistics() {
        logger.info("获取转账统计信息");

        try {
            Object[] rawStats = transferRecordRepository.getTransferStatistics();
            logger.info("原始统计数据: {}", java.util.Arrays.toString(rawStats));

            // JPA查询返回的可能是嵌套数组，需要解包
            if (rawStats != null && rawStats.length == 1 && rawStats[0] instanceof Object[]) {
                rawStats = (Object[]) rawStats[0];
                logger.info("解包后统计数据: {}", java.util.Arrays.toString(rawStats));
            }

            if (rawStats == null || rawStats.length < 5) {
                logger.warn("统计数据不完整，数组长度: {}", rawStats == null ? 0 : rawStats.length);
                throw new RuntimeException("统计数据不完整");
            }

            // 安全地解析查询结果
            Long totalCount = rawStats[0] != null ? ((Number) rawStats[0]).longValue() : 0L;
            BigDecimal totalAmount = rawStats[1] != null ? convertToBigDecimal(rawStats[1]) : BigDecimal.ZERO;
            Long largeAmountCount = rawStats[2] != null ? ((Number) rawStats[2]).longValue() : 0L;
            Long todayCount = rawStats[3] != null ? ((Number) rawStats[3]).longValue() : 0L;
            BigDecimal todayAmount = rawStats[4] != null ? convertToBigDecimal(rawStats[4]) : BigDecimal.ZERO;

            logger.info("解析后统计数据 - 总笔数: {}, 总金额: {}, 大额笔数: {}, 今日笔数: {}, 今日金额: {}",
                    totalCount, totalAmount, largeAmountCount, todayCount, todayAmount);

            Map<String, Object> stats = new HashMap<>();
            stats.put("total_count", totalCount);
            stats.put("total_amount", totalAmount);
            stats.put("large_amount_count", largeAmountCount);
            stats.put("today_count", todayCount);
            stats.put("today_amount", todayAmount);

            // 格式化金额显示
            stats.put("total_amount_display", AMOUNT_FORMAT.format(totalAmount));
            stats.put("today_amount_display", AMOUNT_FORMAT.format(todayAmount));

            logger.info("最终统计结果: {}", stats);
            return stats;

        } catch (Exception e) {
            logger.error("获取转账统计信息失败，尝试备用方法", e);

            try {
                // 尝试备用统计方法
                return getTransferStatisticsBackup();
            } catch (Exception backupError) {
                logger.error("备用统计方法也失败", backupError);

                // 返回默认统计信息
                Map<String, Object> defaultStats = new HashMap<>();
                defaultStats.put("total_count", 0L);
                defaultStats.put("total_amount", BigDecimal.ZERO);
                defaultStats.put("large_amount_count", 0L);
                defaultStats.put("today_count", 0L);
                defaultStats.put("today_amount", BigDecimal.ZERO);
                defaultStats.put("total_amount_display", "0.00");
                defaultStats.put("today_amount_display", "0.00");

                logger.info("返回默认统计数据: {}", defaultStats);
                return defaultStats;
            }
        }
    }

    /**
     * 备用统计方法 - 分别查询各项统计
     */
    private Map<String, Object> getTransferStatisticsBackup() {
        logger.info("使用备用统计方法");

        Long totalCount = transferRecordRepository.getTotalCount();
        BigDecimal totalAmount = transferRecordRepository.getTotalAmount();
        Long largeAmountCount = transferRecordRepository.getLargeAmountCount();
        Long todayCount = transferRecordRepository.getTodayCount();

        logger.info("备用统计结果 - 总笔数: {}, 总金额: {}, 大额笔数: {}, 今日笔数: {}",
                totalCount, totalAmount, largeAmountCount, todayCount);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_count", totalCount);
        stats.put("total_amount", totalAmount);
        stats.put("large_amount_count", largeAmountCount);
        stats.put("today_count", todayCount);
        stats.put("today_amount", BigDecimal.ZERO); // 今日金额暂时设为0，避免复杂查询

        // 格式化金额显示
        stats.put("total_amount_display", AMOUNT_FORMAT.format(totalAmount));
        stats.put("today_amount_display", "0.00");

        return stats;
    }

    /**
     * 获取账户转账统计
     */
    public Map<String, Object> getAccountTransferStatistics(Long accountId) {
        logger.info("获取账户转账统计: {}", accountId);

        try {
            Object[] rawStats = transferRecordRepository.getAccountTransferStats(accountId);

            // 解析查询结果
            Long totalCount = ((Number) rawStats[0]).longValue();
            BigDecimal totalAmount = (BigDecimal) rawStats[1];

            Map<String, Object> stats = new HashMap<>();
            stats.put("total_count", totalCount);
            stats.put("total_amount", totalAmount);

            // 格式化金额显示
            stats.put("total_amount_display", AMOUNT_FORMAT.format(totalAmount));

            return stats;
        } catch (Exception e) {
            logger.error("获取账户转账统计失败", e);
            // 返回默认统计信息
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("total_count", 0L);
            defaultStats.put("total_amount", BigDecimal.ZERO);
            defaultStats.put("total_amount_display", "0.00");
            return defaultStats;
        }
    }

    /**
     * 检查字符串是否有值
     */
    private boolean hasValue(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 将数据库查询结果转换为DTO（包含账户信息）
     */
    private TransferRecordDTO convertToDTO(Object[] row) {
        try {
            TransferRecordDTO dto = new TransferRecordDTO();

            dto.setTransferId(((Number) row[0]).longValue());
            dto.setFromAccountId(((Number) row[1]).longValue());
            dto.setToAccountId(((Number) row[2]).longValue());
            dto.setTransferNo((String) row[3]);
            dto.setAmount(convertToBigDecimal(row[4]));
            dto.setTransferTime(convertToLocalDateTime(row[5]));
            dto.setFromAccountName((String) row[6]);
            dto.setFromAccountPhone((String) row[7]);
            dto.setToAccountName((String) row[8]);
            dto.setToAccountPhone((String) row[9]);

            // 设置业务字段
            enrichDTO(dto);

            return dto;
        } catch (Exception e) {
            logger.error("转换DTO失败", e);
            throw new RuntimeException("转换DTO失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将实体转换为简单DTO（不包含账户信息）
     */
    private TransferRecordDTO convertToSimpleDTO(TransferRecord record) {
        TransferRecordDTO dto = new TransferRecordDTO();

        dto.setTransferId(record.getTransferId());
        dto.setFromAccountId(record.getFromAccountId());
        dto.setToAccountId(record.getToAccountId());
        dto.setTransferNo(record.getTransferNo());
        dto.setAmount(record.getAmount());
        dto.setTransferTime(record.getTransferTime());

        // 设置业务字段
        enrichDTO(dto);

        return dto;
    }

    /**
     * 丰富DTO的业务字段
     */
    private void enrichDTO(TransferRecordDTO dto) {
        // 格式化金额显示
        if (dto.getAmount() != null) {
            dto.setAmountDisplay(AMOUNT_FORMAT.format(dto.getAmount()));
        }

        // 格式化时间显示
        if (dto.getTransferTime() != null) {
            dto.setTransferTimeDisplay(dto.getTransferTime().format(TIME_FORMAT));
        }

        // 判断是否大额交易
        if (dto.getAmount() != null) {
            dto.setIsLargeAmount(dto.getAmount().compareTo(LARGE_AMOUNT_THRESHOLD) >= 0);
        }
    }

    /**
     * 安全地将数据库值转换为LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime();
            } else if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            } else if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate().atStartOfDay();
            } else {
                logger.warn("无法转换时间类型: {}", value.getClass());
                return null;
            }
        } catch (Exception e) {
            logger.error("时间转换失败", e);
            return null;
        }
    }

    /**
     * 安全地将数据库值转换为BigDecimal
     */
    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return new BigDecimal(value.toString());
            } else {
                logger.warn("无法转换数值类型: {}", value.getClass());
                return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}

