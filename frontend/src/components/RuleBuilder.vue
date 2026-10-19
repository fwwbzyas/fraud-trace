<template>
  <div class="rule-builder">
    <div class="rule-builder-header">
      <h3>规则表达式</h3>
      <p class="description">分别构建受害人和可疑分子的识别规则</p>
    </div>

    <!-- 分离规则模式 -->
    <div v-if="ruleType === 'separate'" class="separate-rules-container">
      <!-- 受害人规则 -->
      <div class="rule-section victim-rules">
        <div class="section-header">
          <h4>受害人识别规则（付款方指标）</h4>
          <p class="section-desc">基于A、B指标识别可能的受害人</p>
        </div>
        
        <div class="conditions-container">
          <div 
            v-for="(condition, index) in victimConditions" 
            :key="'victim-' + index"
            class="condition-item victim-condition"
            :class="{ 'has-error': condition.hasError }"
          >
            <div class="condition-number">V{{ index + 1 }}</div>
            <div class="condition-content">
              <div class="condition-fields">
                <!-- 指标代码选择 -->
                <div class="field-group">
                  <label class="field-label">指标</label>
                  <el-select
                    v-model="condition.indicatorCode"
                    placeholder="选择指标"
                    filterable
                    @change="updateModelValue"
                    class="indicator-select"
                  >
                    <el-option
                      v-for="indicator in victimIndicators"
                      :key="indicator.code"
                      :label="`${indicator.code}`"
                      :value="indicator.code"
                    >
                      <div class="option-content">
                        <span class="option-code">{{ indicator.code }}</span>
                        <span class="option-name">{{ indicator.name }}</span>
                      </div>
                    </el-option>
                  </el-select>
                </div>

                <!-- 操作符选择 -->
                <div class="field-group">
                  <label class="field-label">条件</label>
                  <el-select v-model="condition.operator" @change="validateRules" class="operator-select">
                    <el-option label="等于 (=)" value="=" />
                    <el-option label="不等于 (!=)" value="!=" />
                    <el-option label="大于 (>)" value=">" />
                    <el-option label="大于等于 (>=)" value=">=" />
                    <el-option label="小于 (<)" value="<" />
                    <el-option label="小于等于 (<=)" value="<=" />
                  </el-select>
                </div>

                <!-- 数值输入 -->
                <div class="field-group">
                  <label class="field-label">数值</label>
                  <el-input-number
                    v-model="condition.value"
                    :min="0"
                    :precision="2"
                    @change="updateModelValue"
                    placeholder="输入数值"
                    class="value-input"
                  />
                </div>

                <!-- 逻辑连接符 -->
                <div v-if="index < victimConditions.length - 1" class="field-group">
                  <label class="field-label">连接</label>
                  <el-select v-model="condition.connector" @change="validateRules" class="connector-select">
                    <el-option label="并且 (AND)" value="AND" />
                    <el-option label="或者 (OR)" value="OR" />
                  </el-select>
                </div>

                <!-- 删除按钮 -->
                <div class="field-group">
                  <el-button 
                    v-if="victimConditions.length > 1"
                    type="danger" 
                    size="small" 
                    :icon="Delete" 
                    circle 
                    @click="removeVictimCondition(index)"
                    class="delete-btn"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="rule-actions">
          <el-button type="primary" :icon="Plus" @click="addVictimCondition" size="small">
            添加受害人条件
          </el-button>
        </div>

      
      </div>

      <!-- 可疑分子规则 -->
      <div class="rule-section suspect-rules">
        <div class="section-header">
          <h4>可疑分子识别规则（收款方指标）</h4>
          <p class="section-desc">基于C指标识别可疑的收款账户</p>
        </div>
        
        <div class="conditions-container">
          <div 
            v-for="(condition, index) in suspectConditions" 
            :key="'suspect-' + index"
            class="condition-item suspect-condition"
            :class="{ 'has-error': condition.hasError }"
          >
            <div class="condition-number">S{{ index + 1 }}</div>
            <div class="condition-content">
              <div class="condition-fields">
                <!-- 指标代码选择 -->
                <div class="field-group">
                  <label class="field-label">指标</label>
                  <el-select
                    v-model="condition.indicatorCode"
                    placeholder="选择指标"
                    filterable
                    @change="updateModelValue"
                    class="indicator-select"
                  >
                    <el-option
                      v-for="indicator in suspectIndicators"
                      :key="indicator.code"
                      :label="`${indicator.code}`"
                      :value="indicator.code"
                    >
                      <div class="option-content">
                        <span class="option-code">{{ indicator.code }}</span>
                        <span class="option-name">{{ indicator.name }}</span>
                      </div>
                    </el-option>
                  </el-select>
                </div>

                <!-- 操作符选择 -->
                <div class="field-group">
                  <label class="field-label">条件</label>
                  <el-select v-model="condition.operator" @change="validateRules" class="operator-select">
                    <el-option label="等于 (=)" value="=" />
                    <el-option label="不等于 (!=)" value="!=" />
                    <el-option label="大于 (>)" value=">" />
                    <el-option label="大于等于 (>=)" value=">=" />
                    <el-option label="小于 (<)" value="<" />
                    <el-option label="小于等于 (<=)" value="<=" />
                  </el-select>
                </div>

                <!-- 数值输入 -->
                <div class="field-group">
                  <label class="field-label">数值</label>
                  <el-input-number
                    v-model="condition.value"
                    :min="0"
                    :precision="2"
                    @change="updateModelValue"
                    placeholder="输入数值"
                    class="value-input"
                  />
                </div>

                <!-- 逻辑连接符 -->
                <div v-if="index < suspectConditions.length - 1" class="field-group">
                  <label class="field-label">连接</label>
                  <el-select v-model="condition.connector" @change="validateRules" class="connector-select">
                    <el-option label="并且 (AND)" value="AND" />
                    <el-option label="或者 (OR)" value="OR" />
                  </el-select>
                </div>

                <!-- 删除按钮 -->
                <div class="field-group">
                  <el-button 
                    v-if="suspectConditions.length > 1"
                    type="danger" 
                    size="small" 
                    :icon="Delete" 
                    circle 
                    @click="removeSuspectCondition(index)"
                    class="delete-btn"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="rule-actions">
          <el-button type="danger" :icon="Plus" @click="addSuspectCondition" size="small">
            添加可疑分子条件
          </el-button>
        </div>

        
      </div>
    </div>

    

    

  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { Plus, Delete, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'RuleBuilder',
  components: {
    Plus,
    Delete,
    Refresh
  },
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    availableIndicators: {
      type: Array,
      default: () => [
        { code: 'INDICATOR_A', name: '短时大额转出' },
        { code: 'INDICATOR_B', name: '短时高频转出' },
        { code: 'INDICATOR_C', name: '销赃账户转出' },
        { code: 'INDICATOR_D', name: '深夜转账' },
        { code: 'INDICATOR_E', name: '异地转账' },
        { code: 'INDICATOR_F', name: '团伙转账' },
        { code: 'INDICATOR_N', name: '正常指标' }
      ]
    }
  },
  emits: ['update:modelValue', 'rule-change'],
  setup(props, { emit }) {
    // 规则类型
    const ruleType = ref('separate')
    
    // 传统条件列表
    // 移除传统模式条件，专注分离规则
    const conditions = ref([])

    // 受害人条件列表
    const victimConditions = ref([
      {
        indicatorCode: '',
        operator: '>',
        value: 0,
        connector: 'AND',
        hasError: false
      }
    ])

    // 可疑分子条件列表
    const suspectConditions = ref([
      {
        indicatorCode: '',
        operator: '>=',
        value: 0,
        connector: 'AND',
        hasError: false
      }
    ])

    // 指标分类 - 现在使用所有可用指标，支持灵活配置
    const victimIndicators = computed(() => {
      return props.availableIndicators.map(indicator => ({
        code: indicator.indicatorCode,
        name: indicator.indicatorName
      }))
    })

    const suspectIndicators = computed(() => {
      return props.availableIndicators.map(indicator => ({
        code: indicator.indicatorCode,
        name: indicator.indicatorName
      }))
    })


    // 生成的规则表达式
    const generatedRule = computed(() => {
      return JSON.stringify({
        type: 'separate_rules',
        victimRule: generatedVictimRule.value,
        suspectRule: generatedSuspectRule.value
      })
    })

    // 生成受害人规则
    const generatedVictimRule = computed(() => {
      try {
        const validConditions = victimConditions.value.filter(condition => 
          condition.indicatorCode && 
          condition.operator && 
          condition.value !== null && 
          condition.value !== undefined
        )

        if (validConditions.length === 0) return ''

        let rule = ''
        for (let i = 0; i < validConditions.length; i++) {
          const condition = validConditions[i]
          rule += `${condition.indicatorCode} ${condition.operator} ${condition.value}`
          
          if (i < validConditions.length - 1) {
            rule += ` ${condition.connector} `
          }
        }
        
        return rule
      } catch (error) {
        console.error('生成受害人规则错误:', error)
        return ''
      }
    })

    // 生成可疑分子规则
    const generatedSuspectRule = computed(() => {
      try {
        const validConditions = suspectConditions.value.filter(condition => 
          condition.indicatorCode && 
          condition.operator && 
          condition.value !== null && 
          condition.value !== undefined
        )

        if (validConditions.length === 0) return ''

        let rule = ''
        for (let i = 0; i < validConditions.length; i++) {
          const condition = validConditions[i]
          rule += `${condition.indicatorCode} ${condition.operator} ${condition.value}`
          
          if (i < validConditions.length - 1) {
            rule += ` ${condition.connector} `
          }
        }
        
        return rule
      } catch (error) {
        console.error('生成可疑分子规则错误:', error)
        return ''
      }
    })

    // 添加条件
    // 移除传统模式的添加/删除
    const addCondition = () => {}

    // 删除条件
    const removeCondition = () => {}

    // 添加受害人条件
    const addVictimCondition = () => {
      victimConditions.value.push({
        indicatorCode: '',
        operator: '>',
        value: 0,
        connector: 'AND',
        hasError: false
      })
      validateRules()
    }

    // 删除受害人条件
    const removeVictimCondition = (index) => {
      if (victimConditions.value.length > 1) {
        victimConditions.value.splice(index, 1)
        updateModelValue()
      }
    }

    // 添加可疑分子条件
    const addSuspectCondition = () => {
      suspectConditions.value.push({
        indicatorCode: '',
        operator: '>',
        value: 0,
        connector: 'AND',
        hasError: false
      })
      validateRules()
    }

    // 删除可疑分子条件
    const removeSuspectCondition = (index) => {
      if (suspectConditions.value.length > 1) {
        suspectConditions.value.splice(index, 1)
        updateModelValue()
      }
    }

    // 规则类型变化处理
    const handleRuleTypeChange = () => {
      updateModelValue()
      ElMessage.info(`已切换到${ruleType.value === 'separate' ? '分离规则' : '传统规则'}模式`)
    }

    // 重置规则
    const resetRules = () => {
      // 重置分离规则为空条件
      victimConditions.value = [
        {
          indicatorCode: '',
          operator: '>',
          value: 0,
          connector: 'AND',
          hasError: false
        }
      ]
      
      suspectConditions.value = [
        {
          indicatorCode: '',
          operator: '>=',
          value: 0,
          connector: 'AND',
          hasError: false
        }
      ]
      
      updateModelValue()
    }


    // 更新模型值
    const updateModelValue = () => {
      const newRule = generatedRule.value
      emit('update:modelValue', newRule)
      emit('rule-change', {
        rule: newRule,
        isValid: true,
        errors: []
      })
    }

    // 监听规则变化
    watch(generatedRule, (newRule) => {
      updateModelValue()
    }, { immediate: true })

    // 监听外部值变化
    watch(() => props.modelValue, (newValue) => {
      if (newValue && newValue !== generatedRule.value) {
        console.log('外部规则变化:', newValue)
      }
    })

    return {
      // 规则类型
      ruleType,
      handleRuleTypeChange,
      
      // 传统规则
      conditions,
      addCondition,
      removeCondition,
      
      // 分离规则
      victimConditions,
      suspectConditions,
      victimIndicators,
      suspectIndicators,
      addVictimCondition,
      removeVictimCondition,
      addSuspectCondition,
      removeSuspectCondition,
      generatedVictimRule,
      generatedSuspectRule,
      
      // 共用功能
      generatedRule,
      resetRules,
      
      // 图标
      Plus,
      Delete,
      Refresh
    }
  }
}
</script>

