<template>
  <div class="transfer-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-row justify="space-between" align="middle">
        <el-col :span="12">
          <h2 class="page-title">
            <el-icon style="margin-right: 8px"><Money /></el-icon>
            转账记录管理
          </h2>
          <p class="page-subtitle">查看和分析所有转账交易记录</p>
        </el-col>
        <el-col :span="12" style="text-align: right">
          <el-button :icon="Refresh" @click="loadTransferRecords">
            刷新
          </el-button>
          <el-button type="primary" :icon="Download" @click="exportRecords">
            导出记录
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.total_count || 0 }}</div>
            <div class="stats-label">总转账笔数</div>
          </div>
          <el-icon class="stats-icon" style="color: #409EFF"><DataBoard /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.total_amount_display || '0.00' }}</div>
            <div class="stats-label">总转账金额</div>
          </div>
          <el-icon class="stats-icon" style="color: #67C23A"><Money /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.large_amount_count || 0 }}</div>
            <div class="stats-label">大额交易</div>
          </div>
          <el-icon class="stats-icon" style="color: #E6A23C"><Warning /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.today_count || 0 }}</div>
            <div class="stats-label">今日交易</div>
          </div>
          <el-icon class="stats-icon" style="color: #909399"><Calendar /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和筛选 -->
    <el-card class="filter-card">
      <div class="search-tips">
        <el-alert
          title="搜索提示"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <div style="font-size: 13px; line-height: 1.4;">
              • <strong>转出账户</strong>：只搜索作为付款方的账户<br>
              • <strong>转入账户</strong>：只搜索作为收款方的账户<br>
              • <strong>组合搜索</strong>：同时填写转出和转入账户将精确匹配转账关系
            </div>
          </template>
        </el-alert>
      </div>
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="流水号">
          <el-input
            v-model="searchForm.transferNo"
            placeholder="输入转账流水号"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        
        <el-form-item label="转出账户">
          <el-input
            v-model="searchForm.fromAccountName"
            placeholder="输入转出账户名"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        
        <el-form-item label="转入账户">
          <el-input
            v-model="searchForm.toAccountName"
            placeholder="输入转入账户名"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        
        <el-form-item label="金额范围">
          <el-input-number
            v-model="searchForm.minAmount"
            placeholder="最小金额"
            :min="0"
            :precision="2"
            style="width: 120px"
          />
          <span style="margin: 0 8px">-</span>
          <el-input-number
            v-model="searchForm.maxAmount"
            placeholder="最大金额"
            :min="0"
            :precision="2"
            style="width: 120px"
          />
        </el-form-item>
        
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch" :loading="loading">
            搜索
          </el-button>
          <el-button :icon="RefreshLeft" @click="resetSearch">
            重置
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="quick-filters">
        <el-button-group>
          <el-button 
            :type="quickFilter === 'all' ? 'primary' : ''"
            @click="applyQuickFilter('all')"
          >
            全部
          </el-button>
          <el-button 
            :type="quickFilter === 'large' ? 'primary' : ''"
            @click="applyQuickFilter('large')"
          >
            大额交易(>=5万)
          </el-button>
          <el-button 
            :type="quickFilter === 'today' ? 'primary' : ''"
            @click="applyQuickFilter('today')"
          >
            今日交易
          </el-button>
          <el-button 
            :type="quickFilter === 'week' ? 'primary' : ''"
            @click="applyQuickFilter('week')"
          >
            本周交易
          </el-button>
        </el-button-group>
      </div>
    </el-card>

    <!-- 关联提示条 -->
    <div v-if="isViewingRelated" class="related-strip-card">
      <div class="related-strip-info">
        <el-icon class="related-icon"><Link /></el-icon>
        <div>
          <div class="strip-title">正在查看关联转账</div>
          <div class="strip-desc">{{ relatedDescription }}</div>
        </div>
      </div>
      <el-button type="primary" :icon="ArrowLeft" @click="backToAllTransfers">
        返回
      </el-button>
    </div>

    <!-- 转账记录列表 -->
    <el-card class="table-card">
      <el-table
        :data="transferRecords"
        v-loading="loading"
        stripe
        style="width: 100%"
        @row-click="viewTransferDetail"
        class="transfer-table"
      >
        <el-table-column prop="transferNo" label="流水号" width="220">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.transferNo }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="转出账户" width="200">
          <template #default="{ row }">
            <div>
              <div class="account-name">{{ maskName(row.fromAccountName) || '未知账户' }}</div>
              <div class="account-no">{{ row.fromAccountNo || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="转入账户" width="200">
          <template #default="{ row }">
            <div>
              <div class="account-name">{{ maskName(row.toAccountName) || '未知账户' }}</div>
              <div class="account-no">{{ row.toAccountNo || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="交易金额" width="180" align="right">
          <template #default="{ row }">
            <div class="amount-cell">
              <span 
                :class="['amount', { 'large-amount': row.isLargeAmount }]"
              >
                ¥{{ row.amountDisplay }}
              </span>
              <el-tag 
                v-if="row.isLargeAmount" 
                type="warning" 
                size="small"
                class="large-tag"
              >
                大额
              </el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="transferTimeDisplay" label="转账时间" width="240" />

        <el-table-column label="账户状态" width="150" align="center">
          <template #default="{ row }">
            <div v-if="row.isSuspiciousAccount" class="suspicious-indicator">
              <el-tag type="danger" size="small" class="suspicious-tag">
                <el-icon style="margin-right: 4px"><Warning /></el-icon>
                可疑
              </el-tag>
              <div class="suspicious-reason" v-if="row.suspiciousReason">
                {{ row.suspiciousReason }}
              </div>
            </div>
            <div v-else class="normal-indicator">
              <el-tag type="success" size="small">正常</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button
                size="small"
                type="success"
                :icon="Search"
                @click.stop="viewRelatedTransfers(row)"
                class="operation-btn"
                plain
              >
                关联
              </el-button>
              <el-button
                size="small"
                type="primary"
                :icon="View"
                @click.stop="viewTransferDetail(row)"
                class="operation-btn"
                plain
              >
                详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-if="totalElements > 0"
        class="pagination"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="totalElements"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 转账详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="转账记录详情"
      width="800px"
    >
      <div v-if="selectedTransfer" class="transfer-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="流水号">
            {{ selectedTransfer.transferNo }}
          </el-descriptions-item>
          <el-descriptions-item label="转账金额">
            <span :class="{ 'large-amount': selectedTransfer.isLargeAmount }">
              ¥{{ selectedTransfer.amountDisplay }}
            </span>
            <el-tag v-if="selectedTransfer.isLargeAmount" type="warning" size="small" style="margin-left: 8px">
              大额交易
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="转出账户">
            {{ maskName(selectedTransfer.fromAccountName) || '未知账户' }}
          </el-descriptions-item>
          <el-descriptions-item label="转出账户号">
            {{ selectedTransfer.fromAccountNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="转入账户">
            {{ maskName(selectedTransfer.toAccountName) || '未知账户' }}
          </el-descriptions-item>
          <el-descriptions-item label="转入账户号">
            {{ selectedTransfer.toAccountNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="转账时间" :span="2">
            {{ selectedTransfer.transferTimeDisplay }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="viewAccountDetails">查看账户详情</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Money, Refresh, Download, Search, RefreshLeft, View, Warning,
  DataBoard, Calendar, ArrowLeft, Link
} from '@element-plus/icons-vue'
import { transferApi } from '../api/transfer'
import { maskName } from '../utils/nameMasking'

// 响应式数据
const loading = ref(false)
const transferRecords = ref([])
const statistics = ref({})
const quickFilter = ref('all')
const lastFilterType = ref('all')
const lastSearchParams = ref(null)
const returnFilterSnapshot = ref({ type: 'all', params: null })

// 分页数据
const currentPage = ref(1)
const pageSize = ref(20)
const totalElements = ref(0)

// 搜索表单
const searchForm = ref({
  transferNo: '',
  fromAccountName: '',
  toAccountName: '',
  minAmount: null,
  maxAmount: null,
  timeRange: null
})

// 对话框
const detailDialogVisible = ref(false)
const selectedTransfer = ref(null)
const isViewingRelated = ref(false)
const relatedDescription = ref('')

// 生命周期
onMounted(() => {
  loadTransferRecords()
  loadStatistics()
})

// 方法
const resetRelatedView = () => {
  isViewingRelated.value = false
  relatedDescription.value = ''
}

const loadTransferRecords = async (options = {}) => {
  const { skipRelatedReset = false } = options
  loading.value = true
  try {
    if (!skipRelatedReset) {
      resetRelatedView()
    }
    const response = await transferApi.getTransferRecords(currentPage.value - 1, pageSize.value)
    if (response.data.success) {
      transferRecords.value = response.data.data
      totalElements.value = response.data.totalElements
      lastFilterType.value = 'all'
      lastSearchParams.value = null
      returnFilterSnapshot.value = { type: 'all', params: null }
      quickFilter.value = 'all'
    } else {
      ElMessage.error(response.data.message || '获取转账记录失败')
    }
  } catch (error) {
    console.error('获取转账记录失败:', error)
    ElMessage.error('获取转账记录失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    console.log('开始获取统计信息...')
    const response = await transferApi.getTransferStatistics()
    console.log('统计信息API响应:', response.data)
    
    if (response.data.success) {
      statistics.value = response.data.data
      console.log('统计信息设置成功:', statistics.value)
    } else {
      console.error('统计信息API返回失败:', response.data.message)
      ElMessage.error('获取统计信息失败: ' + response.data.message)
    }
  } catch (error) {
    console.error('获取统计信息失败:', error)
    ElMessage.error('获取统计信息失败: ' + error.message)
    
    // 设置默认统计数据
    statistics.value = {
      total_count: 0,
      total_amount_display: '0.00',
      large_amount_count: 0,
      today_count: 0
    }
  }
}

const buildSearchParams = () => ({
  transferNo: searchForm.value.transferNo,
  fromAccountName: searchForm.value.fromAccountName,
  toAccountName: searchForm.value.toAccountName,
  minAmount: searchForm.value.minAmount,
  maxAmount: searchForm.value.maxAmount,
  startTime: searchForm.value.timeRange?.[0],
  endTime: searchForm.value.timeRange?.[1],
  page: 0,
  size: pageSize.value
})

const handleSearch = async (customParams = null, options = {}) => {
  const { skipMessage = false } = options
  loading.value = true
  try {
    if (!customParams) resetRelatedView()
    const searchParams = customParams || buildSearchParams()
    const response = await transferApi.searchTransferRecords(searchParams)
    
    if (response.data.success) {
      transferRecords.value = response.data.data
      totalElements.value = response.data.totalElements
      currentPage.value = 1
      lastFilterType.value = 'search'
      lastSearchParams.value = searchParams
      returnFilterSnapshot.value = {
        type: 'search',
        params: searchParams
      }
      
      if (!skipMessage) {
        let searchDescription = ''
        if (searchParams.transferNo) {
          searchDescription = `流水号"${searchParams.transferNo}"`
        } else if (searchParams.fromAccountName && searchParams.toAccountName) {
          searchDescription = `转出账户"${maskName(searchParams.fromAccountName)}"和转入账户"${maskName(searchParams.toAccountName)}"`
        } else if (searchParams.fromAccountName) {
          searchDescription = `转出账户"${maskName(searchParams.fromAccountName)}"`
        } else if (searchParams.toAccountName) {
          searchDescription = `转入账户"${maskName(searchParams.toAccountName)}"`
        } else if (searchParams.minAmount || searchParams.maxAmount) {
          const min = searchParams.minAmount || '0'
          const max = searchParams.maxAmount || '无限制'
          searchDescription = `金额范围${min}-${max}`
        } else if (searchParams.startTime && searchParams.endTime) {
          searchDescription = `时间范围${searchParams.startTime}至${searchParams.endTime}`
        } else {
          searchDescription = '全部记录'
        }
        
        if (response.data.totalElements > 0) {
          ElMessage.success(`搜索${searchDescription}成功，找到 ${response.data.totalElements} 条记录`)
        } else {
          ElMessage.info(`未找到符合${searchDescription}条件的记录`)
        }
      }
    } else if (!skipMessage) {
      ElMessage.error(response.data.message || '搜索失败')
    }
  } catch (error) {
    console.error('搜索失败:', error)
    if (!skipMessage) {
      ElMessage.error('搜索失败: ' + error.message)
    }
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.value = {
    transferNo: '',
    fromAccountName: '',
    toAccountName: '',
    minAmount: null,
    maxAmount: null,
    timeRange: null
  }
  quickFilter.value = 'all'
  currentPage.value = 1
  resetRelatedView()
  loadTransferRecords()
}

const applyQuickFilter = async (filter, options = {}) => {
  const { skipMessage = false, skipRelatedReset = false } = options
  quickFilter.value = filter
  loading.value = true
  if (!skipRelatedReset) resetRelatedView()
  
  try {
    let response
    const page = 0
    const size = pageSize.value
    
    switch (filter) {
      case 'large':
        response = await transferApi.getLargeAmountTransfers(page, size)
        break
      case 'today':
        response = await transferApi.getTodayTransfers(page, size)
        break
      case 'week':
        response = await transferApi.getWeekTransfers(page, size)
        break
      default:
        response = await transferApi.getTransferRecords(page, size)
        break
    }
    
    if (response.data.success) {
      transferRecords.value = response.data.data
      totalElements.value = response.data.totalElements
      currentPage.value = 1
      lastFilterType.value = filter
      lastSearchParams.value = null
      returnFilterSnapshot.value = { type: filter, params: null }
      
      if (!skipMessage) {
        const filterNames = {
          'all': '全部',
          'large': '大额交易',
          'today': '今日交易',
          'week': '本周交易'
        }
        ElMessage.success(`${filterNames[filter]}筛选成功，找到 ${response.data.totalElements} 条记录`)
      }
    } else {
      ElMessage.error(response.data.message || '筛选失败')
    }
  } catch (error) {
    console.error('快速筛选失败:', error)
    ElMessage.error('筛选失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadTransferRecords()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadTransferRecords()
}

const viewTransferDetail = (row) => {
  selectedTransfer.value = row
  detailDialogVisible.value = true
}

const viewRelatedTransfers = async (row) => {
  try {
    loading.value = true
    returnFilterSnapshot.value = {
      type: lastFilterType.value,
      params: lastFilterType.value === 'search' && lastSearchParams.value ? { ...lastSearchParams.value } : null
    }
    const searchParams = {
      fromAccountName: row.fromAccountName || '',
      toAccountName: row.toAccountName || '',
      page: 0,
      size: pageSize.value
    }
    
    const response = await transferApi.searchTransferRecords(searchParams)
    if (response.data.success) {
      transferRecords.value = response.data.data
      totalElements.value = response.data.totalElements
      currentPage.value = 1
      const parts = []
      if (row.fromAccountName) parts.push(`转出账户 ${maskName(row.fromAccountName)}`)
      if (row.toAccountName) parts.push(`转入账户 ${maskName(row.toAccountName)}`)
      relatedDescription.value = parts.join(' ｜ ') || `流水号 ${row.transferNo}`
      isViewingRelated.value = true
      lastFilterType.value = 'related'
      ElMessage.success(`找到 ${response.data.totalElements} 条相关转账记录`)
    } else {
      ElMessage.error(response.data.message || '获取相关记录失败')
    }
  } catch (error) {
    console.error('获取相关转账记录失败:', error)
    ElMessage.error('获取相关转账记录失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const resumeLastFilter = async () => {
  const snapshot = returnFilterSnapshot.value || { type: 'all', params: null }
  switch (snapshot.type) {
    case 'large':
      await applyQuickFilter('large', { skipMessage: true, skipRelatedReset: true })
      break
    case 'today':
      await applyQuickFilter('today', { skipMessage: true, skipRelatedReset: true })
      break
    case 'week':
      await applyQuickFilter('week', { skipMessage: true, skipRelatedReset: true })
      break
    case 'search':
      if (snapshot.params) {
        await handleSearch(snapshot.params, { skipMessage: true })
      } else {
        await loadTransferRecords({ skipRelatedReset: true })
      }
      break
    case 'all':
      quickFilter.value = 'all'
      await loadTransferRecords({ skipRelatedReset: true })
      break
    default:
      await loadTransferRecords({ skipRelatedReset: true })
  }
  lastFilterType.value = snapshot.type || 'all'
}

const backToAllTransfers = async () => {
  resetRelatedView()
  await resumeLastFilter()
  ElMessage.success('已返回筛选前的列表')
}

const viewAccountDetails = async () => {
  if (!selectedTransfer.value) return
  
  try {
    const transfer = selectedTransfer.value
    
    ElMessageBox.alert(
      `<div style="text-align: left;">
        <h3>账户信息</h3>
        <p><strong>转出账户:</strong> ${maskName(transfer.fromAccountName) || '未知'}</p>
        <p><strong>转出账户号:</strong> ${transfer.fromAccountNo || '-'}</p>
        <p><strong>转入账户:</strong> ${maskName(transfer.toAccountName) || '未知'}</p>
        <p><strong>转入账户号:</strong> ${transfer.toAccountNo || '-'}</p>
        <p><strong>交易金额:</strong> ¥${transfer.amountDisplay}</p>
        <p><strong>交易时间:</strong> ${transfer.transferTimeDisplay}</p>
        ${transfer.isLargeAmount ? '<p style="color: #E6A23C;"><strong>⚠️ 大额交易</strong></p>' : ''}
      </div>`,
      '转账详细信息',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      }
    )
  } catch (error) {
    console.error('获取账户详情失败:', error)
    ElMessage.error('获取账户详情失败: ' + error.message)
  }
}

const exportRecords = () => {
  if (transferRecords.value.length === 0) {
    ElMessage.warning('没有可导出的记录')
    return
  }

  try {
    // 创建CSV内容
    const headers = ['流水号', '转出账户', '转出账户号', '转入账户', '转入账户号', '金额', '转账时间', '是否大额']
    const csvContent = [
      headers.join(','),
      ...transferRecords.value.map(record => [
        record.transferNo || '',
        maskName(record.fromAccountName) || '',
        record.fromAccountNo || '',
        maskName(record.toAccountName) || '',
        record.toAccountNo || '',
        record.amountDisplay || '',
        record.transferTimeDisplay || '',
        record.isLargeAmount ? '是' : '否'
      ].join(','))
    ].join('\n')

    // 创建并下载文件
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `转账记录_${new Date().toISOString().slice(0, 10)}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败: ' + error.message)
  }
}
</script>

<style scoped>
.transfer-management {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
  width: 100%;
}

.page-header {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.page-subtitle {
  margin: 5px 0 0 0;
  color: #909399;
  font-size: 14px;
  font-weight: normal;
}

.stats-cards {
  margin-bottom: 20px;
}

.stats-card {
  position: relative;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
  cursor: pointer;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
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
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

.filter-card .el-card__body {
  padding: 24px;
  background: white;
}

.search-tips {
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: 16px;
}

.quick-filters {
  border-top: 1px solid #EBEEF5;
  padding-top: 16px;
}

.related-strip-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-radius: 12px;
  border: 1px solid #c6e2ff;
  background: linear-gradient(120deg, #f0f7ff 0%, #ffffff 100%);
  box-shadow: 0 6px 12px rgba(64, 158, 255, 0.08);
  margin-bottom: 16px;
}

.related-strip-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.strip-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.strip-desc {
  font-size: 13px;
  color: #606266;
  margin-top: 2px;
}

.table-card {
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

.table-card .el-card__body {
  padding: 0;
  background: white;
}

.related-icon {
  color: #409eff;
  font-size: 20px;
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
:deep(.transfer-table .el-table__row) {
  height: auto;
  border-bottom: 1px solid #f0f0f0;
}

/* 增加行与行之间的间距，让表格更美观 */
:deep(.transfer-table .el-table__body tr) {
  height: auto;
  min-height: 64px;
}

/* 表头行间距 */
:deep(.transfer-table .el-table th) {
  padding: 16px 10px !important;
  height: 52px;
  line-height: 20px;
}

/* 数据行间距 - 更舒适的行高 */
:deep(.transfer-table .el-table td) {
  padding: 20px 10px !important;
  height: auto;
  min-height: 64px;
  line-height: 24px;
}

/* 首列：流水号 */
:deep(.transfer-table .el-table th:first-child) {
  padding-left: 20px !important;
  padding-right: 12px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.transfer-table .el-table td:first-child) {
  padding-left: 20px !important;
  padding-right: 12px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 转出账户列 */
:deep(.transfer-table .el-table th:nth-child(2)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.transfer-table .el-table td:nth-child(2)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 转入账户列 */
:deep(.transfer-table .el-table th:nth-child(3)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.transfer-table .el-table td:nth-child(3)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 交易金额列 */
:deep(.transfer-table .el-table th:nth-child(4)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.transfer-table .el-table td:nth-child(4)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 转账时间列 */
:deep(.transfer-table .el-table th:nth-child(5)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.transfer-table .el-table td:nth-child(5)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 账户状态列 */
:deep(.transfer-table .el-table th:nth-child(6)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.transfer-table .el-table td:nth-child(6)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 操作列：末列 */
:deep(.transfer-table .el-table th:last-child) {
  padding-left: 20px !important;
  padding-right: 20px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.transfer-table .el-table td:last-child) {
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
:deep(.transfer-table) {
  width: 100% !important;
  table-layout: fixed;
}

:deep(.transfer-table .el-table__body-wrapper),
:deep(.transfer-table .el-table__header-wrapper) {
  width: 100% !important;
}

/* 确保表格主体占满容器 */
:deep(.transfer-table .el-table__body),
:deep(.transfer-table .el-table__header) {
  width: 100% !important;
  table-layout: fixed;
}

/* 让表格自动拉伸占满容器 */
:deep(.transfer-table .el-table) {
  width: 100% !important;
}

.account-name {
  font-weight: 500;
  color: #303133;
}

.account-no {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.amount-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.amount {
  font-weight: 600;
  color: #303133;
}

.amount.large-amount {
  color: #E6A23C;
  font-weight: 700;
}

.large-tag {
  margin-top: 4px;
}

/* 可疑账户状态样式 */
.suspicious-indicator {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.suspicious-tag {
  font-size: 12px;
  padding: 2px 8px;
}

.suspicious-reason {
  font-size: 11px;
  color: #909399;
  text-align: center;
  max-width: 100px;
  word-break: break-all;
}

.normal-indicator {
  display: flex;
  justify-content: center;
}

/* 操作按钮样式 - 横向两格分布 */
.operation-buttons {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
}

.operation-btn {
  padding: 5px 10px;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.2s ease;
  white-space: nowrap;
  height: 26px;
  line-height: 1;
  flex-shrink: 0;
}

.operation-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12);
}

.operation-btn .el-icon {
  font-size: 13px;
  margin-right: 3px;
}

/* 确保按钮组整齐排列 */
.operation-buttons .el-button {
  margin: 0;
  padding: 5px 10px;
}

.pagination {
  padding: 20px;
  text-align: center;
}

.transfer-detail {
  margin: 20px 0;
}

.transfer-detail .large-amount {
  color: #E6A23C;
  font-weight: 600;
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

  .search-form .el-form-item {
    margin-bottom: 16px;
  }

  .operation-buttons {
    flex-direction: row;
    gap: 4px;
  }

  .operation-btn {
    font-size: 11px;
    padding: 4px 8px;
  }

  .el-table-column--fixed-right {
    right: 0;
  }

  .suspicious-reason {
    display: none;
  }
}
</style>
