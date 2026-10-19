// 模拟API服务
import { generateAllMockData } from '@/data/mockDataGenerator'
import { testModeConfig } from '@/config/testMode'

// 生成并缓存数据
let mockData = null
let mockDataPromise = null
const getMockData = async () => {
  if (!mockData) {
    if (!mockDataPromise) {
      mockDataPromise = generateAllMockData()
    }
    mockData = await mockDataPromise
    // 在控制台显示数据来源
    if (mockData.dataSource) {
      console.log(`\n🎯 当前使用的数据源: ${mockData.dataSource === 'CSV' ? '✅ CSV文件' : '🎲 随机生成'}\n`)
    }
  }
  return mockData
}

// 模拟网络延迟
const delay = (ms = testModeConfig.apiDelay) => {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// 模拟分页
const paginate = (data, page = 0, size = 20) => {
  const start = page * size
  const end = start + size
  const content = data.slice(start, end)
  
  return {
    content,
    totalElements: data.length,
    totalPages: Math.ceil(data.length / size),
    size,
    number: page,
    first: page === 0,
    last: page >= Math.ceil(data.length / size) - 1
  }
}

// 模拟搜索过滤
const searchFilter = (data, searchParams) => {
  return data.filter(item => {
    for (const [key, value] of Object.entries(searchParams)) {
      if (value === null || value === undefined || value === '') continue
      
      const itemValue = item[key]
      if (itemValue === null || itemValue === undefined) continue
      
      // 字符串模糊匹配
      if (typeof value === 'string' && typeof itemValue === 'string') {
        if (!itemValue.toLowerCase().includes(value.toLowerCase())) {
          return false
        }
      }
      // 数值精确匹配
      else if (typeof value === 'number') {
        if (itemValue !== value) {
          return false
        }
      }
      // 时间范围匹配
      else if (key === 'startTime' && value) {
        if (new Date(item.loginTime || item.transferTime || item.createTime) < new Date(value)) {
          return false
        }
      }
      else if (key === 'endTime' && value) {
        if (new Date(item.loginTime || item.transferTime || item.createTime) > new Date(value)) {
          return false
        }
      }
      // 金额范围匹配
      else if (key === 'minAmount' && value && item.amount < value) {
        return false
      }
      else if (key === 'maxAmount' && value && item.amount > value) {
        return false
      }
    }
    return true
  })
}

// 模拟API响应格式
const createResponse = (data, success = true, message = '') => {
  return {
    data: {
      success,
      data,
      message: message || (success ? '操作成功' : '操作失败'),
      timestamp: new Date().toISOString()
    }
  }
}

// 登录记录模拟API
export const mockLoginApi = {
  async getLoginRecords(page = 0, size = 20) {
    await delay()
    const data = await getMockData()
    const pageData = paginate(data.loginRecords, page, size)
    
    // 返回符合前端期望的格式
    return {
      data: {
        success: true,
        data: {
          content: pageData.content,
          totalElements: pageData.totalElements,
          totalPages: pageData.totalPages,
          currentPage: pageData.number,
          pageSize: pageData.size
        },
        message: '获取成功'
      }
    }
  },

  async searchLoginRecords(searchParams) {
    await delay()
    const data = await getMockData()
    const { page = 0, size = 20, ...filters } = searchParams
    
    const filtered = searchFilter(data.loginRecords, filters)
    const pageData = paginate(filtered, page, size)
    
    // 返回符合前端期望的格式
    return {
      data: {
        success: true,
        data: {
          content: pageData.content,
          totalElements: pageData.totalElements,
          totalPages: pageData.totalPages,
          currentPage: pageData.number,
          pageSize: pageData.size
        },
        message: '搜索成功'
      }
    }
  },

  async getLoginRecordById(id) {
    await delay()
    const data = await getMockData()
    const record = data.loginRecords.find(r => r.recordId === parseInt(id))
    return createResponse(record)
  },

  async getAccountLoginRecords(accountId) {
    await delay()
    const data = await getMockData()
    const records = data.loginRecords.filter(r => r.accountId === parseInt(accountId))
    return createResponse(records)
  },

  async getSuccessfulLogins() {
    await delay()
    const data = await getMockData()
    const records = data.loginRecords.filter(r => r.loginResult === 1)
    return createResponse(records)
  },

  async getFailedLogins() {
    await delay()
    const data = await getMockData()
    const records = data.loginRecords.filter(r => r.loginResult === 2)
    return createResponse(records)
  },

  async getLoginStatistics() {
    await delay()
    const data = await getMockData()
    return createResponse(data.statistics.login)
  },

  async getAccountLoginStatistics(accountId) {
    await delay()
    const data = await getMockData()
    const records = data.loginRecords.filter(r => r.accountId === parseInt(accountId))
    
    const stats = {
      loginCount: records.length,
      successCount: records.filter(r => r.loginResult === 1).length,
      failedCount: records.filter(r => r.loginResult === 2).length,
      lastLoginTime: records.length > 0 ? records[0].loginTimeDisplay : '无',
      uniqueIpCount: new Set(records.map(r => r.loginIp)).size
    }
    
    return createResponse(stats)
  },

  async testService() {
    await delay()
    const data = await getMockData()
    return createResponse({
      message: '登录记录服务正常',
      totalRecords: data.loginRecords.length,
      timestamp: new Date().toISOString()
    })
  }
}

// 转账记录模拟API
export const mockTransferApi = {
  async getAllTransfers(page = 0, size = 20) {
    await delay()
    const data = await getMockData()
    const pageData = paginate(data.transferRecords, page, size)
    
    // 返回符合前端期望的格式
    return {
      data: {
        success: true,
        data: pageData.content,
        totalElements: pageData.totalElements,
        totalPages: pageData.totalPages,
        size: pageData.size,
        number: pageData.number,
        first: pageData.first,
        last: pageData.last,
        message: '获取成功'
      }
    }
  },

  async searchTransfers(searchParams) {
    await delay()
    const data = await getMockData()
    const { page = 0, size = 20, ...filters } = searchParams
    
    const filtered = searchFilter(data.transferRecords, filters)
    const pageData = paginate(filtered, page, size)
    
    // 返回符合前端期望的格式
    return {
      data: {
        success: true,
        data: pageData.content,
        totalElements: pageData.totalElements,
        totalPages: pageData.totalPages,
        size: pageData.size,
        number: pageData.number,
        first: pageData.first,
        last: pageData.last,
        message: '搜索成功'
      }
    }
  },

  async getTransferById(id) {
    await delay()
    const data = await getMockData()
    const record = data.transferRecords.find(r => r.transferId === parseInt(id))
    return createResponse(record)
  },

  async getTransferStatistics() {
    await delay()
    const data = await getMockData()
    return createResponse(data.statistics.transfer)
  },

  async getAccountTransfers(accountId) {
    await delay()
    const data = await getMockData()
    const records = data.transferRecords.filter(r => 
      r.fromAccountId === parseInt(accountId) || r.toAccountId === parseInt(accountId)
    )
    return createResponse(records)
  }
}

// 指标管理模拟API
export const mockIndicatorApi = {
  async getAllIndicators() {
    await delay()
    const data = await getMockData()
    return createResponse(data.indicators)
  },

  async getActiveIndicators() {
    await delay()
    const data = await getMockData()
    const active = data.indicators.filter(i => i.isActive === 1)
    return createResponse(active)
  },

  async getIndicatorById(id) {
    await delay()
    const data = await getMockData()
    const indicator = data.indicators.find(i => i.indicatorId === parseInt(id))
    return createResponse(indicator)
  },

  async createIndicator(indicatorData) {
    await delay()
    const data = await getMockData()
    const newIndicator = {
      ...indicatorData,
      indicatorId: Math.max(...data.indicators.map(i => i.indicatorId)) + 1,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    }
    data.indicators.push(newIndicator)
    return createResponse(newIndicator)
  },

  async updateIndicator(code, indicatorData) {
    await delay()
    const data = await getMockData()
    const index = data.indicators.findIndex(i => i.indicatorCode === code)
    if (index !== -1) {
      data.indicators[index] = {
        ...data.indicators[index],
        ...indicatorData,
        updateTime: new Date().toISOString()
      }
      return createResponse(data.indicators[index])
    }
    return createResponse(null, false, '指标不存在')
  },

  async deleteIndicator(code) {
    await delay()
    const data = await getMockData()
    const index = data.indicators.findIndex(i => i.indicatorCode === code)
    if (index !== -1) {
      data.indicators.splice(index, 1)
      return createResponse({ deleted: true })
    }
    return createResponse(null, false, '指标不存在')
  }
}

// 任务管理模拟API
export const mockTaskApi = {
  async getAllTasks() {
    await delay()
    const data = await getMockData()
    return createResponse(data.tasks)
  },

  async getTaskById(id) {
    await delay()
    const data = await getMockData()
    const task = data.tasks.find(t => t.taskId === parseInt(id))
    return createResponse(task)
  },

  async createTask(taskData) {
    await delay()
    const data = await getMockData()
    const newTask = {
      ...taskData,
      taskId: Math.max(...data.tasks.map(t => t.taskId)) + 1,
      taskCode: `TASK_${new Date().getFullYear()}${String(data.tasks.length + 1).padStart(3, '0')}`,
      taskStatus: 0,
      progress: 0,
      analyzedAccounts: 0,
      resultCount: 0,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString(),
      startTime: new Date().toISOString(),
      endTime: null
    }
    data.tasks.unshift(newTask)
    return createResponse(newTask)
  },

  async startTask(id) {
    await delay()
    const data = await getMockData()
    const task = data.tasks.find(t => t.taskId === parseInt(id))
    if (task) {
      task.taskStatus = 1
      task.progress = 0
      task.startTime = new Date().toISOString()
      task.updateTime = new Date().toISOString()
      
      // 模拟任务进度更新
      setTimeout(() => {
        task.progress = 100
        task.taskStatus = 2
        task.endTime = new Date().toISOString()
        task.analyzedAccounts = Math.floor(Math.random() * 1000) + 100
        task.resultCount = Math.floor(Math.random() * 50) + 5
      }, 2000)
      
      return createResponse(task)
    }
    return createResponse(null, false, '任务不存在')
  },

  async stopTask(id) {
    await delay()
    const data = await getMockData()
    const task = data.tasks.find(t => t.taskId === parseInt(id))
    if (task) {
      task.taskStatus = 0
      task.updateTime = new Date().toISOString()
      return createResponse(task)
    }
    return createResponse(null, false, '任务不存在')
  },

  async deleteTask(id) {
    await delay()
    const data = await getMockData()
    const index = data.tasks.findIndex(t => t.taskId === parseInt(id))
    if (index !== -1) {
      data.tasks.splice(index, 1)
      return createResponse({ deleted: true })
    }
    return createResponse(null, false, '任务不存在')
  },

  async getTaskStatistics() {
    await delay()
    const data = await getMockData()
    return createResponse(data.statistics.task)
  },

  async getTaskDetailedResults(id) {
    await delay()
    const data = await getMockData()
    const task = data.tasks.find(t => t.taskId === parseInt(id))
    if (!task || task.taskStatus !== 2) {
      return createResponse(null, false, '任务未完成或不存在')
    }
    
    const riskAccounts = data.riskResults.filter(r => r.taskId === parseInt(id))
    const statistics = {
      totalRiskAccounts: riskAccounts.length,
      victimAccounts: riskAccounts.filter(r => r.accountRole === 1).length,
      suspiciousAccounts: riskAccounts.filter(r => r.accountRole === 2).length
    }
    
    const result = {
      ...task,
      riskAccounts,
      statistics
    }
    
    return createResponse(result)
  }
}

// 重置数据
export const resetMockData = () => {
  mockData = null
  mockDataPromise = null
  console.log('🔄 测试数据已重置')
}

// 获取所有模拟API
export const getAllMockApis = () => ({
  login: mockLoginApi,
  transfer: mockTransferApi,
  indicator: mockIndicatorApi,
  task: mockTaskApi
})
