// 测试模式配置
import { ref } from 'vue'

// 测试模式状态
export const isTestMode = ref((localStorage.getItem('testMode') ?? import.meta.env.VITE_DEMO_MODE ?? 'false') === 'true')

// 切换测试模式
export const toggleTestMode = () => {
  isTestMode.value = !isTestMode.value
  localStorage.setItem('testMode', isTestMode.value.toString())
  
  // 刷新页面以应用新模式
  window.location.reload()
}

// 设置测试模式
export const setTestMode = (enabled) => {
  isTestMode.value = enabled
  localStorage.setItem('testMode', enabled.toString())
}

// 获取当前模式显示文本
export const getCurrentModeText = () => {
  return isTestMode.value ? '测试模式' : '正常模式'
}

// 测试模式配置
export const testModeConfig = {
  // API响应延迟模拟（毫秒）
  apiDelay: 300,
  
  // 是否模拟网络错误
  simulateErrors: false,
  
  // 错误概率（0-1）
  errorProbability: 0.1,
  
  // 分页大小
  defaultPageSize: 20,
  
  // 数据生成配置
  dataGeneration: {
    accounts: 100,
    transferRecords: 500,
    loginRecords: 1000,
    tasks: 20,
    indicators: 10
  }
}
