<template>
  <div class="task-results">
    <div class="top-back-bar">
      <el-button type="primary" :icon="ArrowLeft" @click="goBack">
        返回任务管理
      </el-button>
      <el-text type="info" class="back-tip">回到任务列表以选择其他任务</el-text>
    </div>
    <!-- 面包屑导航 -->
    <el-breadcrumb class="breadcrumb-nav" separator="/">
      <el-breadcrumb-item :to="{ path: '/tasks' }">任务管理</el-breadcrumb-item>
      <el-breadcrumb-item>{{ taskResult?.taskName || '任务结果' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 页面头部 -->
    <div class="page-header">
      <el-row justify="space-between" align="middle">
        <el-col :span="18">
          <div class="header-info">
            <el-button
              type="primary"
              :icon="ArrowLeft"
              @click="goBack"
              class="back-btn"
            >
              返回
            </el-button>
            <div class="title-group">
              <h2 class="page-title">
                <el-icon style="margin-right: 8px"><DataAnalysis /></el-icon>
                {{ taskResult?.taskName || '任务结果' }}
              </h2>
              <p class="page-subtitle">
                任务代码：{{ taskResult?.taskCode }} |
                分析范围：{{ taskResult?.timeRange }}天 |
                状态：<el-tag type="success">已完成</el-tag><br />
                开始时间：{{ formatDateTime(taskResult?.startTime) }} |
                结束时间：{{ formatDateTime(taskResult?.endTime) }} |
                耗时：{{ calculateDuration(taskResult?.startTime, taskResult?.endTime) }}
              </p>
            </div>
          </div>
        </el-col>
        <el-col :span="6" style="text-align: right">
          <el-button type="primary" :icon="Download" @click="exportResults">
            导出结果
          </el-button>
          <el-button :icon="Refresh" @click="loadTaskResults">
            刷新
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="taskResult">
      <!-- 统计概览 -->
      <el-row :gutter="20" class="stats-overview">
        <el-col :span="6">
          <el-card class="stats-card total-card">
            <div class="stats-content">
              <div class="stats-number">{{ taskResult.statistics?.totalRiskAccounts || 0 }}</div>
              <div class="stats-label">总风险账户</div>
            </div>
            <el-icon class="stats-icon"><User /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card victim-card">
            <div class="stats-content">
              <div class="stats-number">{{ taskResult.statistics?.victimAccounts || 0 }}</div>
              <div class="stats-label">受害人账户</div>
            </div>
            <el-icon class="stats-icon"><UserFilled /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card suspicious-card">
            <div class="stats-content">
              <div class="stats-number">{{ taskResult.statistics?.suspiciousAccounts || 0 }}</div>
              <div class="stats-label">可疑账户</div>
            </div>
            <el-icon class="stats-icon"><Warning /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card rate-card">
            <div class="stats-content">
              <div class="stats-number">{{ riskRatio }}%</div>
              <div class="stats-label">风险占比</div>
            </div>
            <el-icon class="stats-icon"><TrendCharts /></el-icon>
          </el-card>
        </el-col>
      </el-row>

      <!-- 视图切换器 -->
      <div class="view-switcher">
        <el-radio-group v-model="currentView" @change="handleViewChange">
          <el-radio-button label="combined">综合视图</el-radio-button>
          <el-radio-button label="normal">正常账户视图</el-radio-button>
          <el-radio-button label="victim">受害人视图</el-radio-button>
          <el-radio-button label="suspect">可疑分子视图</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 风险账户列表 -->
      <el-card class="accounts-table-card">
        <template #header>
          <div class="table-header">
            <h3>{{ getTableTitle() }} (共 {{ currentViewTotal }} 条)</h3>
            <div class="table-controls">
              <el-text type="info">使用下方分页器浏览数据</el-text>
            </div>
          </div>
        </template>

        <el-table
          :data="filteredAccounts"
          stripe
          border
          :default-sort="{ prop: 'accountRole', order: 'ascending' }"
          style="width: 100%"
          @row-click="showAccountDetail"
        >
          <el-table-column prop="accountNo" label="账户号码" width="150" fixed="left">
            <template #default="{ row }">
              <el-button type="primary" link @click="showAccountDetail(row)">
                {{ row.accountNo }}
              </el-button>
            </template>
          </el-table-column>

          <el-table-column prop="holderName" label="持有人" width="120">
            <template #default="{ row }">
              {{ maskName(row.holderName) }}
            </template>
          </el-table-column>


          <el-table-column prop="accountRole" label="账户角色" width="160">
            <template #default="{ row }">
              <div class="account-role-container">
                <el-tag
                  :type="Number(row.accountRole) === 1 ? 'warning' : 'danger'"
                  effect="light"
                  :class="Number(row.accountRole) === 1 ? 'victim-role-tag' : 'suspect-role-tag'"
                >
                  <el-icon v-if="Number(row.accountRole) === 1" class="role-icon"><User /></el-icon>
                  <el-icon v-else class="role-icon"><Warning /></el-icon>
                  {{ row.accountRoleText }}
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="matchedRules" label="匹配规则" min-width="200">
            <template #default="{ row }">
              <el-text class="rule-text" truncated>{{ row.matchedRules }}</el-text>
            </template>
          </el-table-column>

          <el-table-column prop="matchedIndicators" label="实际指标值" width="150">
            <template #default="{ row }">
              <el-popover placement="top" :width="350" trigger="hover">
                <template #reference>
                  <el-button type="info" size="small" plain>
                    查看详情
                  </el-button>
                </template>
                <div class="indicators-detail">
                  <div class="indicator-header">
                    <el-alert
                      title="说明：显示账户的实际指标值，该账户已满足匹配规则"
                      type="info"
                      :closable="false"
                      style="margin-bottom: 12px;"
                    />
                  </div>
                  <div
                    v-for="(value, key) in row.matchedIndicators"
                    :key="key"
                    class="indicator-item"
                  >
                    <span class="indicator-key">{{ getIndicatorName(key) }}:</span>
                    <span class="indicator-value">{{ formatIndicatorValue(key, value) }}</span>
                    <span class="indicator-unit">{{ getIndicatorUnit(key) }}</span>
                  </div>
                  <div class="rule-match-status">
                    <el-divider style="margin: 12px 0;"/>
                    <div class="match-info">
                      <el-icon color="#67c23a"><SuccessFilled /></el-icon>
                      <span style="color: #67c23a; margin-left: 4px;">此账户满足匹配规则</span>
                    </div>
                  </div>
                </div>
              </el-popover>
            </template>
          </el-table-column>

          <el-table-column prop="createTime" label="发现时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                :icon="View"
                @click="showAccountDetail(row)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[20, 50, 100, 200, 500]"
            :total="currentViewTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>

      <!-- 可视化分析 -->
      <el-row :gutter="20" class="charts-section">
        <!-- 左侧：账户分类分布 + 柱状图对比 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <div class="chart-header-row">
                  <span class="chart-title">账户分类分布</span>
                  <el-icon class="chart-icon"><PieChart /></el-icon>
                </div>
                <span class="chart-subtitle">基于本次任务分析结果</span>
              </div>
            </template>
            <div class="chart-content-wrapper">
              <div class="chart-content-grid">
                <div class="chart-ring-block">
                  <div class="chart-ring-header" v-if="taskResult">
                    <span class="chart-ring-label">总账户数</span>
                    <span class="chart-ring-value">{{ taskResult.statistics?.totalAccounts || 0 }}</span>
                  </div>
                  <div ref="roleChartRef" class="chart-container-pie"></div>
                  <div class="chart-ring-legend" v-if="taskResult">
                    <div class="legend-item normal">
                      <span>正常账户</span>
                      <span>{{ normalAccountPercent }}%</span>
                    </div>
                    <div class="legend-item warning">
                      <span>受害人账户</span>
                      <span>{{ victimAccountPercent }}%</span>
                    </div>
                    <div class="legend-item danger">
                      <span>可疑账户</span>
                      <span>{{ suspectAccountPercent }}%</span>
                    </div>
                  </div>
                </div>
                <!-- 统计信息卡片 -->
                <div class="stats-info-grid" v-if="taskResult">
                  <div class="stat-info-item total-stat">
                    <div class="stat-info-icon">
                      <el-icon><Collection /></el-icon>
                    </div>
                    <div class="stat-info-content">
                      <div class="stat-info-label">总账户</div>
                      <div class="stat-info-value">{{ taskResult.statistics?.totalAccounts || 0 }}</div>
                      <div class="stat-info-percent">100%</div>
                    </div>
                  </div>
                  <div class="stat-info-item normal-stat">
                    <div class="stat-info-icon">
                      <el-icon><CircleCheck /></el-icon>
                    </div>
                    <div class="stat-info-content">
                      <div class="stat-info-label">正常账户</div>
                      <div class="stat-info-value">{{ Math.max(0, (taskResult.statistics?.totalAccounts || 0) - (taskResult.statistics?.totalRiskAccounts || 0)) }}</div>
                      <div class="stat-info-percent">{{ normalAccountPercent }}%</div>
                    </div>
                  </div>
                  <div class="stat-info-item victim-stat">
                    <div class="stat-info-icon">
                      <el-icon><UserFilled /></el-icon>
                    </div>
                    <div class="stat-info-content">
                      <div class="stat-info-label">受害人账户</div>
                      <div class="stat-info-value">{{ taskResult.statistics?.victimAccounts || 0 }}</div>
                      <div class="stat-info-percent">{{ victimAccountPercent }}%</div>
                    </div>
                  </div>
                  <div class="stat-info-item suspect-stat">
                    <div class="stat-info-icon">
                      <el-icon><Warning /></el-icon>
                    </div>
                    <div class="stat-info-content">
                      <div class="stat-info-label">可疑账户</div>
                      <div class="stat-info-value">{{ taskResult.statistics?.suspiciousAccounts || 0 }}</div>
                      <div class="stat-info-percent">{{ suspectAccountPercent }}%</div>
                    </div>
                  </div>
                  <div class="stat-info-item risk-stat">
                    <div class="stat-info-icon">
                      <el-icon><TrendCharts /></el-icon>
                    </div>
                    <div class="stat-info-content">
                      <div class="stat-info-label">风险占比</div>
                      <div class="stat-info-value">{{ taskResult.statistics?.totalRiskAccounts || 0 }}</div>
                      <div class="stat-info-percent">{{ riskRatio }}%</div>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 柱状图对比 -->
              <div class="chart-bar-panel">
                <div class="chart-bar-header">
                  <div class="chart-bar-title">
                    <el-icon><DataLine /></el-icon>
                    <span>分类趋势对比</span>
                  </div>
                  <el-tag size="small" type="info" effect="light">最新</el-tag>
                </div>
                <div ref="barChartRef" class="chart-container-bar"></div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：风险占比仪表盘 + 详细信息 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <div class="chart-header-row">
                  <span class="chart-title">风险占比分析</span>
                  <el-icon class="chart-icon"><Odometer /></el-icon>
                </div>
                <span class="chart-subtitle">风险账户占总账户比例</span>
              </div>
            </template>
            <div class="gauge-content-wrapper">
              <div class="gauge-flex">
                <div class="gauge-panel">
                  <div class="gauge-panel-badge">
                    <span class="gauge-panel-label">当前风险占比</span>
                    <span class="gauge-panel-value">{{ riskRatio }}%</span>
                  </div>
                  <div ref="gaugeChartRef" class="chart-container-gauge"></div>
                </div>
                <!-- 风险等级指示 -->
                <div class="risk-detail-panel" v-if="taskResult">
                  <div class="risk-level-card" :class="riskLevelClass">
                    <div class="risk-level-icon">
                      <el-icon v-if="parseFloat(riskRatio) >= 10"><WarningFilled /></el-icon>
                      <el-icon v-else-if="parseFloat(riskRatio) >= 5"><InfoFilled /></el-icon>
                      <el-icon v-else><SuccessFilled /></el-icon>
                    </div>
                    <div class="risk-level-content">
                      <div class="risk-level-title">{{ riskLevelText }}</div>
                      <div class="risk-level-desc">{{ riskLevelDesc }}</div>
                    </div>
                  </div>
                <div class="risk-stats-grid">
                    <div class="risk-stat-item">
                      <div class="risk-stat-icon">
                        <el-icon><User /></el-icon>
                      </div>
                      <div class="risk-stat-content">
                        <div class="risk-stat-label">总账户数</div>
                      <div class="risk-stat-value">{{ taskResult.statistics?.totalAccounts || 0 }}</div>
                      </div>
                    </div>
                    <div class="risk-stat-item">
                      <div class="risk-stat-icon danger">
                        <el-icon><Warning /></el-icon>
                      </div>
                      <div class="risk-stat-content">
                        <div class="risk-stat-label">风险账户</div>
                        <div class="risk-stat-value danger">{{ taskResult.statistics?.totalRiskAccounts || 0 }}</div>
                      </div>
                    </div>
                    <div class="risk-stat-item">
                      <div class="risk-stat-icon success">
                        <el-icon><CircleCheck /></el-icon>
                      </div>
                      <div class="risk-stat-content">
                        <div class="risk-stat-label">正常账户</div>
                      <div class="risk-stat-value success">{{ Math.max(0, (taskResult.statistics?.totalAccounts || 0) - (taskResult.statistics?.totalRiskAccounts || 0)) }}</div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="risk-meta-grid">
                  <div class="risk-threshold-card">
                    <div class="risk-insight-header">
                      <el-icon><Document /></el-icon>
                      <span>阈值参考</span>
                    </div>
                    <div class="risk-threshold-status">
                      <span class="status-label">{{ riskThresholdInfo.status }}</span>
                      <span class="status-value">{{ riskThresholdInfo.ratio }}%</span>
                    </div>
                    <div class="risk-threshold-scale">
                      <div class="risk-threshold-segment low">0-5%</div>
                      <div class="risk-threshold-segment mid">5-10%</div>
                      <div class="risk-threshold-segment high">10%+</div>
                      <div class="risk-threshold-marker" :style="{ left: riskThresholdInfo.marker + '%' }">
                        <span>{{ riskThresholdInfo.ratio }}%</span>
                      </div>
                    </div>
                    <div class="risk-threshold-desc">{{ riskThresholdInfo.desc }}</div>
                  </div>
                  <div class="risk-ops-card">
                    <div class="risk-insight-header">
                      <el-icon><Operation /></el-icon>
                      <span>处置建议</span>
                    </div>
                    <div
                      class="risk-ops-item"
                      v-for="(action, index) in riskActionItems"
                      :key="index"
                    >
                      <div class="risk-ops-badge">{{ index + 1 }}</div>
                      <div class="risk-ops-content">
                        <div class="risk-ops-title">{{ action.title }}</div>
                        <div class="risk-ops-desc">{{ action.desc }}</div>
                      </div>
                      <el-button type="primary" link size="small">{{ action.cta }}</el-button>
                    </div>
                  </div>
                </div>
                <div class="risk-insight-grid">
                  <div class="risk-insight-card">
                    <div class="risk-insight-header">
                      <el-icon><TrendCharts /></el-icon>
                      <span>占比结构概览</span>
                    </div>
                    <div
                      class="risk-insight-row"
                      v-for="item in riskStructureData"
                      :key="item.label"
                    >
                      <div class="risk-insight-label">
                        <span class="risk-dot" :class="item.type"></span>
                        {{ item.label }}
                      </div>
                      <div class="risk-insight-bar">
                        <div
                          class="risk-insight-bar-fill"
                          :class="item.type"
                          :style="{ width: item.percent + '%' }"
                        ></div>
                      </div>
                      <div class="risk-insight-value">
                        {{ item.count }}个 ({{ item.percent }}%)
                      </div>
                    </div>
                  </div>
                  <div class="risk-alert-card">
                    <div class="risk-insight-header">
                      <el-icon><InfoFilled /></el-icon>
                      <span>风险提示</span>
                    </div>
                    <div
                      class="risk-alert-item"
                      v-for="(alert, index) in riskIndicatorHighlights"
                      :key="index"
                    >
                      <span class="risk-alert-tag" :class="alert.level">{{ alert.levelText }}</span>
                      <span class="risk-alert-text">{{ alert.text }}</span>
                    </div>
                    <div v-if="!riskIndicatorHighlights.length" class="risk-alert-empty">
                      当前风险指标平稳，无需额外动作。
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 智能分析 -->
      <el-card class="intelligence-analysis-card" v-if="taskResult">
        <template #header>
          <div class="analysis-header">
            <div class="analysis-title">
              <el-icon class="analysis-icon"><TrendCharts /></el-icon>
              <span>数据分析报告</span>
            </div>
            <el-button
              type="primary"
              size="small"
              :icon="Refresh"
              @click="generateAnalysis"
              :loading="analyzing"
            >
              刷新报告
            </el-button>
          </div>
        </template>

        <div v-if="analysisResult" class="analysis-content">
          <!-- 风险概览 -->
          <div class="analysis-section">
            <h4 class="section-title">
              <el-icon><Warning /></el-icon>
              风险概览
            </h4>
            <div class="risk-overview">
              <el-alert
                :title="analysisResult.riskLevel"
                :type="analysisResult.riskType"
                :closable="false"
                show-icon
              >
                <template #default>
                  <div class="risk-description">
                    {{ analysisResult.riskDescription }}
                  </div>
                </template>
              </el-alert>
            </div>
          </div>

          <!-- 犯罪模式识别 -->
          <div class="analysis-section">
            <h4 class="section-title">
              <el-icon><DataAnalysis /></el-icon>
              犯罪模式识别
            </h4>
            <div class="patterns-list">
              <div
                v-for="(pattern, index) in analysisResult.patterns"
                :key="index"
                class="pattern-item"
              >
                <div class="pattern-header">
                  <el-tag :type="pattern.severity === 'high' ? 'danger' : pattern.severity === 'medium' ? 'warning' : 'info'" size="small">
                    {{ pattern.severity === 'high' ? '高风险' : pattern.severity === 'medium' ? '中风险' : '低风险' }}
                  </el-tag>
                  <span class="pattern-name">{{ pattern.name }}</span>
                </div>
                <div class="pattern-description">{{ pattern.description }}</div>
                <div class="pattern-stats">
                  <span>涉及账户: {{ pattern.accountCount }}个</span>
                  <span v-if="pattern.percentage">占比: {{ pattern.percentage }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 关键发现 -->
          <div class="analysis-section">
            <h4 class="section-title">
              <el-icon><SuccessFilled /></el-icon>
              关键发现
            </h4>
            <ul class="findings-list">
              <li v-for="(finding, index) in analysisResult.findings" :key="index" class="finding-item">
                {{ finding }}
              </li>
            </ul>
          </div>

          <!-- 调查建议 -->
          <div class="analysis-section">
            <h4 class="section-title">
              <el-icon><View /></el-icon>
              调查建议
            </h4>
            <div class="recommendations">
              <div
                v-for="(recommendation, index) in analysisResult.recommendations"
                :key="index"
                class="recommendation-item"
              >
                <div class="recommendation-number">{{ index + 1 }}</div>
                <div class="recommendation-content">
                  <div class="recommendation-title">{{ recommendation.title }}</div>
                  <div class="recommendation-desc">{{ recommendation.description }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="analyzing" class="analysis-loading">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else class="analysis-empty">
          <el-empty description="点击重新分析按钮开始智能分析" :image-size="100" />
        </div>
      </el-card>
    </div>

    <!-- 账户详情对话框 -->
    <el-dialog
      v-model="accountDetailVisible"
      title="账户详细信息"
      width="800px"
      :close-on-click-modal="false"
      class="account-detail-dialog"
    >
      <div v-if="selectedAccount" class="account-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="账户号码">
            {{ selectedAccount.accountNo }}
          </el-descriptions-item>
          <el-descriptions-item label="持有人姓名">
            {{ maskName(selectedAccount.holderName) }}
          </el-descriptions-item>
          <el-descriptions-item label="身份证号">
            {{ maskIdCard(selectedAccount.idCardNo) }}
          </el-descriptions-item>
          <el-descriptions-item label="账户类型">
            {{ selectedAccount.accountType === 1 ? '个人账户' : '企业账户' }}
          </el-descriptions-item>
          <el-descriptions-item label="账户状态">
            <el-tag
              :type="getAccountStatusTagType(selectedAccount.accountStatus)"
              :class="Number(selectedAccount.accountRole) === 1 ? 'victim-status-tag' : 'suspect-status-tag'"
            >
              {{ getAccountStatusText(selectedAccount.accountStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="账户角色">
            <el-tag
              :type="Number(selectedAccount.accountRole) === 1 ? 'warning' : 'danger'"
              :class="Number(selectedAccount.accountRole) === 1 ? 'victim-role-tag' : 'suspect-role-tag'"
            >
              <el-icon v-if="Number(selectedAccount.accountRole) === 1" class="role-icon"><User /></el-icon>
              <el-icon v-else class="role-icon"><Warning /></el-icon>
              {{ selectedAccount.accountRoleText }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="匹配规则" :span="2">
            {{ selectedAccount.matchedRules }}
          </el-descriptions-item>
          <el-descriptions-item label="实际指标详情" :span="2">
            <el-alert
              title="说明：以下显示该账户的实际指标值（非过滤条件），此账户已满足设定的匹配规则"
              type="info"
              :closable="false"
              style="margin-bottom: 16px;"
            />
            <div class="indicators-grid">
              <div
                v-for="(value, key) in selectedAccount.matchedIndicators"
                :key="key"
                class="indicator-card"
              >
                <div class="indicator-name">{{ getIndicatorName(key) }}</div>
                <div class="indicator-value">{{ formatIndicatorValue(key, value) }}</div>
                <div class="indicator-unit">{{ getIndicatorUnit(key) }}</div>
              </div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="发现时间" :span="2">
            {{ formatDateTime(selectedAccount.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button @click="accountDetailVisible = false" size="default">关闭</el-button>
        <el-button type="primary" @click="exportAccountDetail" size="default">导出详情</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, DataAnalysis, Download, Refresh, Search, View,
  User, UserFilled, Warning, CircleCloseFilled, SuccessFilled, TrendCharts,
  PieChart, Odometer, CircleCheck, WarningFilled, InfoFilled,
  Collection, DataLine, Document, Operation
} from '@element-plus/icons-vue'
import { taskApi } from '../api/task'
import { maskName, maskIdCard } from '../utils/nameMasking'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const taskResult = ref(null)
const searchKeyword = ref('')
const filterAccountRole = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const accountDetailVisible = ref(false)
const selectedAccount = ref(null)
const currentView = ref('combined') // 'combined', 'victim', 'suspect'

// 图表引用
const roleChartRef = ref(null)
const barChartRef = ref(null)
const gaugeChartRef = ref(null)
let roleChart = null
let barChart = null
let gaugeChart = null

// 智能分析相关
const analyzing = ref(false)
const analysisResult = ref(null)

// 计算属性 - 后端已经按角色过滤，直接显示
const filteredAccounts = computed(() => {
  if (!taskResult.value?.riskAccounts) return []
  // 后端已经按accountRole过滤了，直接返回
  return taskResult.value.riskAccounts
})

// 当前视图的总数
const currentViewTotal = computed(() => {
  if (!taskResult.value?.statistics) return 0

  if (currentView.value === 'normal') {
    const totalAccounts = taskResult.value.statistics.totalAccounts || 0
    const totalRisk = taskResult.value.statistics.totalRiskAccounts || 0
    return Math.max(0, totalAccounts - totalRisk)
  }
  if (currentView.value === 'victim') {
    return taskResult.value.statistics.victimAccounts || 0
  } else if (currentView.value === 'suspect') {
    return taskResult.value.statistics.suspiciousAccounts || 0
  }

  return taskResult.value.resultCount || 0
})

// 风险占比计算
const riskRatio = computed(() => {
  if (!taskResult.value?.statistics) return 0
  const riskAccounts = taskResult.value.statistics.totalRiskAccounts || 0
  const totalAccounts = taskResult.value.statistics.totalAccounts || 1
  return riskAccounts > 0 ? ((riskAccounts / totalAccounts) * 100).toFixed(1) : 0
})

// 账户分类百分比计算
const normalAccountPercent = computed(() => {
  if (!taskResult.value?.statistics) return 0
  const totalAccounts = taskResult.value.statistics.totalAccounts || 0
  const totalRisk = taskResult.value.statistics.totalRiskAccounts || 0
  const normal = Math.max(0, totalAccounts - totalRisk)
  return totalAccounts > 0 ? ((normal / totalAccounts) * 100).toFixed(2) : 0
})

const victimAccountPercent = computed(() => {
  if (!taskResult.value?.statistics) return 0
  const totalAccounts = taskResult.value.statistics.totalAccounts || 0
  const victim = taskResult.value.statistics.victimAccounts || 0
  return totalAccounts > 0 ? ((victim / totalAccounts) * 100).toFixed(2) : 0
})

const suspectAccountPercent = computed(() => {
  if (!taskResult.value?.statistics) return 0
  const totalAccounts = taskResult.value.statistics.totalAccounts || 0
  const suspect = taskResult.value.statistics.suspiciousAccounts || 0
  return totalAccounts > 0 ? ((suspect / totalAccounts) * 100).toFixed(2) : 0
})

const riskStructureData = computed(() => {
  if (!taskResult.value?.statistics) return []
  const totalAccounts = taskResult.value.statistics.totalAccounts || 0
  if (totalAccounts <= 0) return []
  const stats = taskResult.value.statistics
  const totalRisk = stats.totalRiskAccounts || 0
  const normal = Math.max(0, totalAccounts - totalRisk)
  const formatPercent = (value) => Number(((value / totalAccounts) * 100).toFixed(2))
  return [
    {
      label: '正常账户',
      type: 'normal',
      count: normal,
      percent: formatPercent(normal)
    },
    {
      label: '受害人账户',
      type: 'victim',
      count: stats.victimAccounts || 0,
      percent: formatPercent(stats.victimAccounts || 0)
    },
    {
      label: '可疑账户',
      type: 'suspect',
      count: stats.suspiciousAccounts || 0,
      percent: formatPercent(stats.suspiciousAccounts || 0)
    }
  ]
})

const riskIndicatorHighlights = computed(() => {
  if (!taskResult.value?.statistics) return []
  const highlights = []
  const ratio = parseFloat(riskRatio.value) || 0
  const victimPercent = parseFloat(victimAccountPercent.value) || 0
  const suspectPercent = parseFloat(suspectAccountPercent.value) || 0

  if (ratio >= 10) {
    highlights.push({ level: 'danger', levelText: '高', text: `风险账户占比${ratio}% ，需要立即处置。` })
  } else if (ratio >= 5) {
    highlights.push({ level: 'warning', levelText: '中', text: `风险占比${ratio}% ，需持续跟踪。` })
  } else {
    highlights.push({ level: 'safe', levelText: '低', text: `风险占比${ratio}% ，处于安全区间。` })
  }

  if (victimPercent >= 3) {
    highlights.push({ level: 'warning', levelText: '受', text: `受害人占比${victimPercent}% ，疑似批量受害。` })
  }
  if (suspectPercent >= 3) {
    highlights.push({ level: 'danger', levelText: '疑', text: `可疑账户占比${suspectPercent}% ，可能存在团伙。` })
  } else if (suspectPercent < 1) {
    highlights.push({ level: 'safe', levelText: '稳', text: '可疑账户占比较低，迹象有限。' })
  }

  return highlights.slice(0, 3)
})

const riskThresholdInfo = computed(() => {
  const ratio = parseFloat(riskRatio.value) || 0
  if (ratio >= 10) {
    return {
      ratio: ratio.toFixed(1),
      status: '高风险',
      desc: '风险占比超出10%，建议立即采取专项处置。',
      marker: Math.min(ratio, 100)
    }
  } else if (ratio >= 5) {
    return {
      ratio: ratio.toFixed(1),
      status: '中风险',
      desc: '风险占比接近上限，需要持续关注并抽查账户。',
      marker: Math.min(ratio, 100)
    }
  }
  return {
    ratio: ratio.toFixed(1),
    status: '低风险',
    desc: '风险占比处于安全区间，可保持常规巡检频率。',
    marker: Math.min(ratio, 100)
  }
})

const riskActionItems = computed(() => {
  if (!taskResult.value) return []
  const ratio = parseFloat(riskRatio.value) || 0
  const totalRisk = taskResult.value.statistics?.totalRiskAccounts || 0
  const victimCount = taskResult.value.statistics?.victimAccounts || 0
  const suspectCount = taskResult.value.statistics?.suspiciousAccounts || 0

  const actions = [
    {
      title: ratio >= 5 ? '安排专项复核' : '常规复核',
      desc: ratio >= 5
        ? `风险占比${ratio}% ，优先调度风控同事复核已识别的${totalRisk}个账户。`
        : `风险占比${ratio}% ，保持每周例行抽查，关注新出现的风险账户。`,
      cta: '查看任务'
    },
    {
      title: victimCount > suspectCount ? '受害人联动通知' : '可疑链路排查',
      desc: victimCount > suspectCount
        ? `受害人账户 ${victimCount} 个，建议联动客服完成劝阻及资金冻结。`
        : `可疑账户 ${suspectCount} 个，沿资金链追踪排查团伙活动。`,
      cta: victimCount > suspectCount ? '通知客服' : '追踪链路'
    },
    {
      title: ratio >= 8 ? '调整指标阈值' : '记录分析结果',
      desc: ratio >= 8
        ? '考虑提高关键指标阈值或引入模型辅助，提升识别能力。'
        : '记录本次分析结果，用于后续建模与策略复盘。',
      cta: ratio >= 8 ? '配置指标' : '导出报告'
    }
  ]

  return actions
})

// 风险等级相关计算
const riskLevelClass = computed(() => {
  const ratio = parseFloat(riskRatio.value)
  if (ratio >= 10) return 'risk-high'
  if (ratio >= 5) return 'risk-medium'
  return 'risk-low'
})

const riskLevelText = computed(() => {
  const ratio = parseFloat(riskRatio.value)
  if (ratio >= 10) return '高风险'
  if (ratio >= 5) return '中风险'
  return '低风险'
})

const riskLevelDesc = computed(() => {
  const ratio = parseFloat(riskRatio.value)
  if (ratio >= 10) return '风险账户占比超过10%，建议立即采取行动'
  if (ratio >= 5) return '风险账户占比在5%-10%之间，需要密切关注'
  return '风险账户占比低于5%，处于正常范围'
})

// 方法
const getTableTitle = () => {
  switch (currentView.value) {
    case 'normal':
      return '正常账户详情'
    case 'victim':
      return '受害人账户详情'
    case 'suspect':
      return '可疑分子账户详情'
    default:
      return '风险账户详情'
  }
}

const handleViewChange = (view) => {
  currentView.value = view
  currentPage.value = 1

  // 重新加载数据
  loadTaskResults()

  const viewNames = {
    combined: '综合视图',
    normal: '正常账户视图',
    victim: '受害人视图',
    suspect: '可疑分子视图'
  }
  ElMessage.success(`已切换到${viewNames[view]}`)
}
const goBack = () => {
  router.push('/tasks')
}

const loadTaskResults = async () => {
  const taskId = route.params.id
  if (!taskId) {
    ElMessage.error('任务ID不能为空')
    goBack()
    return
  }

  // 根据视图确定accountRole参数
  let accountRole = 'all'
  if (currentView.value === 'normal') {
    accountRole = 0
  } else if (currentView.value === 'victim') {
    accountRole = 1
  } else if (currentView.value === 'suspect') {
    accountRole = 2
  }

  loading.value = true
  try {
    const response = await taskApi.getTaskDetailedResults(taskId, currentPage.value, pageSize.value, accountRole)
    if (response.data.success) {
      taskResult.value = response.data.data
      console.log('任务结果加载成功，视图:', currentView.value, '角色:', accountRole, '当前页:', currentPage.value, '每页:', pageSize.value, '总页数:', response.data.data.totalPages)
      // 数据加载完成后初始化图表
      initCharts()
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    console.error('获取任务结果失败:', error)
    ElMessage.error('获取任务结果失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const showAccountDetail = (account) => {
  selectedAccount.value = account
  accountDetailVisible.value = true
}

const exportResults = () => {
  ElMessage.info('导出功能开发中...')
}

const exportAccountDetail = () => {
  ElMessage.info('导出功能开发中...')
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1 // 改变每页数量时重置到第1页
  loadTaskResults() // 重新加载数据
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadTaskResults() // 重新加载数据
}

// 工具函数

const getAccountStatusTagType = (status) => {
  switch (status) {
    case 0: return 'success'  // 正常
    case 1: return 'warning'  // 可疑
    default: return 'info'
  }
}

const getAccountStatusText = (status) => {
  switch (status) {
    case 0: return '正常'
    case 1: return '可疑'
    default: return '未知'
  }
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const calculateDuration = (startTime, endTime) => {
  if (!startTime || !endTime) return '-'

  const start = new Date(startTime)
  const end = new Date(endTime)
  const durationMs = end - start

  if (durationMs < 0) return '-'

  const seconds = Math.floor(durationMs / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)

  if (hours > 0) {
    return `${hours}小时${minutes % 60}分${seconds % 60}秒`
  } else if (minutes > 0) {
    return `${minutes}分${seconds % 60}秒`
  } else {
    return `${seconds}秒`
  }
}

// 指标相关的格式化方法
const getIndicatorName = (indicatorCode) => {
  const nameMap = {
    'INDICATOR_A': '短时大额转出',
    'INDICATOR_B': '短时高频转出',
    'INDICATOR_C': '销赃账户转出',
    'INDICATOR_D': '深夜转账',
    'INDICATOR_E': '异地转账',
    'INDICATOR_F': '团伙转账',
    'INDICATOR_N': '正常指标',
    'indicator_a': '短时大额转出',
    'indicator_b': '短时高频转出',
    'indicator_c': '销赃账户转出',
    'indicator_d': '深夜转账',
    'indicator_e': '异地转账',
    'indicator_f': '团伙转账',
    'indicator_n': '正常指标'
  }
  return nameMap[indicatorCode] || indicatorCode
}

const formatIndicatorValue = (indicatorCode, value) => {
  const code = indicatorCode.toLowerCase()
  // C指标（销赃账户转出）是金额类，其他都是计数类
  if (code.includes('_c') || code.includes('amount') || code.includes('income')) {
    // 金额类指标，格式化为货币
    return new Intl.NumberFormat('zh-CN', {
      style: 'currency',
      currency: 'CNY',
      minimumFractionDigits: 2
    }).format(value)
  } else {
    // 计数类指标，显示整数
    return Math.round(value).toString()
  }
}

const getIndicatorUnit = (indicatorCode) => {
  const code = indicatorCode.toLowerCase()
  // C指标（销赃账户转出）是金额类，其他都是计数类
  if (code.includes('_c') || code.includes('amount') || code.includes('income')) {
    return '' // 货币格式已包含单位
  } else {
    return '次'
  }
}

// 初始化图表
const initCharts = () => {
  nextTick(() => {
    // 账户角色分布环形图
    if (roleChartRef.value) {
      if (!roleChart) {
        roleChart = echarts.init(roleChartRef.value)
      }
      updateCharts()
    }

    // 柱状图对比
    if (barChartRef.value) {
      if (!barChart) {
        barChart = echarts.init(barChartRef.value)
      }
      updateCharts()
    }

    // 风险占比仪表盘
    if (gaugeChartRef.value) {
      if (!gaugeChart) {
        gaugeChart = echarts.init(gaugeChartRef.value)
      }
      updateCharts()
    }
  })
}

// 更新图表数据
const updateCharts = () => {
  if (!taskResult.value) return

  const stats = taskResult.value.statistics || {}
  const victimCount = stats.victimAccounts || 0
  const suspectCount = stats.suspiciousAccounts || 0
  const totalRiskCount = stats.totalRiskAccounts || 0
  const totalAccounts = stats.totalAccounts || 0
  // 正常账户 = 总账户数 - 风险账户总数
  const normalCount = Math.max(0, totalAccounts - totalRiskCount)

  // 分类趋势对比柱状图
  if (barChart) {
    const categories = [
      {
        name: '正常账户',
        count: normalCount,
        percent: totalAccounts > 0 ? ((normalCount / totalAccounts) * 100).toFixed(1) : 0,
        color: ['#b7eb8f', '#73d13d']
      },
      {
        name: '受害人账户',
        count: victimCount,
        percent: totalAccounts > 0 ? ((victimCount / totalAccounts) * 100).toFixed(1) : 0,
        color: ['#ffe58f', '#fa8c16']
      },
      {
        name: '可疑账户',
        count: suspectCount,
        percent: totalAccounts > 0 ? ((suspectCount / totalAccounts) * 100).toFixed(1) : 0,
        color: ['#ff9c9c', '#ff4d4f']
      }
    ]

    const barOption = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params) => {
          const bar = params.find(item => item.seriesName === '账户数量')
          const line = params.find(item => item.seriesName === '占比')
          return `
            <div style="margin-bottom: 6px; font-weight:600;">${bar?.axisValue || ''}</div>
            <div style="color:#303133;">数量: <span style="color:#409eff;font-weight:700;">${bar?.data || 0}</span> 个</div>
            <div style="color:#303133;">占比: <span style="color:#67c23a;font-weight:700;">${line?.data || 0}%</span></div>
          `
        }
      },
      grid: {
        left: '3%',
        right: '8%',
        top: '12%',
        bottom: '8%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: categories.map(item => item.name),
        axisLine: { lineStyle: { color: '#dcdfe6' } },
        axisLabel: { color: '#606266', fontSize: 13 }
      },
      yAxis: [
        {
          type: 'value',
          name: '账户数量',
          axisLabel: { color: '#909399' },
          splitLine: { lineStyle: { color: '#f0f2f5' } }
        },
        {
          type: 'value',
          name: '占比(%)',
          axisLabel: { color: '#909399' },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '账户数量',
          type: 'bar',
          barWidth: 28,
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: (params) => {
              const color = categories[params.dataIndex].color
              return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: color[0] },
                { offset: 1, color: color[1] }
              ])
            },
            shadowBlur: 10,
            shadowColor: 'rgba(0,0,0,0.15)'
          },
          label: {
            show: true,
            position: 'top',
            color: '#303133',
            fontWeight: 600
          },
          data: categories.map(item => item.count)
        },
        {
          name: '占比',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          symbol: 'circle',
          symbolSize: 10,
          lineStyle: {
            width: 3,
            color: '#409eff'
          },
          itemStyle: {
            color: '#409eff',
            borderWidth: 2,
            borderColor: '#fff'
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64,158,255,0.25)' },
              { offset: 1, color: 'rgba(64,158,255,0.02)' }
            ])
          },
          data: categories.map(item => Number(item.percent))
        }
      ]
    }
    barChart.setOption(barOption)
  }

  // 更新账户角色分布精美3D环形图
  if (roleChart) {
    const total = normalCount + victimCount + suspectCount
    const roleOption = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        borderColor: 'rgba(255, 255, 255, 0.2)',
        borderWidth: 1,
        borderRadius: 12,
        padding: [16, 20],
        textStyle: {
          color: '#fff',
          fontSize: 14,
          fontWeight: 500
        },
        formatter: (params) => {
          return `
            <div style="margin-bottom: 8px; font-size: 16px; font-weight: 700; color: ${params.color};">${params.name}</div>
            <div style="margin-bottom: 4px;">数量: <span style="color: #409eff; font-weight: 700; font-size: 16px;">${params.value}</span> 个</div>
            <div>占比: <span style="color: #67c23a; font-weight: 700; font-size: 16px;">${params.percent}%</span></div>
          `
        }
      },
      legend: {
        show: false  // 禁用ECharts自带图例，使用自定义HTML图例
      },
      series: [
        {
          name: '账户分类',
          type: 'pie',
          radius: ['40%', '75%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 3,
            shadowBlur: 15,
            shadowColor: 'rgba(0, 0, 0, 0.15)'
          },
          label: {
            show: true,
            position: 'outside',
            formatter: (params) => {
              if (params.percent < 1) return ''  // 小于1%不显示标签
              return `{value|${params.value}}`  // 只显示数值，简化标签
            },
            rich: {
              value: {
                fontSize: 16,
                color: '#303133',
                fontWeight: 700,
                lineHeight: 20
              }
            },
            distanceToLabelLine: 8
          },
          labelLine: {
            show: true,
            length: 20,
            length2: 15,
            lineStyle: {
              color: '#909399',
              width: 2,
              type: 'solid'
            },
            smooth: 0.2
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 25,
              shadowOffsetX: 0,
              shadowOffsetY: 8,
              shadowColor: 'rgba(0, 0, 0, 0.4)',
              borderWidth: 4
            },
            label: {
              fontSize: 15,
              fontWeight: 700
            },
            scale: true,
            scaleSize: 8
          },
          animationType: 'scale',
          animationEasing: 'elasticOut',
          animationDelay: (idx) => idx * 150,
          data: [
            {
              value: normalCount,
              name: '正常账户',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#b7eb8f' },
                  { offset: 0.5, color: '#73d13d' },
                  { offset: 1, color: '#52c41a' }
                ])
              }
            },
            {
              value: victimCount,
              name: '受害人账户',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#ffe58f' },
                  { offset: 0.5, color: '#ffc53d' },
                  { offset: 1, color: '#fa8c16' }
                ])
              }
            },
            {
              value: suspectCount,
              name: '可疑账户',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#ff9c9c' },
                  { offset: 0.5, color: '#ff7875' },
                  { offset: 1, color: '#ff4d4f' }
                ])
              }
            }
          ]
        }
      ]
    }
    roleChart.setOption(roleOption)
  }

  // 更新风险占比精美仪表盘
  if (gaugeChart) {
    const riskPercent = parseFloat(riskRatio.value)
    const gaugeOption = {
      backgroundColor: 'transparent',
      tooltip: {
        formatter: `风险占比: ${riskPercent}%`
      },
      series: [
        {
          name: '风险占比',
          type: 'gauge',
          center: ['50%', '55%'],
          radius: '85%',
          min: 0,
          max: 100,
          splitNumber: 10,
          axisLine: {
            lineStyle: {
              width: 20,
              color: [
                [riskPercent / 100, new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: '#ff4d4f' },
                  { offset: 0.5, color: '#fa8c16' },
                  { offset: 1, color: '#ffc53d' }
                ])],
                [1, '#e4e7ed']
              ]
            }
          },
          pointer: {
            itemStyle: {
              color: 'auto',
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.3)'
            },
            length: '70%',
            width: 6
          },
          axisTick: {
            distance: -25,
            length: 8,
            lineStyle: {
              color: '#909399',
              width: 2
            }
          },
          splitLine: {
            distance: -30,
            length: 14,
            lineStyle: {
              color: '#909399',
              width: 3
            }
          },
          axisLabel: {
            distance: -40,
            color: '#606266',
            fontSize: 12,
            fontWeight: 600
          },
          title: {
            offsetCenter: [0, '20%'],
            fontSize: 16,
            color: '#303133',
            fontWeight: 700
          },
          detail: {
            valueAnimation: true,
            offsetCenter: [0, '0%'],
            fontSize: 42,
            fontWeight: 700,
            color: riskPercent > 10 ? '#ff4d4f' : riskPercent > 5 ? '#fa8c16' : '#52c41a',
            formatter: '{value}%'
          },
          data: [
            {
              value: riskPercent,
              name: '风险占比'
            }
          ]
        }
      ]
    }
    gaugeChart.setOption(gaugeOption)
  }
}

