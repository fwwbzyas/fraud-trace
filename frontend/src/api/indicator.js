import axios from 'axios'
import { isTestMode } from '@/config/testMode'
import { mockIndicatorApi } from '@/api/mockServices'

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    console.log('API请求:', config.method.toUpperCase(), config.url, config.data)
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
    console.log('API响应:', response.status, response.data)
    return response
  },
  error => {
    console.error('响应错误:', error.response?.status, error.response?.data || error.message)
    
    // 统一错误处理
    const message = error.response?.data?.message || error.message || '网络请求失败'
    
    // 可以在这里添加全局错误提示
    // ElMessage.error(message)
    
    return Promise.reject(new Error(message))
  }
)

// 指标管理API
export const indicatorApi = {
  // 获取所有指标（包括停用的和扩展的）
  getAllIndicators() {
    if (isTestMode.value) {
      return mockIndicatorApi.getAllIndicators()
    }
    return api.get('/indicators')
  },

  // 获取激活的指标
  getActiveIndicators() {
    if (isTestMode.value) {
      return mockIndicatorApi.getActiveIndicators()
    }
    return api.get('/indicators/active')
  },

  // 获取所有指标（包括未激活的）- 用于调试
  getAllIndicatorsDebug() {
    return api.get('/indicators/all')
  },

  // 获取核心指标
  getCoreIndicators() {
    return api.get('/indicators/core')
  },

  // 获取扩展指标
  getExtendedIndicators() {
    return api.get('/indicators/extended')
  },

  // 根据ID获取指标详情
  getIndicatorById(id) {
    return api.get(`/indicators/${id}`)
  },

  // 根据代码获取指标详情
  getIndicatorByCode(code) {
    return api.get(`/indicators/code/${code}`)
  },


  // 更新指标参数
  updateParams(code, params) {
    return api.patch(`/indicators/${code}/params`, params)
  },

  // 切换指标激活状态
  toggleStatus(code, isActive) {
    return api.patch(`/indicators/${code}/toggle`, null, {
      params: { isActive }
    })
  },


  // 搜索指标
  searchIndicators(keyword) {
    return api.get('/indicators/search', {
      params: { keyword }
    })
  },

  // 获取统计信息
  getStatistics() {
    return api.get('/indicators/statistics')
  },

  // 创建指标
  createIndicator(indicatorData) {
    if (isTestMode.value) {
      return mockIndicatorApi.createIndicator(indicatorData)
    }
    return api.post('/indicators', indicatorData)
  },

  // 更新指标
  updateIndicator(code, indicatorData) {
    if (isTestMode.value) {
      return mockIndicatorApi.updateIndicator(code, indicatorData)
    }
    return api.put(`/indicators/${code}`, indicatorData)
  },

  // 删除指标
  deleteIndicator(code) {
    if (isTestMode.value) {
      return mockIndicatorApi.deleteIndicator(code)
    }
    return api.delete(`/indicators/${code}`)
  }
}

export default api
