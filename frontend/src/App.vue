<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModeSwitcher from './components/ModeSwitcher.vue'
import { DataBoard, DataAnalysis, Money, Key, User, SwitchButton, ArrowDown, CaretBottom, Document, QuestionFilled } from '@element-plus/icons-vue'
import { useAuth } from './composables/useAuth'

const route = useRoute()
const router = useRouter()
const { user, logout, isAdmin, isStaff } = useAuth()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/indicators')) return '/indicators'
  if (path.startsWith('/tasks')) return '/tasks'
  if (path.startsWith('/transfers')) return '/transfers'
  if (path.startsWith('/logins')) return '/logins'
  return '/indicators'
})

// 判断是否为首页或登录页
const isHomePage = computed(() => route.path === '/' || route.path === '/login')

// 用户角色显示文本
const roleText = computed(() => {
  if (isAdmin.value) return '管理员'
  if (isStaff.value) return '普通职员'
  return '未知'
})

// 下拉菜单命令处理
const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  }
}

// 登出
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}

// 使用指南对话框
const guideDialogVisible = ref(false)

// 显示使用指南
const showGuide = () => {
  guideDialogVisible.value = true
}
</script>

<template>
  <div class="app-container">
    <!-- 首页布局 -->
    <div class="home-layout" :class="{ 'home-active': isHomePage }">
      <router-view />
    </div>

    <!-- 内部系统布局 -->
    <div class="internal-layout" :class="{ 'internal-active': !isHomePage }">
      <!-- 顶部导航栏 -->
      <header class="app-header">
        <div class="header-content">
          <!-- 左侧：Logo和系统名称 -->
          <div class="logo-section">
            <div class="logo-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 2L4 5V11C4 16.55 7.16 21.74 12 23C16.84 21.74 20 16.55 20 11V5L12 2Z" fill="currentColor" opacity="0.1"/>
                <path d="M12 2L4 5V11C4 16.55 7.16 21.74 12 23C16.84 21.74 20 16.55 20 11V5L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M9 12L11 14L15 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <h1 class="app-title">金融电诈数据追查系统</h1>
          </div>
          
          <!-- 中间：导航菜单 -->
          <nav class="nav-menu">
            <router-link 
              to="/indicators" 
              class="nav-item"
              :class="{ 'active': activeMenu === '/indicators' }"
            >
              <el-icon><DataBoard /></el-icon>
              <span>指标管理</span>
            </router-link>
            <router-link 
              to="/tasks" 
              class="nav-item"
              :class="{ 'active': activeMenu === '/tasks' }"
            >
              <el-icon><DataAnalysis /></el-icon>
              <span>任务管理</span>
            </router-link>
            <router-link 
              to="/transfers" 
              class="nav-item"
              :class="{ 'active': activeMenu === '/transfers' }"
            >
              <el-icon><Money /></el-icon>
              <span>转账记录</span>
            </router-link>
            <router-link 
              to="/logins" 
              class="nav-item"
              :class="{ 'active': activeMenu === '/logins' }"
            >
              <el-icon><Key /></el-icon>
              <span>登录记录</span>
            </router-link>
          </nav>
          
          <!-- 右侧：功能按钮 -->
          <div class="header-right">
            <!-- 使用指南 -->
            <el-button 
              v-if="user"
              text
              :icon="QuestionFilled"
              @click="showGuide"
              class="guide-button"
              title="使用指南"
            >
              使用指南
            </el-button>
            <!-- 用户信息 -->
            <div v-if="user" class="user-info">
              <el-dropdown trigger="click" @command="handleCommand">
                <div class="user-dropdown-trigger">
                  <el-icon class="user-icon"><User /></el-icon>
                  <span class="username">{{ user.username }}</span>
                  <el-tag 
                    :type="isAdmin ? 'warning' : 'info'" 
                    size="small"
                    class="role-tag"
                  >
                    {{ roleText }}
                  </el-tag>
                  <el-icon class="dropdown-icon"><CaretBottom /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item disabled>
                      <div class="user-menu-info">
                        <div class="user-menu-name">{{ user.username }}</div>
                        <div class="user-menu-role">{{ roleText }}</div>
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item divided command="logout">
                      <el-icon><SwitchButton /></el-icon>
                      <span>退出登录</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <ModeSwitcher />
          </div>
          
          <!-- 使用指南对话框 -->
          <el-dialog
            v-model="guideDialogVisible"
            title="系统使用指南"
            width="800px"
            :close-on-click-modal="false"
          >
            <div class="guide-content">
              <el-scrollbar height="500px">
                <div class="guide-section">
                  <h3>📊 指标管理</h3>
                  <ul>
                    <li>查看和管理所有检测指标</li>
                    <li>可以查看指标的详细信息、状态和配置</li>
                    <li>核心指标会特别标注</li>
                  </ul>
                </div>
                
                <div class="guide-section">
                  <h3>📋 任务管理</h3>
                  <ul>
                    <li><strong>创建任务：</strong>点击"新建任务"按钮，按照步骤配置任务</li>
                    <li><strong>任务步骤：</strong>
                      <ol>
                        <li>填写基本信息（任务名称、描述等）</li>
                        <li>选择关联的指标（至少选择一个）</li>
                        <li>配置规则表达式（使用可视化规则构建器）</li>
                        <li>确认并提交任务</li>
                      </ol>
                    </li>
                    <li><strong>任务操作：</strong>
                      <ul>
                        <li>▶️ 启动：启动待执行的任务</li>
                        <li>⏸️ 停止：停止正在执行的任务</li>
                        <li>👁️ 查看结果：查看已完成任务的分析结果</li>
                        <li>🗑️ 删除：删除任务（仅管理员，且任务不能处于执行中状态）</li>
                      </ul>
                    </li>
                    <li><strong>任务状态：</strong>待执行、执行中、已完成、执行失败</li>
                  </ul>
                </div>
                
                <div class="guide-section">
                  <h3>💸 转账记录</h3>
                  <ul>
                    <li>查看和分析账户间的转账记录</li>
                    <li>支持按账户、时间范围等条件筛选</li>
                    <li>可以查看转账详情和关联信息</li>
                  </ul>
                </div>
                
                <div class="guide-section">
                  <h3>🔑 登录记录</h3>
                  <ul>
                    <li>查看系统登录历史记录</li>
                    <li>支持按账户、IP地址、时间范围筛选</li>
                    <li>可以查看登录详情和关联账户信息</li>
                  </ul>
                </div>
                
                <div class="guide-section">
                  <h3>💡 使用技巧</h3>
                  <ul>
                    <li>使用搜索功能快速查找内容</li>
                    <li>利用筛选条件精确查找数据</li>
                    <li>任务创建时建议选择多个相关指标以提高检测准确性</li>
                    <li>规则表达式支持复杂的逻辑组合</li>
                    <li>管理员拥有更多操作权限，如删除任务等</li>
                  </ul>
                </div>
                
                <div class="guide-section">
                  <h3>❓ 常见问题</h3>
                  <ul>
                    <li><strong>Q: 如何创建任务？</strong><br>
                    A: 在任务管理页面点击"新建任务"，按照四个步骤完成配置即可。</li>
                    <li><strong>Q: 任务执行失败怎么办？</strong><br>
                    A: 检查任务配置是否正确，特别是规则表达式和指标选择，然后重新创建任务。</li>
                    <li><strong>Q: 如何查看任务结果？</strong><br>
                    A: 任务完成后，点击操作列的"查看结果"按钮，或直接点击任务行进入结果页面。</li>
                    <li><strong>Q: 可以删除正在执行的任务吗？</strong><br>
                    A: 不可以，必须先停止任务才能删除。</li>
                  </ul>
                </div>
              </el-scrollbar>
            </div>
            <template #footer>
              <el-button @click="guideDialogVisible = false">关闭</el-button>
            </template>
          </el-dialog>
        </div>
      </header>

      <!-- 主内容区 -->
      <main class="app-main">
        <router-view />
      </main>
      
      <!-- AI智能助手 -->
    </div>
  </div>