// 监听窗口大小变化，调整图表大小
const handleResize = () => {
  if (roleChart) {
    roleChart.resize()
  }
  if (barChart) {
    barChart.resize()
  }
  if (gaugeChart) {
    gaugeChart.resize()
  }
}

// 生命周期
onMounted(() => {
  loadTaskResults()
  window.addEventListener('resize', handleResize)
})

// 监听任务结果变化，更新图表
watch(() => taskResult.value, () => {
  if (taskResult.value) {
    initCharts()
  }
}, { deep: true })

// 生成智能分析
const generateAnalysis = async () => {
  if (!taskResult.value) return

  analyzing.value = true
  analysisResult.value = null

  try {
    // 模拟AI分析延迟
    await new Promise(resolve => setTimeout(resolve, 1500))

    const stats = taskResult.value.statistics || {}
    const accounts = taskResult.value.riskAccounts || []
    const victimCount = stats.victimAccounts || 0
    const suspectCount = stats.suspiciousAccounts || 0
    const totalRiskCount = stats.totalRiskAccounts || 0
    const totalCount = stats.totalAccounts || totalRiskCount
    const normalCount = Math.max(0, totalCount - totalRiskCount)

    // 分析账户角色分布（基于总账户数）
    const normalRatio = totalCount > 0 ? ((normalCount / totalCount) * 100).toFixed(1) : 0
    const victimRatio = totalCount > 0 ? ((victimCount / totalCount) * 100).toFixed(1) : 0
    const suspectRatio = totalCount > 0 ? ((suspectCount / totalCount) * 100).toFixed(1) : 0
    const riskRatio = totalCount > 0 ? ((totalRiskCount / totalCount) * 100).toFixed(1) : 0

    // 分析指标数据
    const indicators = accounts.reduce((acc, account) => {
      if (account.matchedIndicators) {
        Object.keys(account.matchedIndicators).forEach(key => {
          if (!acc[key]) {
            acc[key] = { count: 0, total: 0, accounts: [] }
          }
          acc[key].count++
          acc[key].total += account.matchedIndicators[key] || 0
          acc[key].accounts.push(account.accountNo)
        })
      }
      return acc
    }, {})

    // 确定风险等级（基于风险占比）
    let riskLevel = '低风险'
    let riskType = 'success'
    let riskDescription = ''

    if (parseFloat(riskRatio) >= 10) {
      riskLevel = '高风险'
      riskType = 'error'
      riskDescription = `本次分析共处理${totalCount}个账户，识别出${totalRiskCount}个风险账户，占比${riskRatio}%，超出正常阈值，建议优先处理。`
    } else if (parseFloat(riskRatio) >= 5) {
      riskLevel = '中风险'
      riskType = 'warning'
      riskDescription = `本次分析共处理${totalCount}个账户，识别出${totalRiskCount}个风险账户，占比${riskRatio}%，需进一步核实。`
    } else {
      riskDescription = `本次分析共处理${totalCount}个账户，识别出${totalRiskCount}个风险账户，占比${riskRatio}%，处于正常范围。`
    }

    // 识别犯罪模式
    const patterns = []

    // 模式1: 风险账户占比过高
    if (parseFloat(riskRatio) > 15) {
      patterns.push({
        name: '风险集中',
        severity: 'high',
        description: `风险账户占比${riskRatio}%，明显高于行业平均水平，建议扩大调查范围。`,
        accountCount: totalRiskCount,
        percentage: riskRatio
      })
    }

    // 模式2: 受害人账户占比较高（基于总账户）
    if (parseFloat(victimRatio) > 5) {
      patterns.push({
        name: '受害人集中',
        severity: 'high',
        description: `受害人账户${victimCount}个，占比${victimRatio}%，可能存在批量诈骗行为。`,
        accountCount: victimCount,
        percentage: victimRatio
      })
    }

    // 模式3: 可疑账户占比较高（基于总账户）
    if (parseFloat(suspectRatio) > 5) {
      patterns.push({
        name: '可疑活动集中',
        severity: 'high',
        description: `可疑账户${suspectCount}个，占比${suspectRatio}%，可能存在团伙作案。`,
        accountCount: suspectCount,
        percentage: suspectRatio
      })
    }

    // 模式3: 指标A（短时大额转出）异常
    if (indicators.INDICATOR_A && indicators.INDICATOR_A.count > totalCount * 0.5) {
      patterns.push({
        name: '短时大额转出模式',
        severity: 'medium',
        description: `${indicators.INDICATOR_A.count}个账户出现短时大额转出行为，可能存在资金快速转移特征。`,
        accountCount: indicators.INDICATOR_A.count,
        percentage: ((indicators.INDICATOR_A.count / totalCount) * 100).toFixed(1)
      })
    }

    // 模式4: 指标C（销赃账户转出）异常
    if (indicators.INDICATOR_C && indicators.INDICATOR_C.total > 1000000) {
      patterns.push({
        name: '大额销赃模式',
        severity: 'high',
        description: `检测到总金额超过${(indicators.INDICATOR_C.total / 10000).toFixed(0)}万元的大额转出，可能存在销赃行为。`,
        accountCount: indicators.INDICATOR_C.count,
        percentage: totalCount > 0 ? ((indicators.INDICATOR_C.count / totalCount) * 100).toFixed(1) : 0
      })
    }

    // 模式5: 指标B（短时高频转出）异常
    if (indicators.INDICATOR_B && indicators.INDICATOR_B.count > totalCount * 0.4) {
      patterns.push({
        name: '高频交易模式',
        severity: 'medium',
        description: `${indicators.INDICATOR_B.count}个账户出现短时高频转出，可能存在洗钱或资金分散特征。`,
        accountCount: indicators.INDICATOR_B.count,
        percentage: ((indicators.INDICATOR_B.count / totalCount) * 100).toFixed(1)
      })
    }

    // 如果没有检测到明显模式，添加默认模式
    if (patterns.length === 0) {
      patterns.push({
        name: '混合风险模式',
        severity: 'info',
        description: '风险账户呈现多样化特征，需要结合具体账户信息进行详细分析。',
        accountCount: totalCount,
        percentage: 100
      })
    }

    // 关键发现
    const findings = []
    findings.push(`本次分析覆盖${totalCount}个账户，正常账户${normalCount}个，风险账户${totalRiskCount}个。`)
    findings.push(`风险账户中，受害人${victimCount}个，可疑账户${suspectCount}个。`)

    if (parseFloat(riskRatio) > 10) {
      findings.push(`风险账户占比${riskRatio}%，超过预警阈值，建议启动专项调查。`)
    } else if (parseFloat(riskRatio) < 2) {
      findings.push(`风险账户占比${riskRatio}%，在正常范围内。`)
    } else {
      findings.push(`风险账户占比${riskRatio}%，需保持关注。`)
    }

    if (victimCount > suspectCount * 2) {
      findings.push(`受害人数量显著多于可疑账户，疑似一对多诈骗模式。`)
    } else if (suspectCount > victimCount * 2) {
      findings.push(`可疑账户数量较多，需排查是否存在资金集中转移。`)
    }

    if (indicators.INDICATOR_C && indicators.INDICATOR_C.total > 500000) {
      findings.push(`发现大额资金流动，涉及金额${(indicators.INDICATOR_C.total / 10000).toFixed(0)}万元，建议追踪资金流向。`)
    }

    // 调查建议
    const recommendations = []

    if (victimCount > 0) {
      recommendations.push({
        title: '核实受害人信息',
        description: `联系${victimCount}个受害人账户，确认资金损失情况，收集相关证据。`
      })
    }

    if (suspectCount > 0) {
      recommendations.push({
        title: '追踪可疑账户',
        description: `对${suspectCount}个可疑账户进行资金流向追踪，梳理账户关联关系。`
      })
    }

    if (indicators.INDICATOR_C && indicators.INDICATOR_C.total > 500000) {
      recommendations.push({
        title: '资金流向核查',
        description: `核查大额资金流向，确认资金最终去向，排查接收账户。`
      })
    }

    if (totalRiskCount > 0) {
      recommendations.push({
        title: '关联账户排查',
        description: `基于风险账户，排查其交易对手方和关联账户，扩大调查范围。`
      })
    }

    recommendations.push({
      title: '持续监控',
      description: `对已识别风险账户进行持续监控，关注后续交易行为。`
    })

    analysisResult.value = {
      riskLevel,
      riskType,
      riskDescription,
      patterns,
      findings,
      recommendations
    }
  } catch (error) {
    console.error('生成分析失败:', error)
    ElMessage.error('智能分析失败，请稍后重试')
  } finally {
    analyzing.value = false
  }
}

