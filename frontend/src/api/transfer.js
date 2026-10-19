import axios from 'axios'
import { isTestMode } from '@/config/testMode'
import { mockTransferApi } from '@/api/mockServices'

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    console.log('转账记录API请求:', config.method.toUpperCase(), config.url, config.params)
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    console.log('转账记录API响应:', response.status, response.data)
    return response
  },
  error => {
    console.error('响应错误:', error.response?.status, error.response?.data || error.message)
    
    const message = error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  }
)

// 转账记录管理API - 重新实现
export const transferApi = {
  // 分页查询转账记录
  getTransferRecords(page = 0, size = 20) {
    if (isTestMode.value) {
      return mockTransferApi.getAllTransfers(page, size)
    }
    return api.get('/transfers', {
      params: { page, size }
    })
  },

  // 搜索转账记录 - 简化版本
  searchTransferRecords(searchParams) {
    if (isTestMode.value) {
      return mockTransferApi.searchTransfers(searchParams)
    }
    
    const {
      transferNo,
      fromAccountName,
      toAccountName,
      minAmount,
      maxAmount,
      startTime,
      endTime,
      page = 0,
      size = 20
    } = searchParams

    // 清理空值参数
    const params = {}
    if (transferNo && transferNo.trim()) params.transferNo = transferNo.trim()
    if (fromAccountName && fromAccountName.trim()) params.fromAccountName = fromAccountName.trim()
    if (toAccountName && toAccountName.trim()) params.toAccountName = toAccountName.trim()
    if (minAmount !== null && minAmount !== undefined) params.minAmount = minAmount.toString()
    if (maxAmount !== null && maxAmount !== undefined) params.maxAmount = maxAmount.toString()
    if (startTime && startTime.trim()) params.startTime = startTime.trim()
    if (endTime && endTime.trim()) params.endTime = endTime.trim()
    params.page = page
    params.size = size

    return api.get('/transfers/search', { params })
  },

  // 快速筛选 - 大额交易
  getLargeAmountTransfers(page = 0, size = 20) {
    if (isTestMode.value) {
      return mockTransferApi.searchTransfers({ minAmount: 50000, page, size })
    }
    return api.get('/transfers/large-amount', {
      params: { page, size }
    })
  },

  // 快速筛选 - 今日交易
  getTodayTransfers(page = 0, size = 20) {
    if (isTestMode.value) {
      const today = new Date().toISOString().slice(0, 10)
      return mockTransferApi.searchTransfers({ 
        startTime: today + ' 00:00:00', 
        endTime: today + ' 23:59:59', 
        page, size 
      })
    }
    return api.get('/transfers/today', {
      params: { page, size }
    })
  },

  // 快速筛选 - 本周交易
  getWeekTransfers(page = 0, size = 20) {
    if (isTestMode.value) {
      const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()
      return mockTransferApi.searchTransfers({ 
        startTime: weekAgo, 
        page, size 
      })
    }
    return api.get('/transfers/week', {
      params: { page, size }
    })
  },

  // 根据ID获取转账记录详情
  getTransferRecordById(id) {
    if (isTestMode.value) {
      return mockTransferApi.getTransferById(id)
    }
    return api.get(`/transfers/${id}`)
  },

  // 根据流水号获取转账记录
  getTransferRecordByNo(transferNo) {
    if (isTestMode.value) {
      return mockTransferApi.searchTransfers({ transferNo })
    }
    return api.get(`/transfers/by-no/${transferNo}`)
  },

  // 获取账户相关的转账记录
  getAccountTransferRecords(accountId) {
    if (isTestMode.value) {
      return mockTransferApi.getAccountTransfers(accountId)
    }
    return api.get(`/transfers/account/${accountId}`)
  },

  // 获取账户转出记录
  getAccountOutboundRecords(accountId) {
    if (isTestMode.value) {
      return mockTransferApi.getAccountTransfers(accountId)
    }
    return api.get(`/transfers/account/${accountId}/outbound`)
  },

  // 获取账户转入记录
  getAccountInboundRecords(accountId) {
    if (isTestMode.value) {
      return mockTransferApi.getAccountTransfers(accountId)
    }
    return api.get(`/transfers/account/${accountId}/inbound`)
  },

  // 获取转账统计信息
  getTransferStatistics() {
    if (isTestMode.value) {
      return mockTransferApi.getTransferStatistics()
    }
    return api.get('/transfers/statistics')
  },

  // 获取账户转账统计
  getAccountTransferStatistics(accountId) {
    if (isTestMode.value) {
      return mockTransferApi.getTransferStatistics()
    }
    return api.get(`/transfers/account/${accountId}/statistics`)
  },

  // 测试服务
  testService() {
    if (isTestMode.value) {
      return Promise.resolve({ data: { success: true, message: '转账服务正常（测试模式）' } })
    }
    return api.get('/transfers/test')
  }
}

export default api
