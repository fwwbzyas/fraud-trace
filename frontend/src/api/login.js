import axios from 'axios'
import { isTestMode } from '@/config/testMode'
import { mockLoginApi } from '@/api/mockServices'

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
    console.log('登录记录API请求:', config.method.toUpperCase(), config.url, config.params)
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
    console.log('登录记录API响应:', response.status, response.data)
    return response
  },
  error => {
    console.error('响应错误:', error.response?.status, error.response?.data || error.message)
    
    const message = error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  }
)

// 登录记录管理API
export const loginApi = {
  // 分页查询登录记录
  getLoginRecords(page = 0, size = 20) {
    if (isTestMode.value) {
      return mockLoginApi.getLoginRecords(page, size)
    }
    return api.get('/logins', {
      params: { page, size }
    })
  },

  // 搜索登录记录
  searchLoginRecords(searchParams) {
    if (isTestMode.value) {
      return mockLoginApi.searchLoginRecords(searchParams)
    }
    
    const {
      accountName,
      loginIp,
      loginResult,
      startTime,
      endTime,
      page = 0,
      size = 20
    } = searchParams

    return api.get('/logins/search', {
      params: {
        accountName,
        loginIp,
        loginResult,
        startTime,
        endTime,
        page,
        size
      }
    })
  },

  // 根据ID获取登录记录详情
  getLoginRecordById(id) {
    if (isTestMode.value) {
      return mockLoginApi.getLoginRecordById(id)
    }
    return api.get(`/logins/${id}`)
  },

  // 根据账户ID获取登录记录
  getAccountLoginRecords(accountId) {
    if (isTestMode.value) {
      return mockLoginApi.getAccountLoginRecords(accountId)
    }
    return api.get(`/logins/account/${accountId}`)
  },

  // 获取成功登录记录
  getSuccessfulLogins() {
    if (isTestMode.value) {
      return mockLoginApi.getSuccessfulLogins()
    }
    return api.get('/logins/successful')
  },

  // 获取失败登录记录
  getFailedLogins() {
    if (isTestMode.value) {
      return mockLoginApi.getFailedLogins()
    }
    return api.get('/logins/failed')
  },

  // 获取登录统计信息
  getLoginStatistics() {
    if (isTestMode.value) {
      return mockLoginApi.getLoginStatistics()
    }
    return api.get('/logins/statistics')
  },

  // 获取账户登录统计
  getAccountLoginStatistics(accountId) {
    if (isTestMode.value) {
      return mockLoginApi.getAccountLoginStatistics(accountId)
    }
    return api.get(`/logins/account/${accountId}/statistics`)
  },

  // 测试接口
  testService() {
    if (isTestMode.value) {
      return mockLoginApi.testService()
    }
    return api.get('/logins/test')
  }
}

export default api