// 监听任务结果变化，自动生成分析
watch(() => taskResult.value, (newVal) => {
  if (newVal && newVal.riskAccounts && newVal.riskAccounts.length > 0) {
    // 延迟一下，等图表加载完成
    setTimeout(() => {
      generateAnalysis()
    }, 500)
  }
}, { deep: true })

// 组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (roleChart) {
    roleChart.dispose()
    roleChart = null
  }
  if (gaugeChart) {
    gaugeChart.dispose()
    gaugeChart = null
  }
})
</script>

<style scoped>
.task-results {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
}

.top-back-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.back-tip {
  font-size: 13px;
}

.breadcrumb-nav {
  margin-bottom: 16px;
  padding: 12px 0;
}

.breadcrumb-nav :deep(.el-breadcrumb__item) {
  cursor: pointer;
}

.breadcrumb-nav :deep(.el-breadcrumb__inner) {
  color: #606266;
  transition: color 0.3s ease;
}

.breadcrumb-nav :deep(.el-breadcrumb__inner:hover) {
  color: #409EFF;
}

.breadcrumb-nav :deep(.el-breadcrumb__inner.is-link) {
  font-weight: 500;
}

.page-header {
  background: white;
  padding: 20px 24px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  margin-right: 8px;
}

.title-group {
  flex: 1;
}

