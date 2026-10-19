# 动态规则评估器使用说明

## 概述

`DynamicRuleEvaluator` 替代了硬编码的 `RuleEvaluator`，支持从规则表达式中动态解析条件和阈值。

## 主要改进

### 旧版本 (RuleEvaluator.java)
```java
// ❌ 硬编码的阈值
Dataset<Row> aFiltered = aRenamed.filter(col("indicator_A").gt(0));
Dataset<Row> bFiltered = bRenamed.filter(col("indicator_B").gt(0));
Dataset<Row> cFiltered = cRenamed.filter(col("indicator_C").geq(largeAmountThreshold));
```

### 新版本 (DynamicRuleEvaluator.java)
```java
// ✅ 动态解析规则表达式
String ruleExpression = "INDICATOR_A > 0 AND INDICATOR_B > 0 AND INDICATOR_C >= 50000";
DynamicRuleEvaluator evaluator = new DynamicRuleEvaluator();
Dataset<Row> result = evaluator.evaluate(ruleExpression, indicatorResults);
```

## 使用示例

### 1. 准备指标计算结果

```java
// 计算各个指标
BanksIndicatorCalculator calculator = new BanksIndicatorCalculator(spark);

Dataset<Row> indicatorA = calculator.calculateIndicator(
    "INDICATOR_A", paramConfigA, accounts, logins, transfers, timeWindowDays);

Dataset<Row> indicatorB = calculator.calculateIndicator(
    "INDICATOR_B", paramConfigB, accounts, logins, transfers, timeWindowDays);

Dataset<Row> indicatorC = calculator.calculateIndicator(
    "INDICATOR_C", paramConfigC, accounts, logins, transfers, timeWindowDays);

// 放入Map
Map<String, Dataset<Row>> indicatorResults = new HashMap<>();
indicatorResults.put("INDICATOR_A", indicatorA);
indicatorResults.put("INDICATOR_B", indicatorB);
indicatorResults.put("INDICATOR_C", indicatorC);
```

### 2. 使用动态规则评估

```java
// 创建评估器
DynamicRuleEvaluator evaluator = new DynamicRuleEvaluator();

// 受害人规则
String victimRule = "INDICATOR_A > 0 AND INDICATOR_B > 0 AND INDICATOR_C = 0";
Dataset<Row> victims = evaluator.evaluate(victimRule, indicatorResults);

// 可疑分子规则
String suspectRule = "INDICATOR_C >= 50000";
Dataset<Row> suspects = evaluator.evaluate(suspectRule, indicatorResults);
```

## 支持的操作符

- `>` : 大于
- `>=` : 大于等于
- `<` : 小于
- `<=` : 小于等于
- `=` : 等于
- `!=` : 不等于

## 支持的逻辑操作符

- `AND` : 交集（所有条件都满足）
- `OR` : 并集（任一条件满足）

## 规则表达式示例

```java
// 简单规则
"INDICATOR_A > 0"

// AND规则（交集）
"INDICATOR_A > 0 AND INDICATOR_B > 0"

// 复杂AND规则
"INDICATOR_A > 0 AND INDICATOR_B > 0 AND INDICATOR_C >= 50000"

// OR规则（并集）
"INDICATOR_A > 10 OR INDICATOR_B > 5"

// 不同阈值
"INDICATOR_A >= 1 AND INDICATOR_B >= 2 AND INDICATOR_C = 0"
```

## 与IntegratedSparkService的集成

`IntegratedSparkService` 已经内置了相同的规则解析逻辑，会自动：

1. 解析规则表达式
2. 提取每个指标的条件和阈值
3. 动态应用过滤条件
4. 根据AND/OR逻辑合并结果

## 迁移指南

如果你的代码使用了旧的 `RuleEvaluator`：

### 旧代码
```java
RuleEvaluator evaluator = new RuleEvaluator();
Dataset<Row> result = evaluator.evaluate(a, b, c, largeAmountThreshold);
```

### 新代码
```java
Map<String, Dataset<Row>> indicatorResults = new HashMap<>();
indicatorResults.put("INDICATOR_A", a);
indicatorResults.put("INDICATOR_B", b);
indicatorResults.put("INDICATOR_C", c);

String ruleExpression = "INDICATOR_A > 0 AND INDICATOR_B > 0 AND INDICATOR_C >= " + largeAmountThreshold;

DynamicRuleEvaluator evaluator = new DynamicRuleEvaluator();
Dataset<Row> result = evaluator.evaluate(ruleExpression, indicatorResults);
```

## 注意事项

1. **列名统一**: 确保指标结果的第一列是账户ID（会自动重命名为 `account_id`）
2. **指标列名**: 指标值列应该命名为 `indicator_A`, `indicator_B` 等
3. **规则格式**: 规则表达式必须严格遵循格式 `INDICATOR_XXX 操作符 数值`
4. **空结果处理**: 如果没有账户满足条件，会返回空的Dataset

## 性能优化建议

1. 在调用 `evaluate` 之前缓存指标结果：
   ```java
   indicatorA.cache();
   indicatorB.cache();
   indicatorC.cache();
   ```

2. 评估完成后释放缓存：
   ```java
   indicatorA.unpersist();
   indicatorB.unpersist();
   indicatorC.unpersist();
   ```

3. 对于大数据集，考虑使用分区：
   ```java
   indicatorA.repartition(200);
   ```
