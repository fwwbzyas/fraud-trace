<template>
  <div class="login-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-row justify="space-between" align="middle">
        <el-col :span="12">
          <h2 class="page-title">
            <el-icon style="margin-right: 8px"><Key /></el-icon>
            登录记录管理
          </h2>
          <p class="page-subtitle">查看和分析所有手机银行登录记录</p>
        </el-col>
        <el-col :span="12" style="text-align: right">
          <el-button :icon="Refresh" @click="loadLoginRecords">
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
      <el-col :span="8">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.total_count || 0 }}</div>
            <div class="stats-label">总登录次数</div>
          </div>
          <el-icon class="stats-icon" style="color: #409EFF"><DataBoard /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.today_count || 0 }}</div>
            <div class="stats-label">今日登录</div>
          </div>
          <el-icon class="stats-icon" style="color: #67C23A"><Calendar /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stats-card">
          <div class="stats-content">
            <div class="stats-number">{{ statistics.unique_ip_count || 0 }}</div>
            <div class="stats-label">登录地区数</div>
          </div>
          <el-icon class="stats-icon" style="color: #E6A23C"><Location /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和筛选 -->
    <el-card class="filter-card">
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="账户名">
          <el-input
            v-model="searchForm.accountName"
            placeholder="输入账户持有人姓名"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        
        <el-form-item label="登录地区">
          <el-input
            v-model="searchForm.loginIp"
            placeholder="输入登录地区(如:北京市朝阳区)"
            clearable
            style="width: 200px"
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
            style="width: 350px"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button :icon="RefreshLeft" @click="resetSearch">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 快速筛选 -->
    <el-card class="quick-filter-card">
      <div class="quick-filter-buttons">
        <el-button 
          :type="quickFilter === 'all' ? 'primary' : ''" 
          @click="applyQuickFilter('all')"
        >
          全部记录
        </el-button>
        <el-button 
          :type="quickFilter === 'today' ? 'warning' : ''" 
          @click="applyQuickFilter('today')"
        >
          今日登录
        </el-button>
        <el-button 
          :type="quickFilter === 'week' ? 'info' : ''" 
          @click="applyQuickFilter('week')"
        >
          本周登录
        </el-button>
      </div>
    </el-card>

    <!-- 登录记录表格 -->
    <el-card class="table-card">
      <el-table 
        :data="loginRecords" 
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        class="login-table"
      >
        
        <el-table-column prop="recordId" label="记录ID" width="120" />
        
        <el-table-column prop="accountName" label="账户名" width="180">
          <template #default="{ row }">
            {{ maskName(row.accountName) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="accountNo" label="账户号" width="200" />
        
        <el-table-column prop="phone" label="手机号" width="160">
          <template #default="{ row }">
            {{ maskPhone(row.phone) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="loginIp" label="登录地区" width="220" />
        
        <el-table-column prop="loginTimeDisplay" label="登录时间" width="240" />
        
        
        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="scope">
            <div class="operation-buttons">
              <el-button 
                type="primary" 
                size="small" 
                :icon="View"
                @click.stop="viewLoginDetail(scope.row)"
                class="operation-btn"
                plain
              >
                详情
              </el-button>
              <el-button 
                type="info" 
                size="small" 
                :icon="Connection"
                @click.stop="viewAccountLogins(scope.row)"
                class="operation-btn"
                plain
              >
                账户登录
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalElements"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 登录详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="登录记录详情"
      width="600px"
    >
      <div v-if="selectedLogin" class="login-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="记录ID">{{ selectedLogin.recordId }}</el-descriptions-item>
          <el-descriptions-item label="账户名">{{ maskName(selectedLogin.accountName) }}</el-descriptions-item>
          <el-descriptions-item label="账户号">{{ selectedLogin.accountNo }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ maskPhone(selectedLogin.phone) }}</el-descriptions-item>
          <el-descriptions-item label="登录地区">{{ selectedLogin.loginIp }}</el-descriptions-item>
          <el-descriptions-item label="登录时间">{{ selectedLogin.loginTimeDisplay }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="viewAccountDetails">
          查看账户详情
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Key, Refresh, Download, DataBoard, Calendar, Location,
  Search, RefreshLeft, View, Connection 
} from '@element-plus/icons-vue'
import { loginApi } from '@/api/login'
import { maskName } from '@/utils/nameMasking'

// 加密手机号：将第4位到第7位（索引3-6）替换为4个*
const maskPhone = (phone) => {
  if (!phone) return phone
  const phoneStr = String(phone)
  if (phoneStr.length < 7) return phoneStr
  // 前3位 + 4个* + 后4位（如果长度>=11）或剩余部分
  if (phoneStr.length >= 11) {
    return phoneStr.substring(0, 3) + '****' + phoneStr.substring(7)
  } else {
    // 如果手机号长度不足11位，仍然加密中间4位
    const prefix = phoneStr.substring(0, 3)
    const suffix = phoneStr.substring(7)
    return prefix + '****' + suffix
  }
}

// 响应式数据
const loading = ref(false)
const loginRecords = ref([])
const statistics = ref({})
const quickFilter = ref('all')

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const totalElements = ref(0)

// 搜索表单
const searchForm = ref({
  accountName: '',
  loginIp: '',
  timeRange: null
})

// 搜索状态标志
const isSearching = ref(false)

// 对话框
const detailDialogVisible = ref(false)
const selectedLogin = ref(null)

// 生命周期
onMounted(() => {
  loadLoginRecords()
  loadStatistics()
})

// 方法
const loadLoginRecords = async () => {
  loading.value = true
  try {
    const response = await loginApi.getLoginRecords(currentPage.value - 1, pageSize.value)
    if (response.data.success) {
      const page = response.data.data || { content: [], totalElements: 0 }
      loginRecords.value = (page.content || []).map(r => ({
        recordId: r.recordId,
        accountId: r.accountId,
        accountName: r.accountName || `未知账户-${r.accountId}`,
        accountNo: r.accountNo || '',
        phone: r.phone || '',
        loginIp: r.loginIp,
        loginTime: r.loginTime,
        loginTimeDisplay: r.loginTimeDisplay || new Date(r.loginTime).toLocaleString('zh-CN'),
      }))
      totalElements.value = page.totalElements || 0
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('获取登录记录失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const response = await loginApi.getLoginStatistics()
    if (response.data.success) {
      statistics.value = response.data.data
    }
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }
}

const handleSearch = async () => {
  loading.value = true
  try {
    const searchParams = {
      accountName: searchForm.value.accountName,
      loginIp: searchForm.value.loginIp,
      startTime: searchForm.value.timeRange?.[0],
      endTime: searchForm.value.timeRange?.[1],
      page: 0,
      size: pageSize.value
    }
    
    const response = await loginApi.searchLoginRecords(searchParams)
    if (response.data.success) {
      const page = response.data.data || { content: [], totalElements: 0 }
      loginRecords.value = (page.content || []).map(r => ({
        recordId: r.recordId,
        accountId: r.accountId,
        accountName: r.accountName || `未知账户-${r.accountId}`,
        accountNo: r.accountNo || '',
        phone: r.phone || '',
        loginIp: r.loginIp,
        loginTime: r.loginTime,
        loginTimeDisplay: r.loginTimeDisplay || new Date(r.loginTime).toLocaleString('zh-CN'),
        loginResult: r.loginResult,
        isSuccessful: r.isSuccessful || r.loginResult === 1,
        loginResultDisplay: r.loginResultDisplay || (r.loginResult === 1 ? '成功' : '失败')
      }))
      totalElements.value = page.totalElements || 0
      currentPage.value = 1
      isSearching.value = true  // 标记为搜索模式
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('搜索失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.value = {
    accountName: '',
    loginIp: '',
    timeRange: null
  }
  quickFilter.value = 'all'
  currentPage.value = 1
  isSearching.value = false  // 退出搜索模式
  loadLoginRecords()
}

const applyQuickFilter = (filter) => {
  quickFilter.value = filter
  resetSearch()
  
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const weekStart = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000)
  
  switch (filter) {
    case 'today':
      searchForm.value.timeRange = [
        today.toISOString().slice(0, 19),
        now.toISOString().slice(0, 19)
      ]
      break
    case 'week':
      searchForm.value.timeRange = [
        weekStart.toISOString().slice(0, 19),
        now.toISOString().slice(0, 19)
      ]
      break
  }
  
  if (filter !== 'all') {
    handleSearch()
  }
}

const handleSizeChange = async (size) => {
  pageSize.value = size
  currentPage.value = 1
  
  // 如果在搜索模式，继续使用搜索条件
  if (isSearching.value) {
    await handleSearch()
  } else {
    await loadLoginRecords()
  }
}

const handleCurrentChange = async (page) => {
  currentPage.value = page
  
  // 如果在搜索模式，继续使用搜索条件进行分页
  if (isSearching.value) {
    loading.value = true
    try {
      const searchParams = {
        accountName: searchForm.value.accountName,
        loginIp: searchForm.value.loginIp,
        startTime: searchForm.value.timeRange?.[0],
        endTime: searchForm.value.timeRange?.[1],
        page: currentPage.value - 1,  // 使用当前页码
        size: pageSize.value
      }
      
      const response = await loginApi.searchLoginRecords(searchParams)
      if (response.data.success) {
        const pageData = response.data.data || { content: [], totalElements: 0 }
        loginRecords.value = (pageData.content || []).map(r => ({
          recordId: r.recordId,
          accountId: r.accountId,
          accountName: r.accountName || `未知账户-${r.accountId}`,
          accountNo: r.accountNo || '',
          phone: r.phone || '',
          loginIp: r.loginIp,
          loginTime: r.loginTime,
          loginTimeDisplay: r.loginTimeDisplay || new Date(r.loginTime).toLocaleString('zh-CN'),
          loginResult: r.loginResult,
          isSuccessful: r.isSuccessful || r.loginResult === 1,
          loginResultDisplay: r.loginResultDisplay || (r.loginResult === 1 ? '成功' : '失败')
        }))
        totalElements.value = pageData.totalElements || 0
      }
    } catch (error) {
      ElMessage.error('加载数据失败: ' + error.message)
    } finally {
      loading.value = false
    }
  } else {
    await loadLoginRecords()
  }
}

const viewLoginDetail = (row) => {
  selectedLogin.value = row
  detailDialogVisible.value = true
}

const viewAccountLogins = async (row) => {
  try {
    loading.value = true
    // 更新搜索表单
    searchForm.value.accountName = row.accountName
    searchForm.value.loginIp = ''
    searchForm.value.timeRange = null
    
    // 搜索该账户的所有登录记录
    const searchParams = {
      accountName: row.accountName,
      page: 0,
      size: pageSize.value
    }
    
    const response = await loginApi.searchLoginRecords(searchParams)
    if (response.data.success) {
      const page = response.data.data || { content: [], totalElements: 0 }
      // 正确映射返回的数据结构
      loginRecords.value = (page.content || []).map(r => ({
        recordId: r.recordId,
        accountId: r.accountId,
        accountName: r.accountName || `未知账户-${r.accountId}`,
        accountNo: r.accountNo || '',
        phone: r.phone || '',
        loginIp: r.loginIp,
        loginTime: r.loginTime,
        loginTimeDisplay: r.loginTimeDisplay || new Date(r.loginTime).toLocaleString('zh-CN'),
        loginResult: r.loginResult,
        isSuccessful: r.isSuccessful || r.loginResult === 1,
        loginResultDisplay: r.loginResultDisplay || (r.loginResult === 1 ? '成功' : '失败')
      }))
      totalElements.value = page.totalElements || 0
      currentPage.value = 1
      isSearching.value = true  // 进入搜索模式
      ElMessage.success(`找到 ${page.totalElements || 0} 条该账户的登录记录`)
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('获取账户登录记录失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const viewAccountDetails = async () => {
  if (!selectedLogin.value) return
  
  try {
    const accountId = selectedLogin.value.accountId
    ElMessageBox.alert(
      `<div style="text-align: left;">
        <h3>账户详细信息</h3>
        <p><strong>账户ID:</strong> ${selectedLogin.value.accountId}</p>
        <p><strong>账户名:</strong> ${maskName(selectedLogin.value.accountName)}</p>
        <p><strong>账户号:</strong> ${selectedLogin.value.accountNo}</p>
        <p><strong>手机号:</strong> ${maskPhone(selectedLogin.value.phone)}</p>
        <p><strong>本次登录地区:</strong> ${selectedLogin.value.loginIp}</p>
        <p><strong>本次登录时间:</strong> ${selectedLogin.value.loginTimeDisplay}</p>
      </div>`,
      '账户详情',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      }
    )
  } catch (error) {
    ElMessage.error('获取账户详情失败: ' + error.message)
  }
}

const exportRecords = () => {
  if (loginRecords.value.length === 0) {
    ElMessage.warning('没有可导出的记录')
    return
  }

  try {
    // 创建CSV内容
    const headers = ['记录ID', '账户名', '账户号', '手机号', '登录IP', '登录时间', '登录结果']
    const csvContent = [
      headers.join(','),
      ...loginRecords.value.map(record => [
        record.recordId || '',
        maskName(record.accountName) || '',
        record.accountNo || '',
        maskPhone(record.phone) || '',
        record.loginIp || '',
        record.loginTimeDisplay || '',
        record.loginResultDisplay || ''
      ].join(','))
    ].join('\n')

    // 创建并下载文件
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `登录记录_${new Date().toISOString().slice(0, 10)}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败: ' + error.message)
  }
}
</script>

<style scoped>
.login-management {
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
  display: flex;
  align-items: center;
}

.page-subtitle {
  margin: 5px 0 0 0;
  color: #909399;
  font-size: 14px;
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

.stats-content {
  position: relative;
  z-index: 2;
}

.stats-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stats-label {
  font-size: 14px;
  color: #909399;
}

.stats-icon {
  position: absolute;
  top: 20px;
  right: 20px;
  font-size: 40px;
  opacity: 0.8;
}

.filter-card, .quick-filter-card, .table-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

.search-form {
  margin-bottom: 0;
}

.quick-filter-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  margin-top: 20px;
  text-align: right;
}

.login-detail {
  margin: 20px 0;
}

/* 优化表格行间距，让整体更舒适美观 */
:deep(.login-table .el-table__row) {
  height: auto;
  border-bottom: 1px solid #f0f0f0;
}

/* 增加行与行之间的间距，让表格更美观 */
:deep(.login-table .el-table__body tr) {
  height: auto;
  min-height: 64px;
}

/* 表头行间距 */
:deep(.login-table .el-table th) {
  padding: 16px 10px !important;
  height: 52px;
  line-height: 20px;
}

/* 数据行间距 - 更舒适的行高 */
:deep(.login-table .el-table td) {
  padding: 20px 10px !important;
  height: auto;
  min-height: 64px;
  line-height: 24px;
}

/* 首列：记录ID */
:deep(.login-table .el-table th:first-child) {
  padding-left: 20px !important;
  padding-right: 12px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.login-table .el-table td:first-child) {
  padding-left: 20px !important;
  padding-right: 12px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 账户名列 */
:deep(.login-table .el-table th:nth-child(2)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.login-table .el-table td:nth-child(2)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 账户号列 */
:deep(.login-table .el-table th:nth-child(3)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.login-table .el-table td:nth-child(3)) {
  padding-left: 12px !important;
  padding-right: 12px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 手机号列 */
:deep(.login-table .el-table th:nth-child(4)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.login-table .el-table td:nth-child(4)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 登录地区列 */
:deep(.login-table .el-table th:nth-child(5)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.login-table .el-table td:nth-child(5)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 登录时间列 */
:deep(.login-table .el-table th:nth-child(6)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.login-table .el-table td:nth-child(6)) {
  padding-left: 16px !important;
  padding-right: 16px !important;
  padding-top: 20px !important;
  padding-bottom: 20px !important;
}

/* 操作列：末列 */
:deep(.login-table .el-table th:last-child) {
  padding-left: 20px !important;
  padding-right: 20px !important;
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}
:deep(.login-table .el-table td:last-child) {
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
:deep(.login-table) {
  width: 100% !important;
  table-layout: fixed;
}

:deep(.login-table .el-table__body-wrapper),
:deep(.login-table .el-table__header-wrapper) {
  width: 100% !important;
}

/* 确保表格主体占满容器 */
:deep(.login-table .el-table__body),
:deep(.login-table .el-table__header) {
  width: 100% !important;
  table-layout: fixed;
}

/* 让表格自动拉伸占满容器 */
:deep(.login-table .el-table) {
  width: 100% !important;
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
</style>