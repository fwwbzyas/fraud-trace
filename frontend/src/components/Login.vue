<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="background-decoration">
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
      <div class="grid-pattern"></div>
      <!-- 粒子效果 -->
      <div class="particles-layer">
        <div v-for="i in 80" :key="'particle-' + i" class="particle" :style="getParticleStyle(i)"></div>
        <div v-for="i in 30" :key="'connection-' + i" class="connection" :style="getConnectionStyle(i)"></div>
      </div>
    </div>

    <!-- 登录表单区域 -->
    <div class="login-section">
      <!-- 返回首页按钮 -->
      <div class="back-button">
        <button class="back-btn" @click="goBack">
          <span class="back-icon">←</span>
          返回首页
        </button>
      </div>
      <div class="login-card">
        <div class="login-header">
          <div class="login-icon">
            <div class="icon-wrapper">
              <span class="icon">🔐</span>
            </div>
          </div>
          <h1 class="login-title">系统登录</h1>
          <p class="login-subtitle">请输入您的账户信息以访问RiskGuard系统</p>
        </div>
        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label for="username" class="form-label">账户名</label>
            <div class="input-wrapper">
              <input
                type="text"
                id="username"
                v-model="loginForm.username"
                class="form-input"
                :class="{ 'input-error': errors.username }"
                placeholder="请输入账户名"
                @blur="validateUsername"
              />
              <div class="input-icon">👤</div>
            </div>
            <div v-if="errors.username" class="error-message">{{ errors.username }}</div>
          </div>
          <div class="form-group">
            <label for="password" class="form-label">密码</label>
            <div class="input-wrapper">
              <input
                :type="showPassword ? 'text' : 'password'"
                id="password"
                v-model="loginForm.password"
                class="form-input"
                :class="{ 'input-error': errors.password }"
                placeholder="请输入密码"
                @blur="validatePassword"
              />
              <div class="input-icon">🔒</div>
              <button
                type="button"
                class="password-toggle"
                @click="togglePassword"
              >
                {{ showPassword ? '👁️' : '👁️‍🗨️' }}
              </button>
            </div>
            <div v-if="errors.password" class="error-message">{{ errors.password }}</div>
          </div>
          <div class="form-options">
            <label class="checkbox-wrapper">
              <input
                type="checkbox"
                v-model="loginForm.rememberMe"
                class="checkbox"
              />
              <span class="checkbox-label">记住我</span>
            </label>
            <a href="#" class="forgot-password" @click.prevent="handleForgotPassword">忘记密码？</a>
          </div>
          <button
            type="submit"
            class="login-btn"
            :class="{ 'btn-loading': isLoading }"
            :disabled="isLoading"
          >
            <span v-if="!isLoading">登录系统</span>
            <span v-else class="loading-content">
              <div class="loading-spinner"></div>
              登录中...
            </span>
          </button>
          <div v-if="loginError" class="error-banner">
            <div class="error-icon">⚠️</div>
            <span>{{ loginError }}</span>
          </div>
        </form>
        <div class="login-footer">
          <p class="footer-text">
            还没有账户？
            <a href="#" class="register-link" @click.prevent="handleRegister">立即注册</a>
          </p>
        </div>
      </div>
    </div>
  </div>
  
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import authService from '../services/authService.js'

const router = useRouter()

// 响应式数据
const isLoading = ref(false)
const showPassword = ref(false)
const loginError = ref('')

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 表单验证错误
const errors = reactive({
  username: '',
  password: ''
})

