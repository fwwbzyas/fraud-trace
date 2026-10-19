<template>
  <div class="indicator-list">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-row justify="space-between" align="middle">
        <el-col :span="12">
          <h2 class="page-title">
            <el-icon style="margin-right: 8px"><DataBoard /></el-icon>
            指标管理
          </h2>
          <p class="page-subtitle">管理检测指标，调整阈值参数，实时生效</p>
        </el-col>
        <el-col :span="12" style="text-align: right">
          <el-button 
            v-if="isAdmin" 
            type="primary" 
            :icon="Plus" 
            @click="createIndicator"
          >
            新建指标
          </el-button>
          <el-button :icon="Refresh" @click="loadIndicators">
            刷新
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.totalCount || 0 }}</div>
            <div class="stats-label">总指标数</div>
          </div>
          <el-icon class="stats-icon" style="color: #409EFF"><DataBoard /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.activeCount || 0 }}</div>
            <div class="stats-label">激活指标</div>
          </div>
          <el-icon class="stats-icon" style="color: #67C23A"><SuccessFilled /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.coreCount || 0 }}</div>
            <div class="stats-label">核心指标</div>
          </div>
          <el-icon class="stats-icon" style="color: #E6A23C"><Star /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.extendedCount || 0 }}</div>
            <div class="stats-label">扩展指标</div>
          </div>
          <el-icon class="stats-icon" style="color: #909399"><Promotion /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和筛选 -->
    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索指标名称..."
            :prefix-icon="Search"
            @input="handleSearch"
            clearable
          />
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterType" placeholder="指标类型" @change="handleFilter" clearable>
            <el-option label="全部" value="" />
            <el-option label="核心指标" value="core" />
            <el-option label="扩展指标" value="extended" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="状态" @change="handleFilter" clearable>
            <el-option label="全部" value="" />
            <el-option label="激活" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
        </el-col>
      </el-row>
    </el-card>

    <!-- 指标列表 -->
    <el-card class="table-card">
      <el-table
        :data="filteredIndicators"
        v-loading="loading"
        stripe
        style="width: 100%"
        @row-click="viewIndicator"
      >
        <el-table-column prop="indicatorCode" label="指标代码" width="150">
          <template #default="{ row }">
            <el-tag :type="row.isCore === 1 ? 'warning' : 'info'" size="small">
              {{ row.indicatorCode }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="indicatorName" label="指标名称" min-width="200">
          <template #default="{ row }">
            <div style="display: flex; align-items: center;">
              <el-icon v-if="row.isCore === 1" style="color: #E6A23C; margin-right: 5px">
                <Star />
              </el-icon>
              <span>{{ row.indicatorName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="indicatorDesc" label="描述" min-width="300" show-overflow-tooltip />
        
        <el-table-column label="核心指标" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.isCore"
              :active-value="1"
              :inactive-value="0"
              @change="toggleCoreStatus(row)"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="用户类型" width="120">
          <template #default="{ row }">
            <el-tag 
              :type="getUserTypeTagType(getUserType(row))" 
              size="small"
            >
              {{ getUserTypeLabel(getUserType(row)) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              :active-value="1"
              :inactive-value="0"
              @change="toggleIndicatorStatus(row)"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="配置参数" width="120">
          <template #default="{ row }">
            <el-tag type="success" size="small">
              {{ getConfigurableFieldsCount(row) }} 个参数
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button 
                v-if="isAdmin"
                size="small" 
                type="primary" 
                :icon="Edit" 
                @click.stop="editIndicator(row)" 
                plain
              >
                编辑
              </el-button>
              <el-button size="small" type="success" :icon="Setting" @click.stop="configParams(row)" plain>
                参数
              </el-button>
              <el-popconfirm
                v-if="isAdmin"
                title="确定删除这个指标吗？"
                @confirm="deleteIndicator(row)"
                width="200"
              >
                <template #reference>
                  <el-button size="small" type="danger" :icon="Delete" @click.stop plain>
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑指标对话框 -->
    <el-dialog
      v-model="indicatorDialogVisible"
      :title="isEditMode ? '编辑指标' : '新建指标'"
      width="900px"
      @closed="resetIndicatorDialog"
      class="indicator-dialog"
    >
      <el-form 
        :model="indicatorForm" 
        :rules="indicatorRules"
        ref="indicatorFormRef"
        label-width="120px"
      >
        <el-form-item label="指标代码" prop="indicatorCode">
          <el-input 
            v-model="indicatorForm.indicatorCode" 
            placeholder="如：INDICATOR_A"
            :disabled="isEditMode"
            style="width: 300px"
          />
          <div class="form-description">
            指标代码必须唯一，创建后不可修改
          </div>
        </el-form-item>
        
        <el-form-item label="指标名称" prop="indicatorName">
          <el-input 
            v-model="indicatorForm.indicatorName" 
            placeholder="如：短时大额转出"
            style="width: 400px"
          />
        </el-form-item>
        
        <el-form-item label="指标描述" prop="indicatorDesc">
          <el-input 
            v-model="indicatorForm.indicatorDesc" 
            type="textarea"
            :rows="3"
            placeholder="详细描述该指标的含义和计算方式"
            style="width: 600px"
          />
        </el-form-item>
        
        <el-form-item label="核心指标">
          <el-switch
            v-model="indicatorForm.isCore"
            :active-value="1"
            :inactive-value="0"
          />
          <div class="form-description">
            核心指标会在规则构建器中优先显示
          </div>
        </el-form-item>
        
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="indicatorForm.userType" style="width: 300px">
            <el-option label="付款方/受害人" value="payer">
              <div style="display: flex; align-items: center;">
                <el-tag type="success" size="small" style="margin-right: 8px">付款方</el-tag>
                <span>识别可能的受害人</span>
              </div>
            </el-option>
            <el-option label="收款方/可疑分子" value="payee">
              <div style="display: flex; align-items: center;">
                <el-tag type="danger" size="small" style="margin-right: 8px">收款方</el-tag>
                <span>识别可疑的收款账户</span>
              </div>
            </el-option>
            <el-option label="通用指标" value="both">
              <div style="display: flex; align-items: center;">
                <el-tag type="info" size="small" style="margin-right: 8px">通用</el-tag>
                <span>可用于两种规则</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="初始状态">
          <el-switch
            v-model="indicatorForm.isActive"
            :active-value="1"
            :inactive-value="0"
            active-text="激活"
            inactive-text="停用"
          />
        </el-form-item>
        
        <el-divider content-position="left">参数配置</el-divider>
        
        <el-form-item label="参数配置">
          <div class="param-config-editor">
            <el-alert
              title="参数配置说明"
              type="info"
              :closable="false"
              style="margin-bottom: 10px"
            >
              <template #default>
                <div style="font-size: 12px;">
                  参数配置为JSON格式，用于存储指标的检测阈值和计算参数。
                  <br>例如：{"large_amount_threshold": 50000, "time_window_days": 30}
                </div>
              </template>
            </el-alert>
            <el-input
              v-model="paramConfigText"
              type="textarea"
              :rows="6"
              placeholder='{"param1": value1, "param2": value2}'
              style="font-family: monospace;"
            />
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="indicatorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveIndicator" :loading="savingIndicator">
          {{ isEditMode ? '保存修改' : '创建指标' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 参数配置对话框 -->
    <el-dialog
      v-model="paramDialogVisible"
      :title="`参数调整 - ${currentIndicator?.indicatorName}`"
      width="800px"
      @closed="resetParamDialog"
      class="param-dialog"
    >
      <div v-if="currentIndicator">
        <el-alert
          title="实时参数调整"
          type="warning"
          description="调整检测阈值参数，保存后立即生效，影响后续的风险分析结果"
          show-icon
          :closable="false"
          style="margin-bottom: 20px"
        />
        
        <el-form :model="paramForm" label-width="120px">
          <!-- 用户类型配置 -->
          <el-form-item label="用户类型">
            <el-select v-model="paramForm.user_type" style="width: 200px">
              <el-option label="付款方/受害人" value="payer">
                <div style="display: flex; align-items: center;">
                  <el-tag type="success" size="small" style="margin-right: 8px">付款方</el-tag>
                  <span>识别可能的受害人</span>
                </div>
              </el-option>
              <el-option label="收款方/可疑分子" value="payee">
                <div style="display: flex; align-items: center;">
                  <el-tag type="danger" size="small" style="margin-right: 8px">收款方</el-tag>
                  <span>识别可疑的收款账户</span>
                </div>
              </el-option>
              <el-option label="通用指标" value="both">
                <div style="display: flex; align-items: center;">
                  <el-tag type="info" size="small" style="margin-right: 8px">通用</el-tag>
                  <span>可用于两种规则</span>
                </div>
              </el-option>
            </el-select>
            <div class="param-description">
              指定该指标主要用于识别哪类用户，影响规则构建器中的指标分类显示
            </div>
          </el-form-item>
          
          <!-- 分隔线 -->
          <el-divider content-position="left">检测参数</el-divider>
          
          <!-- 如果有预定义字段，使用字段定义 -->
          <template v-if="configurableFields.length > 0">
            <div v-for="field in configurableFields" :key="field.field">
              <el-form-item :label="field.label || field.field">
                <!-- 数字类型参数 -->
                <el-input-number
                  v-if="field.type === 'integer' || field.type === 'decimal'"
                  v-model="paramForm[field.field]"
                  :min="field.min"
                  :max="field.max"
                  :precision="field.type === 'decimal' ? 2 : 0"
                  :step="field.type === 'decimal' ? 0.01 : 1"
                  style="width: 200px"
                />
                <!-- 布尔类型参数 -->
                <el-switch
                  v-else-if="field.type === 'boolean'"
                  v-model="paramForm[field.field]"
                />
                <!-- 文本类型参数 -->
                <el-input
                  v-else
                  v-model="paramForm[field.field]"
                  style="width: 300px"
                />
                <span class="param-description">
                  (默认: {{ field.default }}, 范围: {{ field.min || '无限制' }} - {{ field.max || '无限制' }})
                </span>
              </el-form-item>
            </div>
          </template>
          
          <!-- 如果没有预定义字段，显示所有参数的通用编辑器 -->
          <template v-else>
            <el-alert
              title="参数配置"
              type="info"
              :closable="false"
              style="margin-bottom: 15px"
            >
              <template #default>
                <div style="font-size: 12px;">
                  该指标没有预定义的参数字段，您可以手动编辑所有参数
                </div>
              </template>
            </el-alert>
            
            <!-- 显示所有参数的动态表单 -->
            <template v-for="(value, key) in paramForm" :key="key">
              <el-form-item v-if="key !== 'user_type'" :label="getParamLabel(key)">
                <!-- 数字类型 -->
                <el-input-number
                  v-if="typeof value === 'number'"
                  v-model="paramForm[key]"
                  :precision="Number.isInteger(value) ? 0 : 2"
                  :step="Number.isInteger(value) ? 1 : 0.01"
                  style="width: 200px"
                />
                <!-- 布尔类型 -->
                <el-switch
                  v-else-if="typeof value === 'boolean'"
                  v-model="paramForm[key]"
                />
                <!-- 字符串类型 -->
                <el-input
                  v-else
                  v-model="paramForm[key]"
                  style="width: 300px"
                />
                <span class="param-description">
                  当前值: {{ value }}
                </span>
              </el-form-item>
            </template>
            
            <!-- JSON 编辑器选项 -->
            <el-divider content-position="left">或使用 JSON 编辑器</el-divider>
            <el-form-item label="JSON 编辑">
              <el-input
                v-model="paramConfigJsonText"
                type="textarea"
                :rows="8"
                placeholder='{"param1": value1, "param2": value2}'
                style="font-family: monospace;"
              />
              <div class="param-description">
                直接编辑 JSON 格式的参数配置（不包括 user_type）
              </div>
            </el-form-item>
          </template>
        </el-form>
      </div>
      
      <template #footer>
        <el-button @click="paramDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveParams" :loading="savingParams">
          保存参数
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, Setting, Search, Star, Plus, Edit, Delete,
  DataBoard, SuccessFilled, Promotion
} from '@element-plus/icons-vue'
import { indicatorApi } from '../api/indicator'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { isAdmin } = useAuth()

// 响应式数据
const loading = ref(false)
const indicators = ref([])
const statistics = ref({})
const searchKeyword = ref('')
const filterType = ref('')
const filterStatus = ref('')

// 参数配置对话框
const paramDialogVisible = ref(false)
const currentIndicator = ref(null)
const paramForm = ref({})
const configurableFields = ref([])
const savingParams = ref(false)

// 创建/编辑指标对话框
const indicatorDialogVisible = ref(false)
const isEditMode = ref(false)
const indicatorFormRef = ref(null)
const savingIndicator = ref(false)
const indicatorForm = ref({
  indicatorCode: '',
  indicatorName: '',
  indicatorDesc: '',
  isCore: 0,
  isActive: 1,
  userType: 'payer',
  paramConfig: {}
})

// 参数配置文本（用于编辑指标）
const paramConfigText = computed({
  get: () => {
    try {
      return JSON.stringify(indicatorForm.value.paramConfig || {}, null, 2)
    } catch {
      return '{}'
    }
  },
  set: (value) => {
    try {
      indicatorForm.value.paramConfig = JSON.parse(value)
    } catch (e) {
      console.error('参数配置JSON格式错误:', e)
    }
  }
})

// 参数配置JSON文本（用于参数配置对话框）
const paramConfigJsonText = computed({
  get: () => {
    try {
      // 排除 user_type
      const config = { ...paramForm.value }
      delete config.user_type
      return JSON.stringify(config, null, 2)
    } catch {
      return '{}'
    }
  },
  set: (value) => {
    try {
      const parsed = JSON.parse(value)
      // 保留 user_type，更新其他参数
      const userType = paramForm.value.user_type
      Object.assign(paramForm.value, parsed)
      paramForm.value.user_type = userType
    } catch (e) {
      console.error('参数配置JSON格式错误:', e)
    }
  }
})

// 获取参数标签（将下划线命名转换为中文）
const getParamLabel = (key) => {
  const labelMap = {
    'large_amount_threshold': '大额阈值',
    'time_window_days': '时间窗口（天）',
    'quick_out_minutes': '快速转出时间（分钟）',
    'login_to_transfer_minutes': '登录到转账时间（分钟）',
    'min_transfer_amount': '最小转账金额',
    'cumulative_income_threshold': '累计入账阈值',
    'min_amount': '最小金额',
    'start_hour': '开始小时',
    'end_hour': '结束小时',
    'time_window_hours': '时间窗口（小时）',
    'min_locations': '最少地点数',
    'abnormal_account_threshold': '异常账户阈值',
    'normal_transaction': '正常交易'
  }
  return labelMap[key] || key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
}

// 表单验证规则
const indicatorRules = {
  indicatorCode: [
    { required: true, message: '请输入指标代码', trigger: 'blur' },
    { pattern: /^INDICATOR_[A-Z]$/, message: '指标代码格式：INDICATOR_大写字母', trigger: 'blur' }
  ],
  indicatorName: [
    { required: true, message: '请输入指标名称', trigger: 'blur' },
    { min: 2, max: 50, message: '指标名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  indicatorDesc: [
    { required: true, message: '请输入指标描述', trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ]
}

// 计算属性
const filteredIndicators = computed(() => {
  let result = [...indicators.value]
  
  // 搜索过滤
  if (searchKeyword.value) {
    result = result.filter(item => 
      item.indicatorName.includes(searchKeyword.value) ||
      item.indicatorCode.includes(searchKeyword.value)
    )
  }
  
  // 类型过滤
  if (filterType.value === 'core') {
    result = result.filter(item => item.isCore === 1)
  } else if (filterType.value === 'extended') {
    result = result.filter(item => item.isCore === 0)
  }
  
  // 状态过滤
  if (filterStatus.value === 'active') {
    result = result.filter(item => item.isActive === 1)
  } else if (filterStatus.value === 'inactive') {
    result = result.filter(item => item.isActive === 0)
  }
  
  return result
})

// 生命周期
onMounted(() => {
  loadIndicators()
  loadStatistics()
})

// 方法
const loadIndicators = async () => {
  loading.value = true
  try {
    const response = await indicatorApi.getAllIndicators()
    if (response.data.success) {
      indicators.value = response.data.data
      console.log('📊 指标数据:', indicators.value)
      // 加载指标后，更新统计数据
      loadStatistics()
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('获取指标列表失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    // 根据实际指标数据计算统计信息
    const totalCount = indicators.value.length
    const activeCount = indicators.value.filter(item => item.isActive === 1).length
    const coreCount = indicators.value.filter(item => item.isCore === 1).length
    const extendedCount = indicators.value.filter(item => item.isCore === 0).length
    
    statistics.value = {
      totalCount,
      activeCount,
      coreCount,
      extendedCount
    }
  } catch (error) {
    console.error('计算统计信息失败:', error)
    // 如果出错，至少显示0
    statistics.value = {
      totalCount: 0,
      activeCount: 0,
      coreCount: 0,
      extendedCount: 0
    }
  }
}

const viewIndicator = (row) => {
  // 点击行查看参数配置
  configParams(row)
}

// 获取指标的用户类型
const getUserType = (indicator) => {
  // 从参数配置中获取用户类型
  if (indicator.paramConfig && indicator.paramConfig.user_type) {
    return indicator.paramConfig.user_type
  }
  
  // 如果没有配置，根据指标代码推断
  const code = indicator.indicatorCode
  // A, B, D, E, F 是付款方/受害人指标
  if (code.includes('_A') || code.includes('_B') || code.includes('_D') || code.includes('_E') || code.includes('_F')) {
    return 'payer' // 付款方/受害人指标
  } else if (code.includes('_C')) {
    return 'payee' // 收款方/可疑分子指标（销赃账户转出）
  } else if (code.includes('_N')) {
    return 'both' // 通用指标（正常指标）
  } else {
    return 'both' // 默认通用指标
  }
}

// 获取用户类型的显示标签
const getUserTypeLabel = (userType) => {
  const labels = {
    'payer': '受害人',
    'payee': '可疑分子',
    'both': '通用'
  }
  return labels[userType] || '未知'
}

// 获取用户类型的标签颜色
const getUserTypeTagType = (userType) => {
  const types = {
    'payer': 'success',
    'payee': 'danger',
    'both': 'info'
  }
  return types[userType] || 'info'
}

const configParams = (row) => {
  currentIndicator.value = row
  const paramConfig = row.paramConfig || {}
  
  // 如果有关联字段定义，使用字段定义
  if (paramConfig.configurable_fields && paramConfig.configurable_fields.length > 0) {
    configurableFields.value = paramConfig.configurable_fields
    
    // 初始化参数表单
    const form = {}
    configurableFields.value.forEach(field => {
      form[field.field] = paramConfig[field.field] !== undefined ? paramConfig[field.field] : field.default
    })
    
    // 初始化用户类型
    form.user_type = getUserType(row)
    
    paramForm.value = form
    paramDialogVisible.value = true
  } else {
    // 如果没有预定义字段，直接编辑所有参数
    configurableFields.value = []
    
    // 从 paramConfig 中提取所有参数（排除 configurable_fields 和 user_type）
    const form = {}
    Object.keys(paramConfig).forEach(key => {
      if (key !== 'configurable_fields' && key !== 'user_type' && key !== 'sql_template') {
        form[key] = paramConfig[key]
      }
    })
    
    // 初始化用户类型
    form.user_type = getUserType(row)
    
    paramForm.value = form
    paramDialogVisible.value = true
  }
}

const saveParams = async () => {
  // 如果使用JSON编辑器，先验证JSON格式
  if (configurableFields.value.length === 0) {
    try {
      JSON.parse(paramConfigJsonText.value)
    } catch (e) {
      ElMessage.error('参数配置JSON格式错误，请检查')
      return
    }
  }
  
  savingParams.value = true
  try {
    // 构建参数配置对象
    const paramConfig = { ...currentIndicator.value.paramConfig }
    
    // 更新所有参数（排除 user_type，它会被单独处理）
    Object.keys(paramForm.value).forEach(key => {
      if (key !== 'user_type') {
        paramConfig[key] = paramForm.value[key]
      }
    })
    
    // 更新 user_type
    paramConfig.user_type = paramForm.value.user_type
    
    // 保存参数
    const response = await indicatorApi.updateParams(
      currentIndicator.value.indicatorCode,
      paramConfig
    )
    
    if (response.data.success) {
      ElMessage.success('参数保存成功')
      paramDialogVisible.value = false
      loadIndicators() // 重新加载列表
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('保存参数失败: ' + error.message)
  } finally {
    savingParams.value = false
  }
}

const toggleIndicatorStatus = async (row) => {
  try {
    const response = await indicatorApi.toggleStatus(
      row.indicatorCode,
      row.isActive === 1
    )
    
    if (response.data.success) {
      ElMessage.success(`指标已${row.isActive === 1 ? '激活' : '停用'}`)
      loadStatistics() // 更新统计信息
    } else {
      ElMessage.error(response.data.message)
      // 恢复原状态
      row.isActive = row.isActive === 1 ? 0 : 1
    }
  } catch (error) {
    ElMessage.error('切换状态失败: ' + error.message)
    // 恢复原状态
    row.isActive = row.isActive === 1 ? 0 : 1
  }
}

const toggleCoreStatus = async (row) => {
  try {
    const response = await indicatorApi.updateParams(
      row.indicatorCode,
      { isCore: row.isCore }
    )
    
    if (response.data.success) {
      ElMessage.success(`指标已设置为${row.isCore === 1 ? '核心' : '扩展'}指标`)
      // 直接更新统计数据，不需要重新加载
      loadStatistics()
    } else {
      ElMessage.error(response.data.message)
      // 恢复原状态
      row.isCore = row.isCore === 1 ? 0 : 1
    }
  } catch (error) {
    ElMessage.error('切换核心状态失败: ' + error.message)
    // 恢复原状态
    row.isCore = row.isCore === 1 ? 0 : 1
  }
}

const handleSearch = () => {
  // 搜索逻辑已在计算属性中处理
}

const handleFilter = () => {
  // 过滤逻辑已在计算属性中处理
}

const getConfigurableFieldsCount = (row) => {
  if (row.paramConfig && row.paramConfig.configurable_fields) {
    return row.paramConfig.configurable_fields.length
  }
  return 0
}

const resetParamDialog = () => {
  currentIndicator.value = null
  paramForm.value = {}
  configurableFields.value = []
}

// 创建指标
const createIndicator = () => {
  isEditMode.value = false
  indicatorForm.value = {
    indicatorCode: '',
    indicatorName: '',
    indicatorDesc: '',
    isCore: 0,
    isActive: 1,
    userType: 'payer',
    paramConfig: {}
  }
  indicatorDialogVisible.value = true
}

// 编辑指标
const editIndicator = (row) => {
  isEditMode.value = true
  indicatorForm.value = {
    indicatorCode: row.indicatorCode,
    indicatorName: row.indicatorName,
    indicatorDesc: row.indicatorDesc,
    isCore: row.isCore,
    isActive: row.isActive,
    userType: getUserType(row),
    paramConfig: row.paramConfig || {}
  }
  indicatorDialogVisible.value = true
}

// 保存指标（创建或更新）
const saveIndicator = async () => {
  if (!indicatorFormRef.value) return
  
  await indicatorFormRef.value.validate(async (valid) => {
    if (valid) {
      // 验证参数配置JSON格式
      try {
        JSON.parse(paramConfigText.value)
      } catch (e) {
        ElMessage.error('参数配置JSON格式错误，请检查')
        return
      }
      
      savingIndicator.value = true
      try {
        const indicatorData = {
          indicatorCode: indicatorForm.value.indicatorCode,
          indicatorName: indicatorForm.value.indicatorName,
          indicatorDesc: indicatorForm.value.indicatorDesc,
          isCore: indicatorForm.value.isCore,
          isActive: indicatorForm.value.isActive,
          paramConfig: {
            ...indicatorForm.value.paramConfig,
            user_type: indicatorForm.value.userType
          }
        }
        
        let response
        if (isEditMode.value) {
          response = await indicatorApi.updateIndicator(indicatorForm.value.indicatorCode, indicatorData)
        } else {
          response = await indicatorApi.createIndicator(indicatorData)
        }
        
        if (response.data.success) {
          ElMessage.success(isEditMode.value ? '指标更新成功' : '指标创建成功')
          indicatorDialogVisible.value = false
          loadIndicators()
          loadStatistics()
        } else {
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        ElMessage.error((isEditMode.value ? '更新' : '创建') + '指标失败: ' + error.message)
      } finally {
        savingIndicator.value = false
      }
    }
  })
}

// 删除指标
const deleteIndicator = async (row) => {
  try {
    const response = await indicatorApi.deleteIndicator(row.indicatorCode)
    
    if (response.data.success) {
      ElMessage.success('指标删除成功')
      loadIndicators() // 重新加载列表，会自动更新统计
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('删除指标失败: ' + error.message)
  }
}

// 重置指标对话框
const resetIndicatorDialog = () => {
  isEditMode.value = false
  indicatorForm.value = {
    indicatorCode: '',
    indicatorName: '',
    indicatorDesc: '',
    isCore: 0,
    isActive: 1,
    userType: 'payer',
    paramConfig: {}
  }
  if (indicatorFormRef.value) {
    indicatorFormRef.value.resetFields()
  }
}
</script>

<style scoped>
.indicator-list {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
  width: 100%;
}

.page-header {
  background: white;
  padding: 20px 24px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 4px 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.page-subtitle {
  margin: 0;
  color: #909399;
  font-size: 13px;
  font-weight: normal;
}

.stats-cards {
  margin-bottom: 20px;
}

.stats-card {
  position: relative;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  cursor: pointer;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
}

.stats-card .el-card__body {
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
}

.stats-content {
  flex: 1;
}

.stats-number {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 5px;
}

.stats-label {
  font-size: 14px;
  color: #909399;
}

.stats-icon {
  font-size: 36px;
  opacity: 0.8;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
}

.filter-card .el-card__body {
  padding: 20px 24px;
  background: white;
}

.table-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
}

.table-card .el-card__body {
  padding: 0;
  background: white;
}

.el-table {
  border-radius: 0;
}

.el-table .el-table__row {
  cursor: pointer;
}

.el-table .el-table__row:hover {
  background-color: #f5f7fa;
}

.param-description {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
  margin-top: 4px;
  line-height: 1.5;
}

.form-description {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.5;
}

.param-config-editor {
  width: 100%;
}

/* 操作按钮样式 */
.operation-buttons {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
}

.operation-buttons .el-button {
  margin: 0;
}

/* 对话框样式优化 */
:deep(.param-dialog .el-dialog),
:deep(.indicator-dialog .el-dialog) {
  border-radius: 12px;
}

:deep(.indicator-dialog .el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

:deep(.indicator-dialog .el-dialog__body) {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.indicator-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
}

:deep(.param-dialog .el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

:deep(.param-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.param-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-cards .el-col {
    margin-bottom: 10px;
  }
  
  .page-header .el-row {
    flex-direction: column;
    gap: 15px;
  }
  
  .page-header .el-col:last-child {
    text-align: left !important;
  }
}
</style>