.page-title {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.page-subtitle {
  margin: 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.loading-container {
  background: white;
  padding: 24px;
  border-radius: 8px;
}

.stats-overview {
  margin-bottom: 20px;
}

.stats-card {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stats-card .el-card__body {
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stats-content {
  flex: 1;
}

.stats-number {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 8px;
}

.stats-label {
  font-size: 14px;
  color: #909399;
}

.stats-icon {
  font-size: 48px;
  opacity: 0.6;
}

.total-card .stats-number { color: #409eff; }
.total-card .stats-icon { color: #409eff; }

.victim-card .stats-number { color: #e6a23c; }
.victim-card .stats-icon { color: #e6a23c; }

.suspicious-card .stats-number { color: #f56c6c; }
.suspicious-card .stats-icon { color: #f56c6c; }

.rate-card .stats-number { color: #67c23a; }
.rate-card .stats-icon { color: #67c23a; }

.charts-section {
  margin-bottom: 20px;
  align-items: stretch;
}

.charts-section .el-col {
  display: flex;
}

.chart-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.chart-card .el-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chart-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart-subtitle {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
  font-weight: normal;
}

.chart-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
}

.chart-content-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(280px, 0.9fr) minmax(320px, 1.1fr);
  align-items: stretch;
}

@media (max-width: 1400px) {
  .chart-content-grid {
    grid-template-columns: minmax(280px, 1fr);
  }
}

.chart-ring-block {
  flex: 1 1 280px;
  background: linear-gradient(135deg, #f0f7ff, #ffffff);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chart-ring-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.chart-ring-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.chart-container-pie {
  width: 100%;
  height: 260px;
}

.chart-ring-legend {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.legend-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  padding-bottom: 6px;
  border-bottom: 1px dashed #ebeef5;
}

.legend-item.normal span:first-child { color: #67c23a; }
.legend-item.warning span:first-child { color: #e6a23c; }
.legend-item.danger span:first-child { color: #f56c6c; }

.stats-info-grid {
  flex: 1 1 320px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.stat-info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  background: #fff;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-info-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-info-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: #f5f7fa;
  color: #409eff;
}

.stat-info-content {
  flex: 1;
}

.stat-info-label {
  font-size: 13px;
  color: #909399;
}

.stat-info-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 4px 0;
}

.stat-info-percent {
  font-size: 12px;
  color: #606266;
}

.stat-info-item.total-stat .stat-info-icon { background: rgba(64, 158, 255, 0.12); color: #409eff; }
.stat-info-item.normal-stat .stat-info-icon { background: rgba(103, 194, 58, 0.12); color: #67c23a; }
.stat-info-item.victim-stat .stat-info-icon { background: rgba(230, 162, 60, 0.12); color: #e6a23c; }
.stat-info-item.suspect-stat .stat-info-icon { background: rgba(245, 108, 108, 0.12); color: #f56c6c; }
.stat-info-item.risk-stat .stat-info-icon { background: rgba(250, 84, 28, 0.12); color: #fa541c; }

.chart-bar-panel {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.chart-bar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chart-bar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
  font-weight: 600;
}

.chart-container-bar {
  width: 100%;
  height: 220px;
}

.chart-container {
  width: 100%;
  height: 350px;
  min-height: 350px;
}

.chart-container-large {
  width: 100%;
  height: 450px;
  min-height: 450px;
}

.gauge-content-wrapper {
  padding: 8px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.gauge-flex {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(320px, 1.1fr) minmax(260px, 0.9fr);
  align-items: start;
}

@media (max-width: 1400px) {
  .gauge-flex {
    grid-template-columns: minmax(320px, 1fr);
  }
}

.gauge-panel {
  flex: 1 1 320px;
  border-radius: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #fff7e6, #ffffff);
  border: 1px solid #ffe7ba;
}

.gauge-panel-badge {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 8px;
}

.gauge-panel-label {
  font-size: 13px;
  color: #fa8c16;
}

.gauge-panel-value {
  font-size: 34px;
  font-weight: 700;
  color: #d46b08;
}

.chart-container-gauge {
  width: 100%;
  height: 320px;
  min-height: 320px;
}

.chart-icon {
  font-size: 18px;
  color: #409eff;
  margin-left: 8px;
}

.risk-detail-panel {
  flex: 1 1 260px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.risk-level-card {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  background: #f5f7ff;
  border: 1px solid #d9e8ff;
}

.risk-level-card.risk-high {
  background: #fff1f0;
  border-color: #ffccc7;
}

.risk-level-card.risk-medium {
  background: #fff7e6;
  border-color: #ffe7ba;
}

.risk-level-card.risk-low {
  background: #f6ffed;
  border-color: #d9f7be;
}

.risk-level-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(64, 158, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  font-size: 20px;
}

.risk-level-card.risk-high .risk-level-icon {
  background: rgba(245, 108, 108, 0.15);
  color: #f56c6c;
}

.risk-level-card.risk-medium .risk-level-icon {
  background: rgba(230, 162, 60, 0.15);
  color: #e6a23c;
}

.risk-level-card.risk-low .risk-level-icon {
  background: rgba(103, 194, 58, 0.15);
  color: #67c23a;
}

.risk-level-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.risk-level-desc {
  font-size: 13px;
  color: #606266;
  margin-top: 4px;
  line-height: 1.5;
}

.risk-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.risk-stat-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
  background: #fff;
}

.risk-stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
}

.risk-stat-icon.danger {
  background: rgba(245, 108, 108, 0.15);
  color: #f56c6c;
}

.risk-stat-icon.success {
  background: rgba(103, 194, 58, 0.15);
  color: #67c23a;
}

.risk-stat-label {
  font-size: 12px;
  color: #909399;
}

.risk-stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.risk-stat-value.danger {
  color: #f56c6c;
}

.risk-stat-value.success {
  color: #67c23a;
}

.risk-insight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.risk-insight-card,
.risk-alert-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
  background: #fff;
}

.risk-alert-card {
  background: #f8fafc;
}

.risk-insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.risk-insight-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.risk-insight-row:last-child {
  margin-bottom: 0;
}

.risk-insight-label {
  width: 90px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.risk-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.risk-dot.normal { background: #67c23a; }
.risk-dot.victim { background: #e6a23c; }
.risk-dot.suspect { background: #f56c6c; }

.risk-insight-bar {
  flex: 1;
  height: 8px;
  border-radius: 10px;
  background: #f0f2f5;
  position: relative;
  overflow: hidden;
}

.risk-insight-bar-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 10px;
}

.risk-insight-bar-fill.normal {
  background: linear-gradient(90deg, #b7eb8f, #52c41a);
}

.risk-insight-bar-fill.victim {
  background: linear-gradient(90deg, #ffe58f, #fa8c16);
}

.risk-insight-bar-fill.suspect {
  background: linear-gradient(90deg, #ff9c9c, #ff4d4f);
}

.risk-insight-value {
  width: 140px;
  text-align: right;
  font-size: 12px;
  color: #909399;
}

.risk-alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px dashed #e4e7ed;
}

.risk-alert-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.risk-alert-tag {
  min-width: 32px;
  text-align: center;
  padding: 2px 6px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.risk-alert-tag.danger { background: #f56c6c; }
.risk-alert-tag.warning { background: #e6a23c; }
.risk-alert-tag.safe { background: #67c23a; }

.risk-alert-text {
  flex: 1;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.risk-alert-empty {
  font-size: 13px;
  color: #c0c4cc;
  text-align: center;
  padding-top: 6px;
}

.risk-meta-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.risk-threshold-card,
.risk-ops-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
  background: #fff;
}

.risk-threshold-status {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}

.risk-threshold-status .status-label {
  font-size: 14px;
  color: #909399;
}

.risk-threshold-status .status-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.risk-threshold-scale {
  position: relative;
  display: flex;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 10px;
  font-size: 12px;
  color: #fff;
  text-align: center;
}

.risk-threshold-segment {
  flex: 1;
  padding: 6px 0;
}

.risk-threshold-segment.low { background: #73d13d; }
.risk-threshold-segment.mid { background: #faad14; }
.risk-threshold-segment.high { background: #f56c6c; }

.risk-threshold-marker {
  position: absolute;
  top: -10px;
  transform: translateX(-50%);
  background: #fff;
  border: 1px solid #409eff;
  border-radius: 10px;
  padding: 2px 8px;
  font-size: 12px;
  color: #409eff;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.15);
}

.risk-threshold-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.risk-ops-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.risk-ops-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.risk-ops-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.risk-ops-badge {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: #f0f5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.risk-ops-content {
  flex: 1;
}

.risk-ops-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.risk-ops-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.intelligence-analysis-card {
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.analysis-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.analysis-icon {
  color: #409EFF;
  font-size: 20px;
}

.analysis-content {
  padding: 8px 0;
}

.analysis-section {
  margin-bottom: 24px;
}

.analysis-section:last-child {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.section-title .el-icon {
  color: #409EFF;
}

.risk-overview {
  margin-bottom: 0;
}

.risk-description {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
}

.patterns-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pattern-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409EFF;
  transition: all 0.3s ease;
}

.pattern-item:hover {
  background: #f0f2f5;
  transform: translateX(4px);
}

.pattern-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.pattern-name {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.pattern-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
}

.pattern-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.findings-list {
  margin: 0;
  padding-left: 20px;
}

.finding-item {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin-bottom: 8px;
}

.finding-item:last-child {
  margin-bottom: 0;
}

.recommendations {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;
}

.recommendation-item:hover {
  background: #f0f2f5;
  border-color: #409EFF;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.recommendation-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.recommendation-content {
  flex: 1;
}

.recommendation-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
  margin-bottom: 4px;
}

.recommendation-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.analysis-loading {
  padding: 20px 0;
}

.analysis-empty {
  padding: 40px 0;
}

.view-switcher {
  margin: 16px 0;
  text-align: center;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
}

.accounts-table-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
}

.accounts-table-card .el-card__header {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-header h3 {
  margin: 0;
  color: #303133;
}

.table-controls {
  display: flex;
  align-items: center;
}

.rule-text {
  max-width: 200px;
}

.indicators-detail {
  max-height: 200px;
  overflow-y: auto;
}

.indicator-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.indicator-key {
  font-weight: 500;
  color: #606266;
  flex: 1;
}

.indicator-value {
  color: #409eff;
  font-weight: 600;
  margin-right: 4px;
}

.indicator-unit {
  color: #909399;
  font-size: 12px;
  min-width: 20px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.account-detail {
  max-height: 500px;
  overflow-y: auto;
}

.indicators-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.indicator-card {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  text-align: center;
}

.indicator-name {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.indicator-card .indicator-value {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
  margin-right: 0;
}

.indicator-card .indicator-unit {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 账户角色标签样式 */
.victim-role-tag {
  background: linear-gradient(135deg, #fff7ed 0%, #fed7aa 100%) !important;
  border: 2px solid #f59e0b !important;
  color: #d97706 !important;
  font-weight: 600 !important;
  box-shadow: 0 2px 4px rgba(245, 158, 11, 0.2) !important;
}

.suspect-role-tag {
  background: linear-gradient(135deg, #fef2f2 0%, #fecaca 100%) !important;
  border: 2px solid #dc2626 !important;
  color: #dc2626 !important;
  font-weight: 600 !important;
  box-shadow: 0 2px 4px rgba(220, 38, 38, 0.2) !important;
}

/* 账户角色容器样式 */
.account-role-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

.role-icon {
  margin-right: 4px;
  font-size: 14px;
}

/* 账户状态标签样式 */
.victim-status-tag.el-tag--success {
  background-color: #f0f9ff !important;
  border-color: #a0c4ff !important;
  color: #0969da !important;
}

.victim-status-tag.el-tag--warning {
  background-color: #fefce8 !important;
  border-color: #fed7aa !important;
  color: #d97706 !important;
}

.suspect-status-tag.el-tag--danger {
  background-color: #fef2f2 !important;
  border-color: #fecaca !important;
  color: #dc2626 !important;
}

.suspect-status-tag.el-tag--warning {
  background-color: #fefce8 !important;
  border-color: #fed7aa !important;
  color: #d97706 !important;
}

.suspect-status-tag.el-tag--success {
  background-color: #f0f9ff !important;
  border-color: #a0c4ff !important;
  color: #0969da !important;
}

/* 对话框样式优化 */
:deep(.account-detail-dialog .el-dialog) {
  border-radius: 12px;
}

:deep(.account-detail-dialog .el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

:deep(.account-detail-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.account-detail-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-overview .el-col {
    margin-bottom: 10px;
  }

  .table-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .table-controls {
    width: 100%;
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>