// 粒子样式生成
const getParticleStyle = (index) => {
  return {
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 100}%`,
    animationDelay: `${Math.random() * 20}s`,
    animationDuration: `${15 + Math.random() * 10}s`,
    opacity: 0.6 + Math.random() * 0.4
  }
}

// 连接线样式生成
const getConnectionStyle = (index) => {
  const startX = Math.random() * 100
  const startY = Math.random() * 100
  const endX = Math.random() * 100
  const endY = Math.random() * 100
  
  return {
    left: `${Math.min(startX, endX)}%`,
    top: `${Math.min(startY, endY)}%`,
    width: `${Math.abs(endX - startX)}%`,
    height: `${Math.abs(endY - startY)}%`,
    animationDelay: `${Math.random() * 15}s`,
    animationDuration: `${20 + Math.random() * 10}s`,
    opacity: 0.3 + Math.random() * 0.4
  }
}

// 表单验证
const validateUsername = () => {
  if (!loginForm.username.trim()) {
    errors.username = '请输入账户名'
    return false
  }
  if (loginForm.username.length < 3) {
    errors.username = '账户名至少需要3个字符'
    return false
  }
  errors.username = ''
  return true
}

const validatePassword = () => {
  if (!loginForm.password) {
    errors.password = '请输入密码'
    return false
  }
  if (loginForm.password.length < 6) {
    errors.password = '密码至少需要6个字符'
    return false
  }
  errors.password = ''
  return true
}

// 切换密码显示
const togglePassword = () => {
  showPassword.value = !showPassword.value
}

// 处理登录
const handleLogin = async () => {
  // 清除之前的错误
  loginError.value = ''
  
  // 验证表单
  const isUsernameValid = validateUsername()
  const isPasswordValid = validatePassword()
  
  if (!isUsernameValid || !isPasswordValid) {
    return
  }

  isLoading.value = true
  try {
    // 使用认证服务进行登录
    const result = await authService.login(loginForm.username, loginForm.password)
    
    if (result.success) {
      // 登录成功，跳转到主系统
      router.push('/indicators')
    } else {
      loginError.value = result.message
    }
  } catch (error) {
    loginError.value = '登录失败，请检查网络连接后重试'
  } finally {
    isLoading.value = false
  }
}

// 处理忘记密码
const handleForgotPassword = () => {
  alert('忘记密码功能正在开发中，请联系系统管理员')
}

// 处理注册
const handleRegister = () => {
  alert('用户注册功能正在开发中')
}

// 返回首页
const goBack = () => {
  router.push('/')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  width: 100vw;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%);
  position: relative;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

.background-decoration {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: -1;
}

.floating-shapes {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(147, 51, 234, 0.1));
  animation: float 20s infinite ease-in-out;
  filter: blur(1px);
}

.shape-1 {
  width: 200px;
  height: 200px;
  top: 10%;
  left: 5%;
}

.shape-2 {
  width: 150px;
  height: 150px;
  top: 60%;
  right: 10%;
}

.shape-3 {
  width: 100px;
  height: 100px;
  top: 30%;
  right: 20%;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  33% { transform: translateY(-20px) rotate(120deg); }
  66% { transform: translateY(10px) rotate(240deg); }
}

.grid-pattern {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-image:
      linear-gradient(rgba(59, 130, 246, 0.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(59, 130, 246, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  pointer-events: none;
  z-index: -1;
}

.particles-layer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 0;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: radial-gradient(circle, rgba(255, 255, 255, 1) 0%, rgba(255, 255, 255, 0.8) 50%, rgba(255, 255, 255, 0.3) 100%);
  border-radius: 50%;
  animation: particleFloat 25s infinite linear;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.9), 0 0 20px rgba(255, 255, 255, 0.6), 0 0 30px rgba(255, 255, 255, 0.3);
}

.connection {
  position: absolute;
  background: linear-gradient(45deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.3));
  animation: connectionPulse 30s infinite ease-in-out;
  border-radius: 1px;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.7);
}

@keyframes particleFloat {
  0% {
    transform: translateY(100vh) translateX(0) rotate(0deg) scale(0.3);
    opacity: 0;
  }
  5% {
    opacity: 0.8;
    transform: translateY(90vh) translateX(10px) rotate(45deg) scale(1);
  }
  20% {
    opacity: 1;
    transform: translateY(80vh) translateX(-5px) rotate(90deg) scale(1.2);
  }
  50% {
    opacity: 0.9;
    transform: translateY(50vh) translateX(-20px) rotate(180deg) scale(1.1);
  }
  80% {
    opacity: 0.8;
    transform: translateY(20vh) translateX(15px) rotate(270deg) scale(0.9);
  }
  95% {
    opacity: 0.6;
    transform: translateY(5vh) translateX(8px) rotate(315deg) scale(0.7);
  }
  100% {
    transform: translateY(-10vh) translateX(0) rotate(360deg) scale(0.3);
    opacity: 0;
  }
}

@keyframes connectionPulse {
  0%, 100% {
    opacity: 0.2;
    transform: scaleX(0.5) scaleY(0.5);
  }
  50% {
    opacity: 0.6;
    transform: scaleX(1.2) scaleY(1.2);
  }
}

.login-section {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.back-button {
  position: absolute;
  top: 30px;
  left: 30px;
  z-index: 10;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 8px;
  color: #94a3b8;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.back-btn:hover {
  background: rgba(148, 163, 184, 0.1);
  border-color: rgba(148, 163, 184, 0.3);
  color: #cbd5e1;
}

.back-icon {
  font-size: 16px;
}

.login-card {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 24px;
  padding: 48px;
  width: 100%;
  max-width: 480px;
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-icon {
  margin-bottom: 24px;
}

.icon-wrapper {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(147, 51, 234, 0.2));
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.icon {
  font-size: 32px;
}

.login-title {
  font-size: 32px;
  font-weight: 700;
  color: white;
  margin: 0 0 12px 0;
}

.login-subtitle {
  font-size: 16px;
  color: #94a3b8;
  margin: 0;
  line-height: 1.5;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #cbd5e1;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.form-input {
  width: 100%;
  padding: 16px 20px 16px 50px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  color: white;
  font-size: 16px;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.form-input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder {
  color: #64748b;
}

.input-error {
  border-color: rgba(239, 68, 68, 0.5) !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1) !important;
}

.input-icon {
  position: absolute;
  left: 16px;
  font-size: 18px;
  color: #64748b;
  pointer-events: none;
}

.password-toggle {
  position: absolute;
  right: 16px;
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  font-size: 18px;
  transition: color 0.3s ease;
}

.password-toggle:hover {
  color: #94a3b8;
}

.error-message {
  font-size: 12px;
  color: #ef4444;
  margin-top: 4px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox {
  width: 16px;
  height: 16px;
  accent-color: #3b82f6;
}

.checkbox-label {
  font-size: 14px;
  color: #94a3b8;
}

.forgot-password {
  font-size: 14px;
  color: #3b82f6;
  text-decoration: none;
  transition: color 0.3s ease;
}

.forgot-password:hover {
  color: #60a5fa;
}

.login-btn {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 8px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.3);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-loading {
  background: linear-gradient(135deg, #64748b, #475569);
}

.loading-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-banner {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #ef4444;
  font-size: 14px;
}

.error-icon {
  font-size: 16px;
}

.login-footer {
  text-align: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
}

.footer-text {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.register-link {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.3s ease;
}

.register-link:hover {
  color: #60a5fa;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 0 20px;
  }
  
  .login-card {
    padding: 32px 24px;
    margin: 0 10px;
  }
  
  .login-title {
    font-size: 28px;
  }
  
  .form-input {
    padding: 14px 16px 14px 45px;
    font-size: 14px;
  }
  
  .input-icon {
    left: 14px;
    font-size: 16px;
  }
  
  .password-toggle {
    right: 14px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 24px 20px;
  }
  
  .login-title {
    font-size: 24px;
  }
  
  .icon-wrapper {
    width: 60px;
    height: 60px;
  }
  
  .icon {
    font-size: 24px;
  }
}
</style>
