<template>
  <div class="welcome-container">
    <!-- 背景装饰 - 宇宙星空风格 -->
    <div class="background-decoration">
      <!-- 深空背景 -->
      <div class="deep-space"></div>
      <!-- 星空背景 -->
      <div class="starfield">
        <div v-for="i in 200" :key="i" class="star" :style="getStarStyle(i)"></div>
      </div>
      <!-- 大星星 -->
      <div class="bright-stars">
        <div v-for="i in 15" :key="i" class="bright-star" :style="getBrightStarStyle(i)"></div>
      </div>
      <!-- 星云效果 -->
      <div class="nebula nebula-1"></div>
      <div class="nebula nebula-2"></div>
      <!-- 大气层光晕（地平线） -->
      <div class="atmosphere-horizon"></div>
      <!-- 动态光点 -->
      <div class="glowing-particles">
        <div v-for="i in 6" :key="i" class="glow-particle" :style="getGlowParticleStyle(i)"></div>
      </div>
      <!-- 流星效果 -->
      <div class="meteors">
        <div v-for="i in 2" :key="i" class="meteor-wrapper" :style="getMeteorStyle(i)">
          <div class="meteor-head"></div>
          <div class="meteor-tail"></div>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="main-content">
      <!-- 顶部导航栏 -->
      <nav class="top-nav">
        <div class="nav-brand">
          <div class="brand-logo">
            <div class="logo-icon">🔍</div>
            <span class="brand-text">金融电诈数据追查系统</span>
          </div>
        </div>
        <div class="nav-actions">
          <button class="nav-btn primary" @click="handleLogin">登录</button>
        </div>
      </nav>

      <!-- 英雄区域 -->
      <section class="hero-section">
        <div class="hero-content">
          <div class="hero-text">
            <h1 class="hero-title">
              <span class="gradient-text">金融电诈数据追查系统</span>
            </h1>
            <p class="hero-subtitle">
              基于AI的智能风险检测系统，为金融机构提供全方位的电诈风险防护解决方案
            </p>
            <div class="hero-features">
              <div
                  v-for="(feature, index) in heroFeatures"
                  :key="index"
                  class="feature-item"
                  :class="{ 'feature-hover': feature.hovered }"
                  @mouseenter="feature.hovered = true"
                  @mouseleave="feature.hovered = false"
                  :style="{ animationDelay: `${index * 0.2}s` }"
              >
                <div class="feature-icon" :class="{ 'icon-spin': feature.hovered }">{{ feature.icon }}</div>
                <span>{{ feature.text }}</span>
              </div>
            </div>
            <div class="hero-actions">
              <button
                  class="btn-primary"
                  @click="handleLogin"
                  :class="{ 'btn-glow': primaryBtnHovered }"
                  @mouseenter="primaryBtnHovered = true"
                  @mouseleave="primaryBtnHovered = false"
              >
                <span>立即开始</span>
                <div class="btn-arrow" :class="{ 'arrow-bounce': primaryBtnHovered }">→</div>
              </button>
              <button
                  class="btn-secondary"
                  @click="playDemo"
                  :class="{ 'btn-pulse': demoBtnHovered }"
                  @mouseenter="demoBtnHovered = true"
                  @mouseleave="demoBtnHovered = false"
              >
                <span>观看演示</span>
                <div class="play-icon" :class="{ 'play-bounce': demoBtnHovered }">▶</div>
              </button>
            </div>
          </div>
          <div class="hero-visual">
            <div class="dashboard-preview">
              <div class="preview-header">
                <div class="preview-dots">
                  <span v-for="i in 3" :key="i" :class="{ 'dot-active': activeDot === i }"></span>
                </div>
                <span class="preview-title">实时监控面板</span>
              </div>
              <div class="preview-content">
                <div class="preview-chart">
                  <div class="chart-line" :class="{ 'line-animate': chartAnimated }"></div>
                  <div class="chart-points">
                    <div
                        v-for="i in 6"
                        :key="i"
                        class="point"
                        :class="{ 'point-pulse': activePoint === i }"
                        :style="{ animationDelay: `${i * 0.3}s` }"
                    ></div>
                  </div>
                </div>
                <div class="preview-metrics">
                  <div
                      v-for="(metric, index) in realtimeMetrics"
                      :key="index"
                      class="metric"
                      :class="{ 'metric-update': metric.updating }"
                  >
                    <div class="metric-value" :class="{ 'value-flash': metric.flash }">
                      {{ metric.value }}
                    </div>
                    <div class="metric-label">{{ metric.label }}</div>
                    <div class="metric-trend" :class="metric.trend">
                      {{ metric.trend === 'up' ? '↗' : '↘' }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 功能特性区域 -->
      <section class="features-section">
        <div class="section-header">
          <h2>核心功能</h2>
          <p>专业的风险检测工具集，助力金融机构构建完善的风控体系</p>
        </div>
        <div class="features-grid">
          <div
              v-for="(feature, index) in features"
              :key="index"
              class="feature-card"
              :class="{ 'card-hover': feature.hovered }"
              @mouseenter="feature.hovered = true"
              @mouseleave="feature.hovered = false"
              :style="{ animationDelay: `${index * 0.1}s` }"
          >
            <div class="feature-card-header">
              <div class="feature-icon-wrapper" :class="{ 'icon-glow': feature.hovered }">
                <div class="feature-icon">{{ feature.icon }}</div>
              </div>
              <h3>{{ feature.title }}</h3>
            </div>
            <p>{{ feature.description }}</p>
            <div class="feature-stats">
              <div class="stat">
                <span class="stat-value" :class="{ 'stat-animate': feature.hovered }">{{ feature.stat1 }}</span>
                <span class="stat-label">{{ feature.label1 }}</span>
              </div>
              <div class="stat">
                <span class="stat-value" :class="{ 'stat-animate': feature.hovered }">{{ feature.stat2 }}</span>
                <span class="stat-label">{{ feature.label2 }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 底部CTA -->
      <section class="cta-section">
        <div class="cta-content">
          <h2>准备开始您的风险防护之旅？</h2>
          <p>加入数百家金融机构的选择，构建更安全的金融环境</p>
          <div class="cta-actions">
            <button class="btn-primary large" @click="handleLogin">
              免费试用
            </button>
            <button class="btn-outline large" @click="contactSales">
              联系销售
            </button>
          </div>
        </div>
      </section>
    </div>

    <!-- 实时通知 -->
    <div class="live-notifications" v-if="showNotifications">
      <div
          v-for="notification in liveNotifications"
          :key="notification.id"
          class="notification-item"
          :class="notification.type"
      >
        <div class="notification-icon">{{ notification.icon }}</div>
        <div class="notification-content">
          <div class="notification-title">{{ notification.title }}</div>
          <div class="notification-message">{{ notification.message }}</div>
        </div>
        <div class="notification-time">{{ notification.time }}</div>
      </div>
    </div>
    
    <!-- AI智能助手 -->
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 响应式数据
const primaryBtnHovered = ref(false)
const demoBtnHovered = ref(false)
const activePoint = ref(1)
const activeDot = ref(1)
const chartAnimated = ref(false)

// 英雄区域特性
const heroFeatures = ref([
  { icon: '🤖', text: 'AI智能分析', hovered: false },
  { icon: '📊', text: '实时数据监控', hovered: false },
  { icon: '🛡️', text: '风险预警系统', hovered: false }
])

// 功能特性
const features = ref([
  {
    icon: '📈',
    title: '智能风险检测',
    description: '基于机器学习的智能算法，自动识别和预警各类电诈风险',
    stat1: '99.8%',
    label1: '检测准确率',
    stat2: '<100ms',
    label2: '响应时间',
    hovered: false
  },
  {
    icon: '🔍',
    title: '深度数据挖掘',
    description: '多维度数据分析，挖掘隐藏的风险模式和关联关系',
    stat1: '1000+',
    label1: '风险维度',
    stat2: '24/7',
    label2: '实时监控',
    hovered: false
  },
  {
    icon: '📊',
    title: '可视化分析',
    description: '直观的图表展示，帮助分析师快速理解风险态势',
    stat1: '50+',
    label1: '图表类型',
    stat2: '实时',
    label2: '数据更新',
    hovered: false
  },
  {
    icon: '⚙️',
    title: '灵活配置',
    description: '支持自定义规则和策略，满足不同机构的风控需求',
    stat1: '100+',
    label1: '预设规则',
    stat2: '无限',
    label2: '自定义规则',
    hovered: false
  }
])

// 实时指标
const realtimeMetrics = ref([
  { value: '98.5%', label: '准确率', trend: 'up', updating: false, flash: false },
  { value: '2.3s', label: '响应时间', trend: 'down', updating: false, flash: false },
  { value: '99.9%', label: '可用性', trend: 'up', updating: false, flash: false }
])

const handleLogin = () => {
  router.push('/login')
}

const playDemo = () => {
  console.log('播放演示视频')
}

const contactSales = () => {
  console.log('联系销售团队')
}

const getStarStyle = (index) => {
  const size = Math.random() * 1.5 + 0.3
  const opacity = Math.random() * 0.9 + 0.1
  const delay = Math.random() * 4
  const duration = Math.random() * 3 + 2
  
  return {
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 75}%`, // 大部分在上半部分
    width: `${size}px`,
    height: `${size}px`,
    opacity: opacity,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

const getBrightStarStyle = (index) => {
  const size = Math.random() * 3 + 2
  const left = Math.random() * 100
  const top = Math.random() * 70
  const delay = Math.random() * 3
  const duration = Math.random() * 2 + 1.5
  
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

const getGlowParticleStyle = (index) => {
  const size = Math.random() * 80 + 40
  const left = Math.random() * 100
  const top = Math.random() * 80 + 10 // 主要在中上部
  const delay = Math.random() * 5
  
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`
  }
}

const getMeteorStyle = (index) => {
  // 从左上角外开始
  const startX = -10 + Math.random() * 5
  const startY = -10 + Math.random() * 5
  
  // 斜向下到右下，角度约45度
  const angle = 35 + Math.random() * 20 // 35-55度
  const distance = 150 + Math.random() * 50 // 移动距离
  
  const endX = startX + distance * Math.cos(angle * Math.PI / 180)
  const endY = startY + distance * Math.sin(angle * Math.PI / 180)
  
  // 随机延迟，让流星分散出现
  const delay = index * 12 + Math.random() * 10
  const duration = 0.8 + Math.random() * 0.4 // 0.8-1.2秒，快速闪过
  
  return {
    '--start-x': `${startX}%`,
    '--start-y': `${startY}%`,
    '--end-x': `${endX}%`,
    '--end-y': `${endY}%`,
    '--angle': `${angle}deg`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

// 实时通知
const showNotifications = ref(false)
const liveNotifications = ref([
  {
    id: 1,
    type: 'success',
    icon: '✅',
    title: '系统运行正常',
    message: '所有服务运行状态良好',
    time: '刚刚'
  },
  {
    id: 2,
    type: 'info',
    icon: '📊',
    title: '数据更新完成',
    message: '最新风险数据已同步完成',
    time: '2分钟前'
  }
])

const showLiveNotifications = () => {
  showNotifications.value = true
  setTimeout(() => {
    showNotifications.value = false
  }, 5000)
}

const updateMetrics = () => {
  realtimeMetrics.value.forEach((metric) => {
    metric.updating = true
    metric.flash = true

    setTimeout(() => {
      if (metric.label === '准确率') {
        metric.value = (98 + Math.random() * 2).toFixed(1) + '%'
      } else if (metric.label === '响应时间') {
        metric.value = (1.5 + Math.random() * 2).toFixed(1) + 's'
      } else if (metric.label === '可用性') {
        metric.value = (99.5 + Math.random() * 0.5).toFixed(1) + '%'
      }

      metric.updating = false
      metric.flash = false
    }, 500)
  })
}

const animateChart = () => {
  chartAnimated.value = true
  activePoint.value = (activePoint.value % 6) + 1
}

// 定时器
let notificationTimer = null
let metricsTimer = null
let chartTimer = null

// 生命周期
onMounted(() => {
  // 启动实时更新
  notificationTimer = setInterval(showLiveNotifications, 15000)
  metricsTimer = setInterval(updateMetrics, 8000)
  chartTimer = setInterval(animateChart, 3000)

  // 启动图表动画
  setTimeout(() => {
    chartAnimated.value = true
  }, 2000)
})

onUnmounted(() => {
  if (notificationTimer) clearInterval(notificationTimer)
  if (metricsTimer) clearInterval(metricsTimer)
  if (chartTimer) clearInterval(chartTimer)
})
</script>

<style scoped>
.welcome-container {
  min-height: 100vh;
  background: #000000;
  position: relative;
  overflow: hidden;
}

.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

/* 深空背景 */
.deep-space {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: 
    radial-gradient(ellipse 100% 80% at 50% 20%, rgba(30, 30, 60, 0.3) 0%, transparent 50%),
    radial-gradient(ellipse 120% 100% at 50% 50%, rgba(20, 20, 40, 0.4) 0%, transparent 60%),
    linear-gradient(to bottom, #000000 0%, #0a0a15 30%, #0f0f1a 60%, #000000 100%);
  z-index: 0;
}

/* 星空背景 */
.starfield {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
}

.star {
  position: absolute;
  background: #ffffff;
  border-radius: 50%;
  animation: twinkle infinite ease-in-out;
  box-shadow: 0 0 2px rgba(255, 255, 255, 0.8);
}

@keyframes twinkle {
  0%, 100% { 
    opacity: 0.3; 
    transform: scale(1);
    box-shadow: 0 0 2px rgba(255, 255, 255, 0.5);
  }
  50% { 
    opacity: 1; 
    transform: scale(1.3);
    box-shadow: 0 0 4px rgba(255, 255, 255, 1);
  }
}

/* 大星星 */
.bright-stars {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 2;
}

.bright-star {
  position: absolute;
  background: radial-gradient(circle, #ffffff 0%, rgba(255, 255, 255, 0.3) 50%, transparent 100%);
  border-radius: 50%;
  animation: bright-twinkle infinite ease-in-out;
  box-shadow: 
    0 0 6px rgba(255, 255, 255, 0.8),
    0 0 12px rgba(200, 220, 255, 0.6);
}

@keyframes bright-twinkle {
  0%, 100% { 
    opacity: 0.6; 
    transform: scale(1);
  }
  50% { 
    opacity: 1; 
    transform: scale(1.4);
  }
}

/* 星云效果 */
.nebula {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.15;
  animation: nebula-drift 30s infinite ease-in-out;
  z-index: 1;
}

.nebula-1 {
  width: 600px;
  height: 400px;
  top: 10%;
  left: -10%;
  background: radial-gradient(ellipse, rgba(100, 150, 255, 0.4) 0%, rgba(150, 100, 255, 0.2) 50%, transparent 100%);
  animation-duration: 40s;
}

.nebula-2 {
  width: 500px;
  height: 350px;
  top: 30%;
  right: -5%;
  background: radial-gradient(ellipse, rgba(255, 100, 150, 0.3) 0%, rgba(255, 150, 100, 0.15) 50%, transparent 100%);
  animation-duration: 35s;
  animation-delay: -10s;
}

@keyframes nebula-drift {
  0%, 100% { 
    transform: translate(0, 0) scale(1);
    opacity: 0.15;
  }
  50% { 
    transform: translate(30px, -20px) scale(1.1);
    opacity: 0.2;
  }
}

/* 大气层光晕（地平线） */
.atmosphere-horizon {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 30%;
  background: 
    radial-gradient(ellipse 200% 100% at 50% 0%, 
      rgba(255, 120, 80, 0.25) 0%,
      rgba(255, 150, 100, 0.15) 20%,
      rgba(255, 180, 140, 0.08) 40%,
      rgba(255, 200, 160, 0.04) 60%,
      transparent 80%
    );
  filter: blur(80px);
  z-index: 2;
  pointer-events: none;
}


/* 动态光点 */
.glowing-particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.glow-particle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(59, 130, 246, 0.4) 0%,
    rgba(147, 51, 234, 0.2) 40%,
    transparent 70%
  );
  filter: blur(20px);
  animation: glow-pulse 8s infinite ease-in-out;
  transform: translate(-50%, -50%);
}

@keyframes glow-pulse {
  0%, 100% { 
    opacity: 0.3;
    transform: translate(-50%, -50%) scale(1);
  }
  50% { 
    opacity: 0.6;
    transform: translate(-50%, -50%) scale(1.2);
  }
}

/* 流星效果 */
.meteors {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 3;
  overflow: hidden;
  pointer-events: none;
}

.meteor-wrapper {
  position: absolute;
  left: var(--start-x);
  top: var(--start-y);
  transform: rotate(var(--angle));
  animation: meteor-move linear infinite;
  opacity: 0;
}

.meteor-head {
  position: absolute;
  width: 6px;
  height: 6px;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 
    0 0 10px rgba(255, 255, 255, 1),
    0 0 20px rgba(200, 220, 255, 0.8),
    0 0 30px rgba(150, 200, 255, 0.6);
  transform: translate(-50%, -50%);
}

.meteor-tail {
  position: absolute;
  width: 2px;
  height: 120px;
  background: linear-gradient(
    to bottom,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(200, 220, 255, 0.7) 20%,
    rgba(150, 200, 255, 0.5) 40%,
    rgba(100, 180, 255, 0.3) 60%,
    rgba(59, 130, 246, 0.1) 80%,
    transparent 100%
  );
  top: 0;
  left: 0;
  transform: translate(-50%, -50%);
  transform-origin: top center;
  box-shadow: 
    0 0 8px rgba(255, 255, 255, 0.6),
    0 0 16px rgba(150, 200, 255, 0.4);
}

@keyframes meteor-move {
  0% {
    opacity: 0;
    transform: translate(0, 0) rotate(var(--angle));
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
    transform: translate(
      calc((var(--end-x) - var(--start-x)) * 1%), 
      calc((var(--end-y) - var(--start-y)) * 1%)
    ) rotate(var(--angle));
  }
  100% {
    opacity: 0;
    transform: translate(
      calc((var(--end-x) - var(--start-x)) * 1%), 
      calc((var(--end-y) - var(--start-y)) * 1%)
    ) rotate(var(--angle));
  }
}

.main-content {
  position: relative;
  z-index: 10;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  margin-bottom: 40px;
  padding-left: 0;
  padding-right: 0;
}

.nav-brand {
  display: flex;
  align-items: center;
  margin-left: -24px;
}

.nav-actions {
  display: flex;
  gap: 16px;
  margin-right: -24px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.brand-logo:hover {
  transform: scale(1.05);
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
}

.brand-text {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-actions {
  display: flex;
  gap: 16px;
}

.nav-btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.nav-btn.primary {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
}

.nav-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(59, 130, 246, 0.3);
}

.hero-section {
  padding: 60px 0 80px;
}

.hero-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
  align-items: center;
}

.hero-text {
  text-align: left;
}

.hero-title {
  font-size: 72px;
  font-weight: 800;
  line-height: 1.1;
  margin: 0 0 32px 0;
  animation: title-fade-in 1s ease forwards;
  opacity: 0;
  transform: translateY(30px);
}

@keyframes title-fade-in {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.gradient-text {
  background: linear-gradient(135deg, #ffffff, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 24px;
  color: #94a3b8;
  line-height: 1.6;
  margin: 0 0 60px 0;
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;
  animation: subtitle-fade-in 1s ease 0.3s forwards;
  opacity: 0;
  transform: translateY(20px);
}

@keyframes subtitle-fade-in {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-features {
  display: flex;
  gap: 24px;
  margin-bottom: 48px;
  justify-content: center;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #cbd5e1;
  font-weight: 500;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  animation: slide-in-left 0.6s ease forwards;
  opacity: 0;
  transform: translateX(-30px);
}

.feature-hover {
  background: rgba(59, 130, 246, 0.1);
  transform: translateX(0) scale(1.05);
}

@keyframes slide-in-left {
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.feature-icon {
  font-size: 20px;
  transition: all 0.3s ease;
}

.icon-spin {
  animation: icon-spin 1s linear infinite;
}

@keyframes icon-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.hero-actions {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.btn-primary, .btn-secondary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 32px;
  border-radius: 12px;
  border: none;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.btn-primary {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(59, 130, 246, 0.3);
}

.btn-glow {
  box-shadow: 0 0 30px rgba(59, 130, 246, 0.5);
}

.btn-secondary {
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.btn-secondary:hover {
  background: rgba(148, 163, 184, 0.2);
  border-color: rgba(148, 163, 184, 0.3);
}

.btn-arrow, .play-icon {
  font-size: 18px;
  transition: transform 0.3s ease;
}

.arrow-bounce {
  animation: arrow-bounce 0.6s ease infinite;
}

@keyframes arrow-bounce {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(4px); }
}

.play-bounce {
  animation: play-bounce 0.6s ease infinite;
}

@keyframes play-bounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.btn-pulse {
  animation: btn-pulse 1s infinite;
}

@keyframes btn-pulse {
  0% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.7); }
  70% { box-shadow: 0 0 0 10px rgba(59, 130, 246, 0); }
  100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0); }
}

/* 英雄区域可视化 */
.hero-visual {
  display: flex;
  justify-content: center;
  align-items: center;
}

.dashboard-preview {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 20px;
  padding: 48px;
  width: 100%;
  max-width: 750px;
  backdrop-filter: blur(20px);
  transition: all 0.3s ease;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.preview-dots {
  display: flex;
  gap: 6px;
}

.preview-dots span {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #64748b;
  transition: all 0.3s ease;
}

.preview-dots span:nth-child(1) { background: #ef4444; }
.preview-dots span:nth-child(2) { background: #f59e0b; }
.preview-dots span:nth-child(3) { background: #10b981; }

.dot-active {
  transform: scale(1.2);
  box-shadow: 0 0 10px currentColor;
}

.preview-title {
  color: #94a3b8;
  font-size: 18px;
  font-weight: 600;
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.preview-chart {
  position: relative;
  height: 225px;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 12px;
  padding: 36px;
}

.chart-line {
  position: absolute;
  top: 50%;
  left: 20px;
  right: 20px;
  height: 2px;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 1px;
  transition: all 0.3s ease;
}

.line-animate {
  background: linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899);
  background-size: 200% 100%;
  animation: line-flow 2s linear infinite;
}

@keyframes line-flow {
  0% { background-position: 0% 0%; }
  100% { background-position: 200% 0%; }
}

.chart-points {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.point {
  width: 8px;
  height: 8px;
  background: #3b82f6;
  border-radius: 50%;
  border: 2px solid rgba(15, 23, 42, 0.8);
  transition: all 0.3s ease;
}

.point-pulse {
  animation: point-pulse 1s ease infinite;
  background: #ec4899;
}

@keyframes point-pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.5); }
  100% { transform: scale(1); }
}

.preview-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.metric {
  text-align: center;
  padding: 30px;
  background: rgba(59, 130, 246, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(59, 130, 246, 0.1);
  position: relative;
  transition: all 0.3s ease;
}

.metric-update {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.3);
}

.metric-value {
  font-size: 36px;
  font-weight: 700;
  color: #3b82f6;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.value-flash {
  animation: value-flash 0.5s ease;
}

@keyframes value-flash {
  0%, 100% { color: #3b82f6; }
  50% { color: #ec4899; }
}

.metric-label {
  font-size: 18px;
  color: #64748b;
}

.metric-trend {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 12px;
  font-weight: bold;
}

.metric-trend.up {
  color: #10b981;
}

.metric-trend.down {
  color: #ef4444;
}

@media (max-width: 768px) {
  .main-content {
    padding: 0 16px;
  }

  .hero-section {
    padding: 60px 0 80px;
  }

  .hero-content {
    grid-template-columns: 1fr;
    gap: 40px;
    text-align: center;
  }

  .hero-text {
    text-align: center;
  }

  .hero-title {
    font-size: 36px;
  }

  .hero-features {
    flex-direction: column;
    gap: 20px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: center;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .section-header h2 {
    font-size: 36px;
  }

  .cta-content h2 {
    font-size: 36px;
  }

  .cta-actions {
    flex-direction: column;
    align-items: center;
  }

  .live-notifications {
    right: 10px;
    left: 10px;
    max-width: none;
  }
}

/* 功能特性区域 */
.features-section {
  padding: 80px 0;
}

.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-header h2 {
  font-size: 48px;
  font-weight: 700;
  color: white;
  margin: 0 0 24px 0;
}

.section-header p {
  font-size: 18px;
  color: #94a3b8;
  max-width: 600px;
  margin: 0 auto;
  line-height: 1.6;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 32px;
}

.feature-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 20px;
  padding: 32px;
  transition: all 0.3s ease;
  backdrop-filter: blur(20px);
  animation: card-fade-in 0.6s ease forwards;
  opacity: 0;
  transform: translateY(30px);
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-5px);
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
}

.card-hover {
  transform: translateY(-8px);
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

@keyframes card-fade-in {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.feature-card-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.feature-icon-wrapper {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(147, 51, 234, 0.2));
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  transition: all 0.3s ease;
}

.icon-glow {
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.3), rgba(147, 51, 234, 0.3));
}

.feature-card h3 {
  font-size: 24px;
  font-weight: 600;
  color: white;
  margin: 0;
}

.feature-card p {
  color: #94a3b8;
  line-height: 1.6;
  margin: 0 0 24px 0;
}

.feature-stats {
  display: flex;
  gap: 24px;
}

.stat {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #3b82f6;
  margin-bottom: 4px;
  transition: all 0.3s ease;
}

.stat-animate {
  animation: stat-bounce 0.6s ease;
}

@keyframes stat-bounce {
  0%, 20%, 53%, 80%, 100% { transform: translate3d(0,0,0); }
  40%, 43% { transform: translate3d(0,-10px,0); }
  70% { transform: translate3d(0,-5px,0); }
  90% { transform: translate3d(0,-2px,0); }
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

/* 底部CTA */
.cta-section {
  padding: 80px 0;
  text-align: center;
}

.cta-content h2 {
  font-size: 48px;
  font-weight: 700;
  color: white;
  margin: 0 0 24px 0;
}

.cta-content p {
  font-size: 18px;
  color: #94a3b8;
  max-width: 600px;
  margin: 0 auto 32px auto;
  line-height: 1.6;
}

.cta-actions {
  display: flex;
  gap: 24px;
  justify-content: center;
}

.btn-primary.large, .btn-outline.large {
  padding: 20px 40px;
  font-size: 18px;
  border-radius: 12px;
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-outline.large {
  background: transparent;
  color: #94a3b8;
  border: 2px solid rgba(148, 163, 184, 0.3);
}

.btn-outline.large:hover {
  border-color: rgba(59, 130, 246, 0.5);
  color: #3b82f6;
}

/* 实时通知 */
.live-notifications {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 350px;
}

.notification-item {
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 16px;
  backdrop-filter: blur(20px);
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: notification-slide-in 0.5s ease;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
}

@keyframes notification-slide-in {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.notification-item.success {
  border-left: 4px solid #10b981;
}

.notification-item.info {
  border-left: 4px solid #3b82f6;
}

.notification-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
}

.notification-title {
  font-weight: 600;
  color: white;
  margin-bottom: 4px;
}

.notification-message {
  color: #94a3b8;
  font-size: 14px;
}

.notification-time {
  color: #64748b;
  font-size: 12px;
  flex-shrink: 0;
}
</style>
