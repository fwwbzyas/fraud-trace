import { useAuth } from '../composables/useAuth'
import { findUserByUsername, verifyPassword } from '../data/users'

// 认证服务
const authService = {
  /**
   * 用户登录
   * @param {string} username - 用户名
   * @param {string} password - 密码
   * @returns {Promise<{success: boolean, message?: string, data?: object}>}
   */
  async login(username, password) {
    try {
      // 使用现有的 useAuth
      const { login: authLogin } = useAuth()
      
      // 验证输入
      if (!username || !password) {
        return {
          success: false,
          message: '请输入用户名和密码'
        }
      }

      // 查找用户
      const user = findUserByUsername(username)
      if (!user) {
        return {
          success: false,
          message: '用户名不存在'
        }
      }

      // 验证密码
      if (!verifyPassword(user, password)) {
        return {
          success: false,
          message: '密码错误'
        }
      }

      // 调用登录，使用用户的实际角色
      const result = await authLogin(username, password, user.role)
      
      if (result.success) {
        return {
          success: true,
          message: '登录成功',
          data: result.data
        }
      } else {
        return {
          success: false,
          message: '登录失败，请检查用户名和密码'
        }
      }
    } catch (error) {
      console.error('登录错误:', error)
      return {
        success: false,
        message: '登录失败，请检查网络连接后重试'
      }
    }
  },

  /**
   * 用户登出
   */
  logout() {
    const { logout: authLogout } = useAuth()
    authLogout()
  },

  /**
   * 检查是否已登录
   */
  isAuthenticated() {
    const { isAuthenticated } = useAuth()
    return isAuthenticated.value
  }
}

export default authService


