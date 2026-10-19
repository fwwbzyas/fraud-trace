<template>
  <div class="mode-switcher">
    <el-dropdown @command="handleCommand" trigger="click">
      <el-button :type="isTestMode ? 'warning' : 'primary'" circle>
        <el-icon>
          <Tools v-if="isTestMode" />
          <Connection v-else />
        </el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item divided>
            <div class="mode-info">
              <div class="current-mode">
                <el-tag :type="isTestMode ? 'warning' : 'success'" size="small">
                  {{ getCurrentModeText() }}
                </el-tag>
              </div>
              <div class="mode-desc">
                {{ isTestMode ? '使用虚拟数据，所有功能可用' : '连接真实后端服务' }}
              </div>
            </div>
          </el-dropdown-item>
          
          <el-dropdown-item command="switch" :disabled="switching">
            <el-icon><Refresh /></el-icon>
            {{ switching ? '切换中...' : '切换模式' }}
          </el-dropdown-item>
          
          <el-dropdown-item command="reset" v-if="isTestMode">
            <el-icon><RefreshLeft /></el-icon>
            重置测试数据
          </el-dropdown-item>
          
          <el-dropdown-item divided command="help">
            <el-icon><QuestionFilled /></el-icon>
            使用说明
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
    
    <!-- 模式指示器 -->
    <div class="mode-indicator" v-if="showIndicator">
      <el-tag 
        :type="isTestMode ? 'warning' : 'success'" 
        size="small"
        effect="dark"
      >
        {{ getCurrentModeText() }}
      </el-tag>
    </div>

    <!-- 帮助对话框 -->
    <el-dialog
      v-model="helpDialogVisible"
      title="模式说明"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="help-content">
        <el-alert
          title="提示"
          type="info"
          description="您可以随时在正常模式和测试模式之间切换"
          :closable="false"
          style="margin-bottom: 20px;"
        />
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card class="mode-card normal-mode">
              <template #header>
                <div class="card-header">
                  <el-icon><Connection /></el-icon>
                  <span>正常模式</span>
                </div>
              </template>
              <ul class="feature-list">
                <li>✅ 连接真实后端服务</li>
                <li>✅ 使用真实数据库数据</li>
                <li>✅ 完整的数据持久化</li>
                <li>❌ 需要后端服务运行</li>
                <li>❌ 可能有网络延迟</li>
              </ul>
            </el-card>
          </el-col>
          
          <el-col :span="12">
            <el-card class="mode-card test-mode">
              <template #header>
                <div class="card-header">
                  <el-icon><Tools /></el-icon>
                  <span>测试模式</span>
                </div>
              </template>
              <ul class="feature-list">
                <li>✅ 无需后端服务</li>
                <li>✅ 使用虚拟数据</li>
                <li>✅ 所有功能可用</li>
                <li>✅ 响应速度快</li>
                <li>❌ 数据不会持久化</li>
              </ul>
            </el-card>
          </el-col>
        </el-row>
        
        <el-divider />
        
        <div class="tips-section">
          <h4>使用建议：</h4>
          <ul class="tips-list">
            <li><strong>开发调试：</strong> 推荐使用测试模式，无需启动后端服务</li>
            <li><strong>功能演示：</strong> 测试模式提供完整的功能体验</li>
            <li><strong>生产使用：</strong> 使用正常模式连接真实数据</li>
            <li><strong>数据重置：</strong> 测试模式下可以重置数据到初始状态</li>
          </ul>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="helpDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="switchToTestMode" v-if="!isTestMode">
          切换到测试模式
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Tools, Connection, Refresh, RefreshLeft, QuestionFilled
} from '@element-plus/icons-vue'
import { isTestMode, toggleTestMode, getCurrentModeText } from '@/config/testMode'
import { resetMockData } from '@/api/mockServices'

// 组件参数
defineProps({
  showIndicator: {
    type: Boolean,
    default: true
  }
})

// 响应式数据
const switching = ref(false)
const helpDialogVisible = ref(false)

// 方法
const handleCommand = (command) => {
  switch (command) {
    case 'switch':
      handleModeSwitch()
      break
    case 'reset':
      handleResetData()
      break
    case 'help':
      helpDialogVisible.value = true
      break
  }
}

const handleModeSwitch = async () => {
  try {
    const targetMode = isTestMode.value ? '正常模式' : '测试模式'
    
    await ElMessageBox.confirm(
      `确定要切换到${targetMode}吗？\n\n${
        isTestMode.value 
          ? '切换到正常模式将连接真实后端服务' 
          : '切换到测试模式将使用虚拟数据'
      }`,
      '确认切换',
      {
        confirmButtonText: '确定切换',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    switching.value = true
    ElMessage.info(`正在切换到${targetMode}...`)
    
    // 延迟一下再切换，给用户反馈
    setTimeout(() => {
      toggleTestMode()
    }, 500)
    
  } catch {
    // 用户取消
  }
}

const handleResetData = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重置所有测试数据吗？\n\n这将清除所有当前的虚拟数据并重新生成',
      '确认重置',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    resetMockData()
    ElMessage.success('测试数据已重置')
    
    // 刷新页面以重新加载数据
    setTimeout(() => {
      window.location.reload()
    }, 1000)
    
  } catch {
    // 用户取消
  }
}

const switchToTestMode = () => {
  helpDialogVisible.value = false
  handleModeSwitch()
}
</script>

<style scoped>
.mode-switcher {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.mode-indicator {
  margin-left: 8px;
}

.mode-info {
  padding: 8px 0;
  min-width: 180px;
}

.current-mode {
  margin-bottom: 4px;
}

.mode-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.help-content {
  padding: 10px 0;
}

.mode-card {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.normal-mode .card-header {
  color: #67c23a;
}

.test-mode .card-header {
  color: #e6a23c;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  padding: 4px 0;
  font-size: 14px;
  line-height: 1.5;
}

.tips-section {
  margin-top: 20px;
}

.tips-section h4 {
  margin: 0 0 10px 0;
  color: #303133;
}

.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-list li {
  padding: 6px 0;
  font-size: 14px;
  line-height: 1.5;
  color: #606266;
}

.tips-list li strong {
  color: #303133;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .help-content .el-row {
    flex-direction: column;
  }
  
  .help-content .el-col {
    margin-bottom: 16px;
  }
}
</style>
