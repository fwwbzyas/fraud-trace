import './assets/main.css'
import { initDifyEmbed } from './config/difyEmbed'

import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createRouter, createWebHistory } from 'vue-router'

// 导入页面组件
import Welcome from './components/Welcome.vue'
import Login from './components/Login.vue'
import IndicatorList from './components/IndicatorList.vue'
import TaskManagement from './components/TaskManagement.vue'
import TaskResults from './components/TaskResults.vue'
import TransferManagement from './components/TransferManagement.vue'
import LoginManagement from './components/LoginManagement.vue'

// 路由配置
const routes = [
  { path: '/', component: Welcome, name: 'Welcome' },
  { path: '/login', component: Login, name: 'Login' },
  { path: '/indicators', component: IndicatorList, name: 'IndicatorList' },
  { path: '/tasks', component: TaskManagement, name: 'TaskManagement' },
  { path: '/tasks/:id/results', component: TaskResults, name: 'TaskResults' },
  { path: '/transfers', component: TransferManagement, name: 'TransferManagement' },
  { path: '/logins', component: LoginManagement, name: 'LoginManagement' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查登录状态
router.beforeEach((to, from, next) => {
  // 公开路由，不需要登录
  const publicRoutes = ['/', '/login']
  
  if (publicRoutes.includes(to.path)) {
    next()
    return
  }
  
  // 检查是否已登录
  const user = localStorage.getItem('user')
  if (!user) {
    // 未登录，跳转到登录页
    next('/login')
    return
  }
  
  // 已登录，允许访问
  next()
})

const app = createApp(App)

// 注册ElementPlus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus)
app.use(router)
app.mount('#app')

initDifyEmbed()