<style scoped>
.rule-builder {
  background: #fafafa;
  border-radius: 8px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  width: 100%;
  box-sizing: border-box;
  overflow: visible;
}

.rule-builder-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.rule-builder-header h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.description {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.conditions-container {
  margin-bottom: 20px;
}

.condition-item {
  display: flex;
  align-items: flex-start;
  background: #fafafa;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
  transition: border-color 0.2s ease;
}

.condition-item:hover {
  border-color: #c0c4cc;
}

.condition-item.has-error {
  border-color: #f56c6c;
}

.condition-number {
  width: 28px;
  height: 28px;
  background: #f0f0f0;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 12px;
  margin-right: 12px;
  flex-shrink: 0;
  color: #606266;
}

.condition-content {
  flex: 1;
}

.condition-fields {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.field-group {
  flex: 1;
  min-width: 120px;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.indicator-select {
  min-width: 180px;
}

.option-content {
  display: flex;
  flex-direction: column;
}

.option-code {
  font-weight: 600;
  color: #409eff;
}

.option-name {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.delete-btn {
  margin-top: 8px;
}

/* 分离规则样式 */
.separate-rules-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  margin-bottom: 20px;
  width: 100%;
  box-sizing: border-box;
}

.rule-section {
  background: white;
  border-radius: 6px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  width: 100%;
  box-sizing: border-box;
  overflow: visible;
  min-width: 0;
}

.victim-rules {
  border-left: 3px solid #67C23A;
}

.suspect-rules {
  border-left: 3px solid #F56C6C;
}

.section-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.section-header h4 {
  margin: 0 0 6px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.section-desc {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.victim-condition .condition-number {
  background: #f0f9ff;
  color: #67C23A;
  border: 1px solid #b3e19d;
}

.suspect-condition .condition-number {
  background: #fef0f0;
  color: #F56C6C;
  border: 1px solid #fbc4c4;
}

.rule-actions {
  margin-top: 16px;
  text-align: left;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .condition-fields {
    flex-direction: column;
    align-items: stretch;
  }
  
  .field-group {
    min-width: auto;
  }
  
  .separate-rules-container {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
</style>