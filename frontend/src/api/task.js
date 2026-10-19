import axios from 'axios'
import { isTestMode } from '@/config/testMode'
import { mockTaskApi } from '@/api/mockServices'

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000, // 增加超时时间，任务可能需要较长时间
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    console.log('任务API请求:', config.method.toUpperCase(), config.url, config.data)
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
    console.log('任务API响应:', response.status, response.data)
    return response
  },
  error => {
    console.error('响应错误:', error.response?.status, error.response?.data || error.message)
    
    const message = error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  }
)

// 任务管理API
export const taskApi = {
  // 获取所有任务
  getAllTasks() {
    if (isTestMode.value) {
      return mockTaskApi.getAllTasks()
    }
    return api.get('/tasks')
  },

  // 根据ID获取任务详情
  getTaskById(id) {
    if (isTestMode.value) {
      return mockTaskApi.getTaskById(id)
    }
    return api.get(`/tasks/${id}`)
  },

  // 创建新任务
  createTask(data) {
    if (isTestMode.value) {
      return mockTaskApi.createTask(data)
    }
    return api.post('/tasks', data)
  },

  // 启动任务
  startTask(id) {
    if (isTestMode.value) {
      return mockTaskApi.startTask(id)
    }
    return api.post(`/tasks/${id}/start`)
  },

  // 停止任务
  stopTask(id) {
    if (isTestMode.value) {
      return mockTaskApi.stopTask(id)
    }
    return api.post(`/tasks/${id}/stop`)
  },

  // 删除任务
  deleteTask(id) {
    if (isTestMode.value) {
      return mockTaskApi.deleteTask(id)
    }
    return api.delete(`/tasks/${id}`)
  },

  // 根据状态获取任务
  getTasksByStatus(status) {
    return api.get(`/tasks/status/${status}`)
  },

  // 搜索任务
  searchTasks(keyword) {
    return api.get('/tasks/search', {
      params: { keyword }
    })
  },

  // 获取任务统计信息
  getTaskStatistics() {
    if (isTestMode.value) {
      return mockTaskApi.getTaskStatistics()
    }
    return api.get('/tasks/statistics')
  },

  // 获取任务结果
  getTaskResults(id) {
    if (isTestMode.value) {
      return mockTaskApi.getTaskDetailedResults(id)
    }
    return api.get(`/tasks/${id}/results`)
  },

  // 获取任务的详细结果
  getTaskDetailedResults(id, page = 1, pageSize = 100, accountRole = null) {
    if (isTestMode.value) {
      return mockTaskApi.getTaskDetailedResults(id)
    }
    const params = { page, pageSize }
    if (accountRole !== null && accountRole !== 'all') {
      params.accountRole = accountRole
    }
    return api.get(`/tasks/${id}/detailed-results`, { params })
  }
}

export default api