</template>

<style scoped>
.app-container {
  height: 100vh;
  width: 100vw;
  margin: 0;
  padding: 0;
  overflow: hidden;
  position: relative;
}

/* 首页布局 */
.home-layout {
  position: absolute;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1000;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.home-layout.home-active {
  opacity: 1;
  pointer-events: auto;
}

/* 内部系统布局 */
.internal-layout {
  position: absolute;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
  background-color: #f5f7fa;
}

.internal-layout.internal-active {
  opacity: 1;
  pointer-events: auto;
}

/* 确保Welcome组件全屏显示 */
:deep(.welcome-container) {
  height: 100vh !important;
  width: 100vw !important;
  margin: 0 !important;
  padding: 0 !important;
  overflow-y: auto !important;
  overflow-x: hidden !important;
}

/* 顶部导航栏 */
.app-header {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 32px;
  max-width: 100%;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: #409EFF;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
}

.logo-icon:hover {
  background: #66b1ff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  transform: translateY(-1px);
}

.logo-icon svg {
  width: 24px;
  height: 24px;
}

.app-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

/* 导航菜单 */
.nav-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  justify-content: center;
  margin: 0 40px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  color: #606266;
  text-decoration: none;
  border-radius: 6px;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  position: relative;
}

.nav-item:hover {
  background-color: #f5f7fa;
  color: #409EFF;
}

.nav-item.active {
  color: #409EFF;
  background-color: #ecf5ff;
}

.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 3px;
  background: #409EFF;
  border-radius: 2px 2px 0 0;
}

.nav-item .el-icon {
  font-size: 18px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

/* 使用指南按钮 */
.guide-button {
  color: #606266;
  font-size: 14px;
  padding: 6px 12px;
  transition: all 0.2s ease;
}

.guide-button:hover {
  color: #409EFF;
  background-color: #f5f7fa;
}

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-dropdown-trigger:hover {
  background-color: #f5f7fa;
}

.user-icon {
  font-size: 18px;
  color: #606266;
}

.username {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.role-tag {
  margin: 0;
}

.dropdown-icon {
  font-size: 12px;
  color: #909399;
}

.user-menu-info {
  padding: 4px 0;
}

.user-menu-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.user-menu-role {
  font-size: 12px;
  color: #909399;
}

/* 使用指南对话框样式 */
.guide-content {
  padding: 0;
}

.guide-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.guide-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.guide-section h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.guide-section ul,
.guide-section ol {
  margin: 8px 0;
  padding-left: 24px;
  color: #606266;
  line-height: 1.8;
}

.guide-section li {
  margin: 6px 0;
  font-size: 14px;
}

.guide-section li strong {
  color: #303133;
  font-weight: 600;
}

.guide-section ol {
  margin-left: 20px;
}

.guide-section ol li {
  margin: 4px 0;
}

.app-main {
  flex: 1;
  overflow-y: auto;
  background-color: #f5f7fa;
  padding: 0;
}

/* 全局样式重置 */
:global(body) {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

:global(html) {
  margin: 0;
  padding: 0;
  height: 100%;
}

:global(#app) {
  height: 100vh;
  width: 100vw;
  margin: 0;
  padding: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }
  
  .app-title {
    font-size: 16px;
  }
  
  .nav-menu {
    margin: 0 16px;
    gap: 4px;
  }
  
  .nav-item {
    padding: 8px 12px;
    font-size: 13px;
  }
  
  .nav-item span {
    display: none;
  }
}
</style>
