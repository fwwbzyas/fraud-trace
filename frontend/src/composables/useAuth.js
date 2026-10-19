import { ref, computed } from 'vue'

// 用户信息
const user = ref(null)

// 从 localStorage 恢复用户信息
const loadUserFromStorage = () => {
  const storedUser = localStorage.getItem('user')
  if (storedUser) {
    try {
      user.value = JSON.parse(storedUser)
    } catch (e) {
      console.error('Failed to parse user from storage:', e)
      localStorage.removeItem('user')
    }
  }
}

// 初始化时加载用户信息
loadUserFromStorage()

// 登录
const login = (username, password, role) => {
  // 这里可以调用真实的登录 API
  // 目前使用模拟数据
  const userData = {
    username,
    role, // 'admin' 或 'staff'
    loginTime: new Date().toISOString()
  }
  
  user.value = userData
  localStorage.setItem('user', JSON.stringify(userData))
  
  return Promise.resolve({
    success: true,
    data: userData
  })
}

// 登出
const logout = () => {
  user.value = null
  localStorage.removeItem('user')
}

// 检查是否已登录
const isAuthenticated = computed(() => {
  return user.value !== null
})

// 检查是否是管理员
const isAdmin = computed(() => {
  return user.value?.role === 'admin'
})

// 检查是否是普通职员
const isStaff = computed(() => {
  return user.value?.role === 'staff'
})

export function useAuth() {
  return {
    user,
    login,
    logout,
    isAuthenticated,
    isAdmin,
    isStaff,
    loadUserFromStorage
  }
}









