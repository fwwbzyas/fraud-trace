<template>
  <div class="task-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-row justify="space-between" align="middle">
        <el-col :span="12">
          <h2 class="page-title">
            <svg 
              class="task-icon" 
              viewBox="0 0 24 24" 
              fill="none" 
              xmlns="http://www.w3.org/2000/svg"
            >
              <path 
                d="M9 5H7C5.89543 5 5 5.89543 5 7V19C5 20.1046 5.89543 21 7 21H17C18.1046 21 19 20.1046 19 19V7C19 5.89543 18.1046 5 17 5H15M9 5C9 6.10457 9.89543 7 11 7H13C14.1046 7 15 6.10457 15 5M9 5C9 3.89543 9.89543 3 11 3H13C14.1046 3 15 3.89543 15 5M9 12L11 14L15 10" 
                stroke="currentColor" 
                stroke-width="2" 
                stroke-linecap="round" 
                stroke-linejoin="round"
              />
            </svg>
            任务管理
          </h2>
          <p class="page-subtitle">配置规则、发起任务、追查电诈风险账户</p>
        </el-col>
        <el-col :span="12" style="text-align: right">
          <el-button 
            v-if="isAdmin" 
            type="primary" 
            :icon="Plus" 
            @click="openCreateTaskDialog"
          >
            新建任务
          </el-button>
          <el-button :icon="Refresh" @click="loadTasks">
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
            <div class="stats-label">总任务数</div>
          </div>
          <el-icon class="stats-icon" style="color: #409EFF"><DataBoard /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.runningCount || 0 }}</div>
            <div class="stats-label">执行中</div>
          </div>
          <el-icon class="stats-icon" style="color: #E6A23C"><Loading /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.completedCount || 0 }}</div>
            <div class="stats-label">已完成</div>
          </div>
          <el-icon class="stats-icon" style="color: #67C23A"><SuccessFilled /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.todayCount || 0 }}</div>
            <div class="stats-label">今日任务</div>
          </div>
          <el-icon class="stats-icon" style="color: #909399"><Calendar /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和筛选 -->
    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索任务名称..."
            :prefix-icon="Search"
            @input="handleSearch"
            clearable
          />
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="任务状态" @change="handleFilter" clearable>
            <el-option label="全部" value="" />
            <el-option label="待执行" value="0" />
            <el-option label="执行中" value="1" />
            <el-option label="已完成" value="2" />
            <el-option label="执行失败" value="3" />
          </el-select>
        </el-col>
      </el-row>
    </el-card>

    <!-- 任务列表 -->
    <el-card class="table-card">
      <el-table
        :data="filteredTasks"
        v-loading="loading"
        stripe
        style="width: 100%"
        @row-click="viewTaskResults"
        class="task-table"
      >
        <el-table-column prop="taskCode" label="任务编号" width="220">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.taskCode }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="taskName" label="任务名称" width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="task-name-text">{{ row.taskName }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="150" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="getStatusTagType(row.taskStatus)" 
              size="small"
              effect="light"
            >
              {{ getStatusName(row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="进度" width="180" align="center">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress 
                :percentage="getAnimatedProgress(row)" 
                :status="row.taskStatus === 3 ? 'exception' : (row.taskStatus === 2 ? 'success' : '')"
                :stroke-width="6"
                :show-text="false"
                style="width: 55px; display: inline-block;"
              />
              <span class="progress-text">{{ Math.round(getAnimatedProgress(row)) }}%</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="timeRange" label="分析范围" width="150" align="center">
          <template #default="{ row }">
            {{ row.timeRange }}天
          </template>
        </el-table-column>
        
        <el-table-column prop="resultCount" label="风险账户" width="150" align="center">
          <template #default="{ row }">
            <span v-if="row.resultCount !== null && row.resultCount !== undefined">
              {{ row.resultCount }}个
            </span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="240" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button
                v-if="row.taskStatus === 0"
                :icon="VideoPlay"
                @click.stop="startTask(row)"
                class="operation-btn icon-only"
                circle
                title="启动"
              />
              <el-button
                v-if="row.taskStatus === 1"
                :icon="VideoPause"
                @click.stop="stopTask(row)"
                class="operation-btn icon-only"
                circle
                title="停止"
              />
              <el-button
                v-if="row.taskStatus === 2"
                :icon="View"
                @click.stop="viewTaskResults(row)"
                class="operation-btn icon-only"
                circle
                title="查看结果"
              />
              <el-popconfirm
                v-if="isAdmin && row.taskStatus !== 1"
                title="确定删除这个任务吗？"
                @confirm="deleteTask(row)"
                width="200"
              >
                <template #reference>
                  <el-button
                    :icon="Delete"
                    @click.stop
                    class="operation-btn icon-only"
                    circle
                    title="删除"
                  />
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建任务分步式界面 -->
    <div v-if="createTaskDialogVisible" class="create-task-page">
      <div class="create-task-container">
        <!-- 顶部返回按钮 -->
        <div class="page-header-bar">
          <el-button 
            text 
            :icon="ArrowLeft" 
            @click="closeCreateTaskDialog"
            class="back-button"
          >
            返回任务列表
          </el-button>
        </div>
        
        <!-- 居中内容区域 -->
        <div class="form-content-wrapper">
          <!-- 步骤指示器 -->
          <div class="step-indicator">
            <div 
              v-for="(step, index) in taskSteps" 
              :key="index"
              class="step-indicator-item"
              :class="{ 
                'is-active': currentStep === index,
                'is-completed': currentStep > index
              }"
              @click="goToStep(index)"
            >
              <div class="step-indicator-number">
                <span v-if="currentStep > index" class="step-check-icon">
                  <el-icon><Check /></el-icon>
                </span>
                <span v-else>{{ index + 1 }}</span>
              </div>
              <div class="step-indicator-label">{{ step.title }}</div>
            </div>
          </div>
          
          <!-- 表单内容 -->
          <div class="form-content">
      <el-form 
        :model="createTaskForm" 
        :rules="createTaskRules"
        ref="createTaskFormRef"
        label-width="120px"
              class="step-form"
            >
              <!-- 步骤1: 基本信息 -->
              <div v-if="currentStep === 0" class="step-panel">
                <el-form-item label="任务ID">
                  <el-input 
                    v-model="createTaskForm.taskId" 
                    placeholder="请输入任务ID (可选,留空则由系统自动生成)"
                    clearable
                    size="large"
                  />
                  <div class="form-hint">
                    <el-icon><InfoFilled /></el-icon>
                    <span>留空则系统自动生成</span>
                  </div>
                </el-form-item>
                
        <el-form-item label="任务名称" prop="taskName">
          <el-input 
            v-model="createTaskForm.taskName" 
                    placeholder="请输入任务名称"
                    clearable
                    size="large"
          />
        </el-form-item>
        
                <el-form-item label="任务描述">
                  <el-input 
                    v-model="createTaskForm.description"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入任务描述"
                    resize="vertical"
                  />
                </el-form-item>
              </div>
            
              <!-- 步骤2: 关联指标 -->
              <div v-if="currentStep === 1" class="step-panel">
                <div class="step-panel-header">
                  <h3>选择关联的指标</h3>
                  <p class="step-panel-desc">请选择要用于分析的检测指标，至少选择一个指标</p>
                </div>
                
                <el-form-item 
                  prop="indicatorIds"
                  class="indicator-form-item"
                >
                  <div class="indicator-grid">
                    <div 
              v-for="indicator in availableIndicators" 
              :key="indicator.indicatorId"
                      class="indicator-card"
                      :class="{ 'is-selected': selectedIndicators.includes(indicator.indicatorId) }"
                      @click="toggleIndicator(indicator.indicatorId)"
                    >
                      <el-checkbox 
                        :model-value="selectedIndicators.includes(indicator.indicatorId)"
                        @change="(val) => toggleIndicator(indicator.indicatorId)"
                        @click.stop
                        class="indicator-checkbox"
                      />
                      <div class="indicator-card-content">
                        <div class="indicator-card-header">
                          <span class="indicator-name">{{ indicator.indicatorName }}</span>
              <el-tag 
                v-if="indicator.isCore === 1" 
                type="warning" 
                size="small" 
              >
                核心
              </el-tag>
                        </div>
                        <div class="indicator-code">{{ indicator.indicatorCode }}</div>
                        <div class="indicator-desc-text">{{ indicator.indicatorDesc }}</div>
                      </div>
                    </div>
                  </div>
        </el-form-item>
              </div>
            
              <!-- 步骤3: 规则配置 -->
              <div v-if="currentStep === 2" class="step-panel">
                <div class="step-panel-header">
                  <h3>配置规则表达式</h3>
                  <p class="step-panel-desc">配置业务规则，用于识别风险账户</p>
                </div>
                
                <el-form-item label="规则表达式" prop="ruleLogic">
          <div class="visual-rule-builder">
            <RuleBuilder
              :key="formVersion"
              v-model="createTaskForm.ruleLogic"
              :available-indicators="availableIndicators"
              @rule-change="handleRuleChange"
            />
          </div>
        </el-form-item>
        
        <el-form-item label="任务筛选范围" prop="taskTimeRange">
          <el-input-number
            v-model="createTaskForm.taskTimeRange"
            :min="1"
            :max="1095"
            controls-position="right"
                    size="large"
            style="width: 200px"
          />
                  <span class="form-hint-text">天（筛选最近多少天有交易的账户，建议365天）</span>
        </el-form-item>
        
        <el-form-item label="指标计算窗口" prop="indicatorTimeWindow">
          <el-input-number
            v-model="createTaskForm.indicatorTimeWindow"
            :min="1"
            :max="365"
            controls-position="right"
                    size="large"
            style="width: 200px"
          />
                  <span class="form-hint-text">天（从最后交易时间往前推多少天，建议30天）</span>
        </el-form-item>
              </div>
            
              <!-- 步骤4: 确认提交 -->
              <div v-if="currentStep === 3" class="step-panel">
                <div class="step-panel-header">
                  <h3>确认任务配置并提交</h3>
                  <p class="step-panel-desc">请确认以下配置信息，确认无误后提交</p>
                </div>
                
                <div class="confirm-info">
                  <el-descriptions :column="1" border>
                    <el-descriptions-item label="任务ID">
                      {{ createTaskForm.taskId || '系统自动生成' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="任务名称">
                      {{ createTaskForm.taskName || '-' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="任务描述">
                      {{ createTaskForm.description || '-' }}
                    </el-descriptions-item>
                    <el-descriptions-item label="关联指标">
                      <div class="selected-indicators-list">
                        <el-tag 
                          v-for="id in selectedIndicators" 
                          :key="id"
                          :type="getIndicatorById(id)?.isCore === 1 ? 'warning' : 'info'"
                          style="margin-right: 8px; margin-bottom: 4px"
                        >
                          {{ getIndicatorById(id)?.indicatorName }}
                        </el-tag>
                        <span v-if="selectedIndicators.length === 0" style="color: #909399;">未选择指标</span>
                      </div>
                    </el-descriptions-item>
                    <el-descriptions-item label="任务筛选范围">
                      {{ createTaskForm.taskTimeRange }}天
                    </el-descriptions-item>
                    <el-descriptions-item label="指标计算窗口">
                      {{ createTaskForm.indicatorTimeWindow }}天
                    </el-descriptions-item>
                  </el-descriptions>
                </div>
              </div>
      </el-form>
      
            <!-- 底部按钮 -->
            <div class="step-footer">
              <el-button 
                v-if="currentStep > 0"
                @click="prevStep"
                size="large"
              >
                上一步
        </el-button>
              <el-button 
                v-if="currentStep < 3"
                @click="nextStep"
                size="large"
              >
                下一步
              </el-button>
              <el-button 
                v-if="currentStep === 3"
                @click="submitCreateTask"
                :loading="creating"
                size="large"
              >
                确认提交
              </el-button>
              <el-button 
                @click="closeCreateTaskDialog"
                size="large"
              >
                取消
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Refresh, Search, VideoPlay, VideoPause, 
  View, Delete, DataBoard, Loading, SuccessFilled, Calendar,
  Document, Operation, Clock, Check, ArrowLeft, InfoFilled
} from '@element-plus/icons-vue'
import { taskApi } from '../api/task'
import { indicatorApi } from '../api/indicator'
import RuleBuilder from './RuleBuilder.vue'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { isAdmin } = useAuth()

// 响应式数据
const loading = ref(false)
const creating = ref(false)
const tasks = ref([])
const statistics = ref({})
const searchKeyword = ref('')
const filterStatus = ref('')

// 进度条动画相关
const animatedProgress = ref({}) // 存储每个任务的动画进度 { taskId: progress }
const progressTimers = ref({}) // 存储每个任务的定时器 { taskId: timer }
const progressStartTimes = ref({}) // 存储每个任务的开始时间 { taskId: startTime }

// 创建任务相关
const createTaskDialogVisible = ref(false)
const createTaskFormRef = ref(null)
const selectedIndicators = ref([]) // 不再默认选择指标
const availableIndicators = ref([])
const currentStep = ref(0) // 当前步骤
const formVersion = ref(0) // 用于强制重置子组件状态

// 任务步骤定义
const taskSteps = [
  {
    title: '基本信息',
    desc: '填写任务基本信息'
  },
  {
    title: '关联指标',
    desc: '选择关联的指标'
  },
  {
    title: '规则配置',
    desc: '配置规则表达式'
  },
  {
    title: '确认提交',
    desc: '确认任务配置并提交'
  }
]

// 规则构建器相关（已删除固定指标逻辑）

// 创建任务表单
const createTaskForm = ref({
  taskId: '',
  taskName: '',
  indicatorIds: '',
  ruleLogic: '{"type": "separate_rules", "victimRule": "", "suspectRule": ""}',
  taskTimeRange: 365,        // 任务筛选时间范围（天）- 决定分析哪些账户
  indicatorTimeWindow: 30,   // 指标计算时间窗口（天）- 从最后交易时间往前推多少天
  description: '',
  // 🔧 新增：规则配置参数
  victimIndicatorA: 1,       // 受害人A指标阈值（快进快出次数 > 1）
  victimIndicatorB: 0,       // 受害人B指标阈值（登录后转账次数 > 0）
  suspiciousThreshold: 500000  // 可疑分子阈值（累计入账 >= 50万）
})

// 表单验证规则
const createTaskRules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 2, max: 50, message: '任务名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  indicatorIds: [
    { 
      validator: (rule, value, callback) => {
        if (selectedIndicators.value.length === 0) {
          callback(new Error('请至少选择一个指标'))
        } else {
          callback()
        }
      }, 
      trigger: 'change' 
    }
  ],
  ruleLogic: [
    { required: true, message: '请输入业务规则', trigger: 'blur' }
  ],
  taskTimeRange: [
    { required: true, message: '请选择任务筛选时间范围', trigger: 'blur' }
  ],
  indicatorTimeWindow: [
    { required: true, message: '请选择指标计算时间窗口', trigger: 'blur' }
  ]
}

// 计算属性
const filteredTasks = computed(() => {
  let result = [...tasks.value]
  
  // 搜索过滤
  if (searchKeyword.value) {
    result = result.filter(item => 
      item.taskName.includes(searchKeyword.value) ||
      item.taskCode.includes(searchKeyword.value)
    )
  }
  
  // 状态过滤
  if (filterStatus.value !== '') {
    result = result.filter(item => item.taskStatus === parseInt(filterStatus.value))
  }
  
  return result
})

// 定时刷新相关
let refreshTimer = null
const REFRESH_INTERVAL = 5000 // 5秒刷新一次

// 启动定时刷新
const startAutoRefresh = () => {
  // 清除旧的定时器
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  
  // 检查是否有执行中的任务
  const hasRunningTasks = tasks.value.some(task => task.taskStatus === 1) // 1 = 执行中
  
  // 如果有执行中的任务，启动定时刷新
  if (hasRunningTasks) {
    refreshTimer = setInterval(() => {
      // 检查是否还有执行中的任务
      const stillHasRunningTasks = tasks.value.some(task => task.taskStatus === 1)
      
      // 如果没有执行中的任务了，停止定时刷新
      if (!stillHasRunningTasks) {
        stopAutoRefresh()
        return
      }
      
      // 只刷新任务列表，不显示loading，避免干扰用户操作
      taskApi.getAllTasks().then(response => {
        if (response.data.success) {
          tasks.value = response.data.data
          // 同时更新统计信息
          loadStatistics()
          
          // 再次检查是否还有执行中的任务
          const hasRunning = tasks.value.some(task => task.taskStatus === 1)
          if (!hasRunning) {
            stopAutoRefresh()
          }
        }
      }).catch(error => {
        console.error('自动刷新任务列表失败:', error)
      })
    }, REFRESH_INTERVAL)
  }
}

// 停止定时刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 生命周期
onMounted(() => {
  loadTasks()
  loadStatistics()
  loadAvailableIndicators()
  
  // 启动定时刷新
  startAutoRefresh()
})

// 组件卸载时清理定时器
onUnmounted(() => {
  stopAutoRefresh()
  // 清理所有进度条动画定时器
  Object.values(progressTimers.value).forEach(timer => {
    if (timer) clearInterval(timer)
  })
  progressTimers.value = {}
})

// 方法
const loadTasks = async () => {
  loading.value = true
  try {
    const response = await taskApi.getAllTasks()
    if (response.data.success) {
      const oldTasks = tasks.value
      tasks.value = response.data.data
      
      // 管理进度条动画
      tasks.value.forEach(task => {
        if (task.taskStatus === 1) {
          // 任务执行中，启动或继续动画
          if (!progressTimers.value[task.taskId]) {
            startProgressAnimation(task)
          }
        } else {
          // 任务已完成或停止，停止动画
          stopProgressAnimation(task.taskId)
        }
      })
      
      // 清理已不存在的任务的动画
      const currentTaskIds = new Set(tasks.value.map(t => t.taskId))
      Object.keys(progressTimers.value).forEach(taskId => {
        if (!currentTaskIds.has(parseInt(taskId))) {
          stopProgressAnimation(parseInt(taskId))
        }
      })
      
      // 检查是否有执行中的任务，如果有则启动/继续定时刷新
      const hasRunningTasks = tasks.value.some(task => task.taskStatus === 1)
      if (hasRunningTasks) {
        startAutoRefresh()
      } else {
        stopAutoRefresh()
      }
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('获取任务列表失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const response = await taskApi.getTaskStatistics()
    if (response.data.success) {
      statistics.value = response.data.data
    }
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }
}

const loadAvailableIndicators = async () => {
  try {
    const response = await indicatorApi.getActiveIndicators()
    if (response.data.success) {
      availableIndicators.value = response.data.data
      console.log('📊 任务管理-可用指标:', availableIndicators.value)
    }
  } catch (error) {
    console.error('获取指标列表失败:', error)
  }
}

const startTask = async (row) => {
  try {
    const response = await taskApi.startTask(row.taskId)
    if (response.data.success) {
      ElMessage.success('任务启动成功，系统将自动更新进度')
      // 立即启动进度条动画
      startProgressAnimation({ ...row, taskStatus: 1, progress: 0 })
      loadTasks()
      loadStatistics()
      // 启动任务后，启动定时刷新
      startAutoRefresh()
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('启动任务失败: ' + error.message)
  }
}

const stopTask = async (row) => {
  try {
    await ElMessageBox.confirm('确定要停止这个任务吗？', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await taskApi.stopTask(row.taskId)
    if (response.data.success) {
      ElMessage.success('任务已停止')
      loadTasks()
      loadStatistics()
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('停止任务失败: ' + error.message)
    }
  }
}

const deleteTask = async (row) => {
  try {
    const response = await taskApi.deleteTask(row.taskId)
    if (response.data.success) {
      ElMessage.success('任务删除成功')
      loadTasks()
      loadStatistics()
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('删除任务失败: ' + error.message)
  }
}

const viewTaskResults = (row) => {
  if (row.taskStatus !== 2) {
    ElMessage.warning('只有已完成的任务才能查看结果')
    return
  }
  
  // 跳转到任务结果页面
  router.push(`/tasks/${row.taskId}/results`)
}

const submitCreateTask = async () => {
  if (!createTaskFormRef.value) return
  
  await createTaskFormRef.value.validate(async (valid) => {
    if (valid) {
      creating.value = true
      try {
        // 构建指标ID字符串
        const indicatorIds = selectedIndicators.value.join(',')
        
        // 🔧 修复：从RuleBuilder生成的ruleLogic中解析规则
        let victimRule = ''
        let suspectRule = ''
        
        try {
          const ruleLogic = JSON.parse(createTaskForm.value.ruleLogic)
          victimRule = ruleLogic.victimRule || ''
          suspectRule = ruleLogic.suspectRule || ''
        } catch (e) {
          console.error('解析规则失败:', e)
          ElMessage.error('规则配置格式错误，请重新配置')
          return
        }
        
        const ruleConfig = {
          victimRule: victimRule,
          suspectRule: suspectRule,
          description: createTaskForm.value.description || '电诈风险账户追查分析'
        }
        
        const taskData = {
          taskName: createTaskForm.value.taskName,
          indicatorIds: indicatorIds,
          ruleConfig: ruleConfig,
          taskTimeRange: createTaskForm.value.taskTimeRange,           // 任务筛选时间范围
          indicatorTimeWindow: createTaskForm.value.indicatorTimeWindow, // 指标计算时间窗口
          description: createTaskForm.value.description
        }
        
        const response = await taskApi.createTask(taskData)
        if (response.data.success) {
          ElMessage.success('任务创建成功，请手动启动任务')
          closeCreateTaskDialog()
          
          loadTasks()
          loadStatistics()
        } else {
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        ElMessage.error('创建任务失败: ' + error.message)
      } finally {
        creating.value = false
      }
    }
  })
}

// 规则构建器事件处理
const handleRuleChange = (ruleData) => {
  console.log('规则变化:', ruleData)
  // 可以在这里添加实时验证逻辑
  if (ruleData.isValid) {
    console.log('规则语法正确:', ruleData.rule)
  } else {
    console.log('规则语法错误:', ruleData.errors)
  }
}

const resetCreateTaskForm = () => {
  createTaskForm.value = {
    taskId: '',
    taskName: '',
    indicatorIds: '',
    ruleLogic: '{"type": "separate_rules", "victimRule": "", "suspectRule": ""}',
    taskTimeRange: 365,
    indicatorTimeWindow: 30,
    description: '',
    // 🔧 新增：重置规则配置参数
    victimIndicatorA: 1,
    victimIndicatorB: 0,
    suspiciousThreshold: 500000
  }
  selectedIndicators.value = []
  currentStep.value = 0
  formVersion.value += 1
  if (createTaskFormRef.value) {
    createTaskFormRef.value.resetFields()
    createTaskFormRef.value.clearValidate?.()
  }
}

const closeCreateTaskDialog = () => {
  createTaskDialogVisible.value = false
  resetCreateTaskForm()
}

// 打开创建任务对话框
const openCreateTaskDialog = () => {
  resetCreateTaskForm()
  createTaskDialogVisible.value = true
}

// 步骤导航
const nextStep = async () => {
  // 验证当前步骤
  if (currentStep.value === 0) {
    // 验证基本信息
    if (!createTaskFormRef.value) return
    try {
      await createTaskFormRef.value.validateField('taskName')
      currentStep.value++
    } catch {
      return
    }
  } else if (currentStep.value === 1) {
    // 验证是否选择了指标
    if (!createTaskFormRef.value) return
    try {
      await createTaskFormRef.value.validateField('indicatorIds')
      currentStep.value++
    } catch {
      ElMessage.warning('请至少选择一个指标')
      return
    }
  } else if (currentStep.value === 2) {
    // 验证规则配置
    if (!createTaskFormRef.value) return
    try {
      await createTaskFormRef.value.validateField('ruleLogic')
      await createTaskFormRef.value.validateField('taskTimeRange')
      await createTaskFormRef.value.validateField('indicatorTimeWindow')
      currentStep.value++
    } catch {
      return
    }
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 切换指标选择
const toggleIndicator = (indicatorId) => {
  const index = selectedIndicators.value.indexOf(indicatorId)
  if (index > -1) {
    selectedIndicators.value.splice(index, 1)
  } else {
    selectedIndicators.value.push(indicatorId)
  }
  // 触发验证
  if (createTaskFormRef.value) {
    createTaskFormRef.value.validateField('indicatorIds')
  }
}

// 根据ID获取指标
const getIndicatorById = (id) => {
  return availableIndicators.value.find(ind => ind.indicatorId === id)
}

// 跳转到指定步骤
const goToStep = (stepIndex) => {
  // 只能跳转到已完成或当前步骤
  if (stepIndex <= currentStep.value) {
    currentStep.value = stepIndex
  }
}

const handleSearch = () => {
  // 搜索逻辑已在计算属性中处理
}

const handleFilter = () => {
  // 过滤逻辑已在计算属性中处理
}

const getStatusName = (status) => {
  const statusMap = {
    0: '待执行',
    1: '执行中',
    2: '已完成',
    3: '执行失败'
  }
  return statusMap[status] || '未知'
}

const getStatusTagType = (status) => {
  const typeMap = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger'
  }
  return typeMap[status] || 'info'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 进度条动画相关函数
const startProgressAnimation = (task) => {
  const taskId = task.taskId
  
  // 如果已经有定时器在运行，不重复启动
  if (progressTimers.value[taskId]) {
    return
  }
  
  // 记录开始时间
  if (!progressStartTimes.value[taskId]) {
    progressStartTimes.value[taskId] = Date.now()
  }
  
  // 初始化动画进度（从后端返回的进度或0开始）
  if (animatedProgress.value[taskId] === undefined) {
    animatedProgress.value[taskId] = task.progress || 0
  }
  
  // 启动动画定时器，每100ms更新一次
  progressTimers.value[taskId] = setInterval(() => {
    // 查找当前任务的最新状态
    const currentTask = tasks.value.find(t => t.taskId === taskId)
    if (!currentTask || currentTask.taskStatus !== 1) {
      // 任务已完成或停止，停止动画
      stopProgressAnimation(taskId)
      return
    }
    
    const startTime = progressStartTimes.value[taskId]
    const elapsed = Date.now() - startTime // 已过时间（毫秒）
    
    // 基于时间的进度估算（约40秒达到100%）
    // 前10秒：快速到25%，10-25秒：25%到70%，25-40秒：70%到100%
    let estimatedProgress = 0
    const elapsedSeconds = elapsed / 1000
    const totalDuration = 40 // 总时长40秒
    
    if (elapsedSeconds < 10) {
      // 前10秒：快速到25%
      estimatedProgress = 25 * (elapsedSeconds / 10)
    } else if (elapsedSeconds < 25) {
      // 10-25秒：25%到70%
      estimatedProgress = 25 + 45 * ((elapsedSeconds - 10) / 15)
    } else if (elapsedSeconds < totalDuration) {
      // 25-40秒：70%到100%
      estimatedProgress = 70 + 30 * ((elapsedSeconds - 25) / 15)
    } else {
      // 40秒后：保持100%
      estimatedProgress = 100
    }
    
    // 如果后端返回的进度更大，使用后端进度
    const backendProgress = currentTask.progress || 0
    const currentProgress = Math.max(estimatedProgress, backendProgress, animatedProgress.value[taskId] || 0)
    
    // 平滑增长，每次最多增加1%（加快速度）
    const targetProgress = Math.min(currentProgress, 100) // 可以到100%
    const currentAnimated = animatedProgress.value[taskId] || 0
    
    if (targetProgress > currentAnimated) {
      // 加快增长速度，每次最多增加1%
      animatedProgress.value[taskId] = Math.min(currentAnimated + 1.0, targetProgress)
    } else if (targetProgress < currentAnimated) {
      // 如果后端进度更新了，平滑过渡到后端进度
      animatedProgress.value[taskId] = currentAnimated + (targetProgress - currentAnimated) * 0.15
    }
  }, 100) // 每100ms更新一次
}

const stopProgressAnimation = (taskId) => {
  if (progressTimers.value[taskId]) {
    clearInterval(progressTimers.value[taskId])
    delete progressTimers.value[taskId]
  }
  // 不删除 progressStartTimes 和 animatedProgress，保留最终状态
}

// 获取动画进度
const getAnimatedProgress = (task) => {
  const taskId = task.taskId
  
  // 如果任务已完成或失败，直接返回后端进度，并停止动画
  if (task.taskStatus === 2 || task.taskStatus === 3) {
    stopProgressAnimation(taskId)
    // 平滑过渡到最终进度
    const finalProgress = task.progress || 0
    if (animatedProgress.value[taskId] !== undefined) {
      animatedProgress.value[taskId] = finalProgress
    }
    return finalProgress
  }
  
  // 如果任务执行中，返回动画进度
  if (task.taskStatus === 1) {
    // 如果后端返回的进度更大，使用后端进度
    const backendProgress = task.progress || 0
    const animated = animatedProgress.value[taskId] || 0
    return Math.max(backendProgress, animated)
  }
  
  // 其他状态返回后端进度
  return task.progress || 0
}
</script>

<style scoped>
.task-management {
  padding: 24px 32px;
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
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

.task-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
  color: #409EFF;
  flex-shrink: 0;
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
  height: auto;
}

.el-table .el-table__row:hover {
  background-color: #f5f7fa;
  transition: background-color 0.2s ease;
}

/* 优化表格行间距，让整体更舒适美观 */
:deep(.task-table .el-table__row) {
  height: auto;
  border-bottom: 1px solid #f0f0f0;
}

/* 增加行与行之间的间距，让表格更美观 */
:deep(.task-table .el-table__body tr) {
  height: auto;
  min-height: 64px;
}

.rule-tips {
  margin-top: 8px;
}

.visual-rule-builder {
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

/* 任务名称样式 */
.task-name-text {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
}

/* 进度单元格样式 */
.progress-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.progress-text {
  font-size: 13px;
  color: #606266;
  min-width: 38px;
  text-align: left;
  font-weight: 500;
}

.empty-text {
  color: #c0c4cc;
}

/* 操作按钮样式 */
.operation-buttons {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
}

.operation-btn {
  flex-shrink: 0;
}

.operation-btn.icon-only {
  width: 32px !important;
  height: 32px !important;
  padding: 0 !important;
  min-width: 32px !important;
  max-width: 32px !important;
  border-radius: 50% !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.operation-btn.icon-only:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
  transition: all 0.2s ease;
}

.operation-btn.icon-only .el-icon {
  font-size: 16px;
  margin: 0 !important;
}

/* 强制统一按钮颜色和样式 - 使用最高优先级 */
.task-management .operation-buttons .el-button,
.task-management :deep(.operation-buttons .el-button),
.task-management :deep(.operation-buttons .el-button.icon-only) {
  width: 32px !important;
  height: 32px !important;
  padding: 0 !important;
  min-width: 32px !important;
  max-width: 32px !important;
  border-radius: 50% !important;
  background-color: #ffffff !important;
  border-color: #dcdfe6 !important;
  color: #606266 !important;
}

.task-management :deep(.operation-buttons .el-button:hover),
.task-management :deep(.operation-buttons .el-button.icon-only:hover) {
  background-color: #ecf5ff !important;
  border-color: #b3d8ff !important;
  color: #409eff !important;
}

.task-management :deep(.operation-buttons .el-button:active),
.task-management :deep(.operation-buttons .el-button.icon-only:active) {
  background-color: #ecf5ff !important;
  border-color: #409eff !important;
  color: #409eff !important;
}

/* 覆盖所有可能的按钮类型颜色 - 使用最高优先级 */
.task-management :deep(.operation-buttons .el-button--primary),
.task-management :deep(.operation-buttons .el-button--success),
.task-management :deep(.operation-buttons .el-button--warning),
.task-management :deep(.operation-buttons .el-button--danger),
.task-management :deep(.operation-buttons .el-button--info),
.task-management :deep(.operation-buttons .el-button.is-plain),
.task-management :deep(.operation-buttons .el-button--primary.is-plain),
.task-management :deep(.operation-buttons .el-button--success.is-plain),
.task-management :deep(.operation-buttons .el-button--warning.is-plain),
.task-management :deep(.operation-buttons .el-button--danger.is-plain) {
  background-color: #ffffff !important;
  border-color: #dcdfe6 !important;
  color: #606266 !important;
}

.task-management :deep(.operation-buttons .el-button--primary:hover),
.task-management :deep(.operation-buttons .el-button--success:hover),
.task-management :deep(.operation-buttons .el-button--warning:hover),
.task-management :deep(.operation-buttons .el-button--danger:hover),
.task-management :deep(.operation-buttons .el-button--info:hover),
.task-management :deep(.operation-buttons .el-button.is-plain:hover),
.task-management :deep(.operation-buttons .el-button--primary.is-plain:hover),
.task-management :deep(.operation-buttons .el-button--success.is-plain:hover),
.task-management :deep(.operation-buttons .el-button--warning.is-plain:hover),
.task-management :deep(.operation-buttons .el-button--danger.is-plain:hover) {
  background-color: #ecf5ff !important;
  border-color: #b3d8ff !important;
  color: #409eff !important;
}

.task-management :deep(.operation-buttons .el-button.icon-only .el-button__content),
.task-management :deep(.operation-buttons .el-button .el-button__content) {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 100% !important;
  height: 100% !important;
  padding: 0 !important;
}

.task-management :deep(.operation-buttons .el-button .el-icon),
.task-management :deep(.operation-buttons .el-button.icon-only .el-icon) {
  display: inline-flex !important;
  margin: 0 !important;
  font-size: 16px !important;
  color: inherit !important;
}

/* 表格样式优化，确保占满宽度并减少列间距 */
.task-table {
  width: 100% !important;
}

:deep(.task-table .el-table) {
  width: 100% !important;
  table-layout: auto;
}

:deep(.task-table .el-table__body-wrapper) {
  width: 100% !important;
}

:deep(.task-table .el-table__header-wrapper) {
  width: 100% !important;
}

/* 重新设计所有列的间距，让整体紧凑且协调 */
/* 表头行间距 */
:deep(.task-table .el-table th) {
  padding: 16px 10px !important;
  height: 52px;
  line-height: 20px;
}

/* 数据行间距 - 更舒适的行高，增加行间距让表格更美观 */
:deep(.task-table .el-table td) {
  padding: 20px 10px !important;
  height: auto;
  min-height: 64px;
  line-height: 24px;
}

/* 首列：任务编号 */
:deep(.task-table .el-table th:first-child) {
  padding-left: 20px !important;
  padding-right: 12px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.task-table .el-table td:first-child) {
  padding-left: 20px !important;
  padding-right: 12px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 任务名称列：紧凑间距，避免与状态列之间留白 */
:deep(.task-table .el-table th:nth-child(2)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.task-table .el-table td:nth-child(2)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 状态列：紧贴任务名称，减少左侧间距 */
:deep(.task-table .el-table th:nth-child(3)) {
  padding-left: 12px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.task-table .el-table td:nth-child(3)) {
  padding-left: 12px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 进度列 - 合理的左右间距 */
:deep(.task-table .el-table th:nth-child(4)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.task-table .el-table td:nth-child(4)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 分析范围列 - 合理的左右间距 */
:deep(.task-table .el-table th:nth-child(5)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.task-table .el-table td:nth-child(5)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 风险账户列 - 合理的左右间距 */
:deep(.task-table .el-table th:nth-child(6)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.task-table .el-table td:nth-child(6)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 创建时间列 - 合理的左右间距 */
:deep(.task-table .el-table th:nth-child(7)) {
  padding-left: 16px !important;
  padding-right: 20px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.task-table .el-table td:nth-child(7)) {
  padding-left: 16px !important;
  padding-right: 20px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 操作列：末列 - 合理的左右间距 */
:deep(.task-table .el-table th:last-child) {
  padding-left: 20px !important;
  padding-right: 20px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.task-table .el-table td:last-child) {
  padding-left: 20px !important;
  padding-right: 20px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 确保表格占满容器 */
.table-card {
  width: 100%;
  max-width: 100%;
}

:deep(.table-card .el-card__body) {
  padding: 0;
  width: 100%;
  max-width: 100%;
}

/* 确保表格充分利用空间，使用固定布局让各列等比例分配 */
:deep(.task-table) {
  width: 100% !important;
  table-layout: fixed;
}

:deep(.task-table .el-table__body-wrapper),
:deep(.task-table .el-table__header-wrapper) {
  width: 100% !important;
}

/* 确保表格主体占满容器 */
:deep(.task-table .el-table__body),
:deep(.task-table .el-table__header) {
  width: 100% !important;
  table-layout: fixed;
}

/* 让表格自动拉伸占满容器 */
:deep(.task-table .el-table) {
  width: 100% !important;
}


/* 创建任务分步式界面样式 */
.create-task-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #f5f7fa;
  z-index: 2000;
  overflow-y: auto;
}

.create-task-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
}

/* 顶部返回按钮 */
.page-header-bar {
  padding: 20px 40px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
}

.back-button {
  padding: 8px 0;
  color: #606266;
  font-size: 14px;
}

.back-button:hover {
  color: #409EFF;
}

/* 居中内容区域 */
.form-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

/* 步骤指示器 */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 40px;
  background: white;
  padding: 30px 60px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  position: relative;
}

.step-indicator-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  cursor: pointer;
  flex: 1;
  z-index: 2;
}

.step-indicator-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: calc(50% + 30px);
  top: 20px;
  width: calc(100% - 60px);
  height: 2px;
  background: #e5e7eb;
  z-index: 1;
}

.step-indicator-item.is-completed:not(:last-child)::after {
  background: #67C23A;
}

.step-indicator-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #909399;
  border: 2px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  transition: all 0.3s ease;
  position: relative;
  z-index: 2;
  margin-bottom: 8px;
}

.step-indicator-item.is-active .step-indicator-number {
  background: #409EFF;
  color: white;
  border-color: #409EFF;
}

.step-indicator-item.is-completed .step-indicator-number {
  background: #67C23A;
  color: white;
  border-color: #67C23A;
}

.step-check-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.step-indicator-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

.step-indicator-item.is-active .step-indicator-label {
  color: #409EFF;
  font-weight: 600;
}

.step-indicator-item:not(.is-active):not(.is-completed) .step-indicator-label {
  color: #909399;
}

/* 表单内容 */
.form-content {
  width: 100%;
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.step-form {
  flex: 1;
  padding: 40px 50px;
  overflow-y: auto;
}

.step-panel {
  width: 100%;
}

.step-panel-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.step-panel-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.step-panel-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.form-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.form-hint-text {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

/* 指标列表布局 */
.indicator-form-item {
  display: flex;
  justify-content: center;
}

.indicator-form-item :deep(.el-form-item__content) {
  width: 100%;
  display: flex;
  justify-content: center;
}

.indicator-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
  width: 100%;
  max-width: 900px;
}

.indicator-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-height: 100px;
  box-sizing: border-box;
  overflow: hidden;
}

.indicator-checkbox {
  flex-shrink: 0;
  margin-top: 0;
}

.indicator-card:hover {
  border-color: #409EFF;
  background: #f5f9ff;
}

.indicator-card.is-selected {
  border-color: #409EFF;
  background: #ecf5ff;
}

.indicator-card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
}

.indicator-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0;
  flex-wrap: wrap;
}

.indicator-card-header .indicator-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.indicator-code {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
  margin-top: 0;
}

.indicator-desc-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-top: 0;
}

/* 确认信息 */
.confirm-info {
  margin-top: 20px;
}

.selected-indicators-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 底部按钮 */
.step-footer {
  padding: 24px 50px;
  background: #fafafa;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: center;
  gap: 16px;
}

/* 规则构建器样式 */
.visual-rule-builder {
  margin: 0;
  border-radius: 6px;
  overflow: visible;
  border: 1px solid #e5e7eb;
  width: 100%;
}

/* 表单项样式 */
:deep(.step-form .el-form-item) {
  margin-bottom: 24px;
}

:deep(.step-form .el-form-item__label) {
  font-weight: 500;
  color: #606266;
  font-size: 14px;
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

  .operation-buttons {
    flex-wrap: wrap;
    gap: 6px;
    justify-content: center;
  }

  .operation-btn.icon-only {
    width: 28px !important;
    height: 28px !important;
    min-width: 28px !important;
    max-width: 28px !important;
  }
  
  .operation-btn.icon-only .el-icon {
    font-size: 14px !important;
  }
}
</style>
