// 虚拟数据生成器
import { testModeConfig } from '@/config/testMode'
import { loadAllDataFromCSV } from './csvDataLoader'

// 随机数生成工具
const random = {
  int: (min, max) => Math.floor(Math.random() * (max - min + 1)) + min,
  float: (min, max, decimals = 2) => +(Math.random() * (max - min) + min).toFixed(decimals),
  choice: (array) => array[Math.floor(Math.random() * array.length)],
  date: (daysAgo = 365) => {
    const now = new Date()
    const past = new Date(now.getTime() - Math.random() * daysAgo * 24 * 60 * 60 * 1000)
    return past
  },
  boolean: () => Math.random() > 0.5,
  id: () => Math.floor(Math.random() * 1000000) + 1
}

// 姓名库
const firstNames = ['张', '王', '李', '赵', '刘', '陈', '杨', '黄', '周', '吴', '徐', '孙', '马', '朱', '胡', '林', '郭', '何', '高', '罗']
const lastNames = ['伟', '芳', '娜', '敏', '静', '雷', '丽', '军', '洋', '勇', '艳', '杰', '娟', '涛', '明', '超', '秀英', '霞', '平', '燕']

// IP地址库
const ipPrefixes = ['192.168.1', '192.168.0', '10.0.0', '172.16.0', '202.108.22', '61.135.169', '123.125.114']

// 生成随机姓名
const generateName = () => {
  return random.choice(firstNames) + random.choice(lastNames) + (Math.random() > 0.7 ? random.choice(lastNames) : '')
}

// 生成随机手机号
const generatePhone = () => {
  const prefixes = ['138', '139', '158', '159', '188', '189', '178', '198']
  return random.choice(prefixes) + String(random.int(10000000, 99999999))
}

// 生成随机身份证号
const generateIdCard = () => {
  const prefix = random.choice(['110101', '320101', '440101', '510101'])
  const year = random.int(1970, 2000)
  const month = String(random.int(1, 12)).padStart(2, '0')
  const day = String(random.int(1, 28)).padStart(2, '0')
  const suffix = String(random.int(1000, 9999))
  return prefix + year + month + day + suffix
}

// 生成随机账号
const generateAccountNo = () => {
  return '62' + String(random.int(100000000000000, 999999999999999))
}

// 生成随机IP
const generateIP = () => {
  const prefix = random.choice(ipPrefixes)
  return `${prefix}.${random.int(1, 254)}`
}

// 生成转账流水号
const generateTransferNo = () => {
  const date = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  return `TF${date}${String(random.int(100000, 999999))}`
}

// 账户数据生成器
export const generateAccounts = (count = testModeConfig.dataGeneration.accounts) => {
  const accounts = []
  const usedPhones = new Set()
  const usedIdCards = new Set()
  const usedAccountNos = new Set()
  
  for (let i = 0; i < count; i++) {
    let phone, idCard, accountNo
    
    // 确保唯一性
    do { phone = generatePhone() } while (usedPhones.has(phone))
    do { idCard = generateIdCard() } while (usedIdCards.has(idCard))
    do { accountNo = generateAccountNo() } while (usedAccountNos.has(accountNo))
    
    usedPhones.add(phone)
    usedIdCards.add(idCard)
    usedAccountNos.add(accountNo)
    
    accounts.push({
      id: i + 1,
      accountNo,
      holderName: generateName(),
      idCardNo: idCard,
      phone,
      accountType: random.choice([1, 1, 1, 2]), // 大部分是个人账户
      status: random.choice([1, 1, 1, 1, 2, 3]), // 大部分是正常状态
      createTime: random.date(1000).toISOString(),
      lastActiveTime: random.date(30).toISOString()
    })
  }
  
  return accounts
}

// 登录记录数据生成器
export const generateLoginRecords = (accounts, count = testModeConfig.dataGeneration.loginRecords) => {
  const records = []
  
  for (let i = 0; i < count; i++) {
    const account = random.choice(accounts)
    const loginTime = random.date(90)
    
    records.push({
      recordId: i + 1,
      accountId: account.id,
      accountName: account.holderName,
      accountNo: account.accountNo,
      phone: account.phone,
      loginTime: loginTime.toISOString(),
      loginTimeDisplay: loginTime.toLocaleString('zh-CN'),
      loginIp: generateIP(),
      loginResult: random.choice([1, 1, 1, 1, 1, 2]), // 成功率较高
      loginResultDisplay: null,
      isSuccessful: null,
      isFailed: null
    })
  }
  
  // 设置显示字段
  records.forEach(record => {
    record.isSuccessful = record.loginResult === 1
    record.isFailed = record.loginResult === 2
    record.loginResultDisplay = record.loginResult === 1 ? '成功' : '失败'
  })
  
  // 按时间倒序排序
  records.sort((a, b) => new Date(b.loginTime) - new Date(a.loginTime))
  
  return records
}

// 转账记录数据生成器
export const generateTransferRecords = (accounts, count = testModeConfig.dataGeneration.transferRecords) => {
  const records = []
  
  for (let i = 0; i < count; i++) {
    const fromAccount = random.choice(accounts)
    let toAccount
    do {
      toAccount = random.choice(accounts)
    } while (toAccount.id === fromAccount.id)
    
    const transferTime = random.date(180)
    const amount = random.choice([
      random.float(100, 1000),      // 小额
      random.float(1000, 10000),    // 中额
      random.float(10000, 50000),   // 大额
      random.float(50000, 500000)   // 特大额
    ])
    
    records.push({
      transferId: i + 1,
      fromAccountId: fromAccount.id,
      toAccountId: toAccount.id,
      transferNo: generateTransferNo(),
      amount: amount,
      amountDisplay: amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
      isLargeAmount: amount >= 50000,
      transferTime: transferTime.toISOString(),
      transferTimeDisplay: transferTime.toLocaleString('zh-CN'),
      fromAccountName: fromAccount.holderName,
      toAccountName: toAccount.holderName,
      fromAccountNo: fromAccount.accountNo,
      toAccountNo: toAccount.accountNo,
      fromAccountPhone: fromAccount.phone,
      toAccountPhone: toAccount.phone
    })
  }
  
  // 按时间倒序排序
  records.sort((a, b) => new Date(b.transferTime) - new Date(a.transferTime))
  
  return records
}

// 分析指标数据生成器
export const generateIndicators = (count = testModeConfig.dataGeneration.indicators) => {
  const indicators = [
    {
      indicatorId: 1,
      indicatorCode: 'INDICATOR_A',
      indicatorName: '短时大额转出',
      indicatorDesc: '账户在最近30天内，在前一笔大额交易后2分钟内又发起大额转出（大于等于5万元）的次数',
      paramConfig: {
        large_amount_threshold: 50000,
        time_window_days: 30,
        quick_out_minutes: 2,
        sql_template: "SELECT account_id, COUNT(*) as value FROM ..."
      },
      isCore: 1,
      isActive: 1,
      userType: 'payer',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    },
    {
      indicatorId: 2,
      indicatorCode: 'INDICATOR_B',
      indicatorName: '短时高频转出',
      indicatorDesc: '账户在最近30天内，登录后5分钟内发起大额转出的次数',
      paramConfig: {
        login_to_transfer_minutes: 5,
        time_window_days: 30,
        min_transfer_amount: 50000,
        sql_template: "SELECT account_id, COUNT(*) as value FROM ..."
      },
      isCore: 1,
      isActive: 1,
      userType: 'payer',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    },
    {
      indicatorId: 3,
      indicatorCode: 'INDICATOR_C',
      indicatorName: '销赃账户转出',
      indicatorDesc: '收款方在最近30天内累计入账金额为0',
      paramConfig: {
        time_window_days: 30,
        cumulative_income_threshold: 0,
        sql_template: "SELECT account_id, SUM(amount) as value FROM ..."
      },
      isCore: 1,
      isActive: 1,
      userType: 'payee',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    },
    {
      indicatorId: 4,
      indicatorCode: 'INDICATOR_D',
      indicatorName: '深夜转账',
      indicatorDesc: '转账金额大于1万元，发生在23:00-4:00之间',
      paramConfig: {
        min_amount: 10000,
        start_hour: 23,
        end_hour: 4,
        sql_template: "SELECT account_id, COUNT(*) as value FROM ..."
      },
      isCore: 0,
      isActive: 1,
      userType: 'payer',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    },
    {
      indicatorId: 5,
      indicatorCode: 'INDICATOR_E',
      indicatorName: '异地转账',
      indicatorDesc: '账户在1小时内发生多地转账',
      paramConfig: {
        time_window_hours: 1,
        min_locations: 2,
        sql_template: "SELECT account_id, COUNT(DISTINCT location) as value FROM ..."
      },
      isCore: 0,
      isActive: 1,
      userType: 'payer',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    },
    {
      indicatorId: 6,
      indicatorCode: 'INDICATOR_F',
      indicatorName: '团伙转账',
      indicatorDesc: '异常账户向其他账户转账',
      paramConfig: {
        abnormal_account_threshold: 1,
        sql_template: "SELECT account_id, COUNT(*) as value FROM ..."
      },
      isCore: 0,
      isActive: 1,
      userType: 'payer',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    },
    {
      indicatorId: 7,
      indicatorCode: 'INDICATOR_N',
      indicatorName: '正常指标',
      indicatorDesc: '正常规则下的交易转账',
      paramConfig: {
        normal_transaction: true,
        sql_template: "SELECT account_id, COUNT(*) as value FROM ..."
      },
      isCore: 0,
      isActive: 1,
      userType: 'both',
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    }
  ]
  
  // 如果需要的指标数量超过已定义的，生成更多指标（跳过H, I, J）
  if (count > 7) {
    for (let i = 8; i <= count; i++) {
      const codeChar = String.fromCharCode(65 + i - 1)
      // 跳过H(72), I(73), J(74)
      if (codeChar === 'H' || codeChar === 'I' || codeChar === 'J') {
        continue
      }
      indicators.push({
        indicatorId: i,
        indicatorCode: `INDICATOR_${codeChar}`,
        indicatorName: `指标${codeChar}`,
        indicatorDesc: `这是第${i}个分析指标的描述`,
        paramConfig: {
          threshold: random.int(1000, 100000),
          time_window: random.int(1, 30),
          sql_template: "SELECT account_id, COUNT(*) as value FROM ..."
        },
        isCore: random.choice([0, 1]),
        isActive: 1,
        userType: random.choice(['payer', 'payee', 'both']),
        createTime: random.date(30).toISOString(),
        updateTime: new Date().toISOString()
      })
    }
  }
  
  return indicators
}

// 分析任务数据生成器
export const generateTasks = (count = testModeConfig.dataGeneration.tasks) => {
  const tasks = []
  const statuses = [0, 1, 2, 2, 2, 3] // 大部分已完成
  
  for (let i = 0; i < count; i++) {
    const createTime = random.date(60)
    const status = random.choice(statuses)
    let endTime = null
    let progress = 0
    
    if (status === 1) {
      progress = random.int(10, 90)
    } else if (status === 2) {
      progress = 100
      endTime = new Date(createTime.getTime() + random.int(300000, 3600000)).toISOString()
    } else if (status === 3) {
      progress = random.int(10, 80)
      endTime = new Date(createTime.getTime() + random.int(600000, 1800000)).toISOString()
    }
    
    tasks.push({
      taskId: i + 1,
      taskName: `电诈风险排查任务${i + 1}`,
      taskCode: `TASK_${new Date().getFullYear()}${String(i + 1).padStart(3, '0')}`,
      indicatorIds: '1,2,3',
      ruleConfig: {
        type: 'separate_rules',
        victimRule: 'INDICATOR_A > 0 AND INDICATOR_B > 0',
        suspectRule: 'INDICATOR_C >= 100000'
      },
      timeRange: random.choice([7, 30, 90, 365]),
      taskTimeRange: random.choice([90, 180, 365]),
      indicatorTimeWindow: random.choice([7, 15, 30]),
      startTime: createTime.toISOString(),
      endTime,
      taskStatus: status,
      progress,
      analyzedAccounts: random.int(100, 1000),
      resultCount: status === 2 ? random.int(5, 50) : (status === 3 ? 0 : null),
      createTime: createTime.toISOString(),
      updateTime: new Date().toISOString()
    })
  }
  
  // 按创建时间倒序排序
  tasks.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
  
  return tasks
}

// 风险结果数据生成器
export const generateRiskResults = (accounts, tasks) => {
  const results = []
  
  tasks.filter(task => task.taskStatus === 2).forEach(task => {
    const riskAccountCount = task.resultCount || random.int(5, 30)
    const selectedAccounts = random.choice(accounts.slice(0, riskAccountCount))
    
    for (let i = 0; i < riskAccountCount; i++) {
      const account = accounts[i % accounts.length]
      
      // 统一设置accountRole，保持与accountRoleText一致
      const accountRole = random.choice([1, 2]) // 1=受害人, 2=可疑账户
      const accountRoleText = accountRole === 1 ? '受害人' : '可疑账户'

      results.push({
        riskId: results.length + 1,
        taskId: task.taskId,
        accountId: account.id,
        accountRole: accountRole,
        matchedIndicators: {
          INDICATOR_A: random.int(0, 5),
          INDICATOR_B: random.int(0, 3),
          INDICATOR_C: random.float(0, 1000000)
        },
        matchedRules: random.choice([
          'INDICATOR_A > 0 AND INDICATOR_B > 0',
          'INDICATOR_C >= 100000',
          'INDICATOR_A > 2 OR INDICATOR_B > 1'
        ]),
        createTime: task.endTime,
        updateTime: task.endTime,
        // 扩展账户信息
        accountNo: account.accountNo,
        holderName: account.holderName,
        phone: account.phone,
        accountStatus: account.status,
        accountType: account.accountType,
        idCardNo: account.idCardNo,
        accountRoleText: accountRoleText
      })
    }
  })
  
  return results
}

// 统计数据生成器
export const generateStatistics = (data) => {
  const { accounts, loginRecords, transferRecords, tasks } = data
  
  // 登录统计
  const successfulLogins = loginRecords.filter(r => r.loginResult === 1).length
  const failedLogins = loginRecords.filter(r => r.loginResult === 2).length
  const successRate = loginRecords.length > 0 ? (successfulLogins / loginRecords.length * 100).toFixed(1) : 0
  
  // 今日登录
  const today = new Date().toDateString()
  const todayLogins = loginRecords.filter(r => new Date(r.loginTime).toDateString() === today).length
  
  // 转账统计
  const totalAmount = transferRecords.reduce((sum, r) => sum + r.amount, 0)
  const largeTransfers = transferRecords.filter(r => r.amount >= 50000).length
  const todayTransfers = transferRecords.filter(r => new Date(r.transferTime).toDateString() === today).length
  const todayAmount = transferRecords
    .filter(r => new Date(r.transferTime).toDateString() === today)
    .reduce((sum, r) => sum + r.amount, 0)
  
  // 任务统计
  const runningTasks = tasks.filter(t => t.taskStatus === 1).length
  const completedTasks = tasks.filter(t => t.taskStatus === 2).length
  const todayTasks = tasks.filter(t => new Date(t.createTime).toDateString() === today).length
  
  return {
    login: {
      total_count: loginRecords.length,
      success_count: successfulLogins,
      failed_count: failedLogins,
      success_rate: `${successRate}%`,
      today_count: todayLogins,
      today_success_count: loginRecords.filter(r => 
        r.loginResult === 1 && new Date(r.loginTime).toDateString() === today
      ).length,
      unique_ip_count: new Set(loginRecords.map(r => r.loginIp)).size
    },
    transfer: {
      total_count: transferRecords.length,
      total_amount: totalAmount,
      total_amount_display: totalAmount.toLocaleString('zh-CN', { 
        style: 'currency', 
        currency: 'CNY' 
      }),
      large_amount_count: largeTransfers,
      today_count: todayTransfers,
      today_amount: todayAmount,
      avg_amount: transferRecords.length > 0 ? (totalAmount / transferRecords.length).toFixed(2) : 0
    },
    task: {
      totalCount: tasks.length,
      runningCount: runningTasks,
      completedCount: completedTasks,
      todayCount: todayTasks,
      successRate: tasks.length > 0 ? (completedTasks / tasks.length * 100).toFixed(1) : 0
    },
    account: {
      totalCount: accounts.length,
      activeCount: accounts.filter(a => a.status === 1).length,
      suspiciousCount: accounts.filter(a => a.status === 2).length,
      frozenCount: accounts.filter(a => a.status === 3).length
    }
  }
}

// 主数据生成器 - 优先从CSV加载，如果失败则使用随机生成
export const generateAllMockData = async () => {
  console.log('🔄 加载数据...')
  
  // 尝试从CSV加载数据
  try {
    const csvData = await loadAllDataFromCSV()
    
    // 如果CSV数据不为空，使用CSV数据
    if (csvData.accounts.length > 0 || csvData.loginRecords.length > 0 || csvData.transferRecords.length > 0) {
      console.log('✅ 使用CSV文件数据')
      console.log('📊 CSV数据统计:', {
        账户数: csvData.accounts.length,
        登录记录数: csvData.loginRecords.length,
        转账记录数: csvData.transferRecords.length
      })
      
      const accounts = csvData.accounts.length > 0 ? csvData.accounts : generateAccounts()
      const loginRecords = csvData.loginRecords.length > 0 ? csvData.loginRecords : generateLoginRecords(accounts)
      const transferRecords = csvData.transferRecords.length > 0 ? csvData.transferRecords : generateTransferRecords(accounts)
      const indicators = generateIndicators()
      const tasks = generateTasks()
      const riskResults = generateRiskResults(accounts, tasks)
      const statistics = generateStatistics({ accounts, loginRecords, transferRecords, tasks })
      
      const mockData = {
        accounts,
        loginRecords,
        transferRecords,
        indicators,
        tasks,
        riskResults,
        statistics,
        generatedAt: new Date().toISOString(),
        dataSource: 'CSV'
      }
      
      console.log('✅ 数据加载完成:', {
        accounts: accounts.length,
        loginRecords: loginRecords.length,
        transferRecords: transferRecords.length,
        indicators: indicators.length,
        tasks: tasks.length,
        riskResults: riskResults.length
      })
      console.log('📌 数据来源: CSV文件')
      
      return mockData
    }
  } catch (error) {
    console.warn('⚠️ CSV数据加载失败，使用随机生成数据:', error)
  }
  
  // 如果CSV加载失败或数据为空，使用随机生成
  console.log('⚠️ CSV数据为空或加载失败，使用随机生成数据...')
  
  const accounts = generateAccounts()
  const loginRecords = generateLoginRecords(accounts)
  const transferRecords = generateTransferRecords(accounts)
  const indicators = generateIndicators()
  const tasks = generateTasks()
  const riskResults = generateRiskResults(accounts, tasks)
  const statistics = generateStatistics({ accounts, loginRecords, transferRecords, tasks })
  
  const mockData = {
    accounts,
    loginRecords,
    transferRecords,
    indicators,
    tasks,
    riskResults,
    statistics,
    generatedAt: new Date().toISOString(),
    dataSource: 'RANDOM'
  }
  
  console.log('✅ 测试数据生成完成:', {
    accounts: accounts.length,
    loginRecords: loginRecords.length,
    transferRecords: transferRecords.length,
    indicators: indicators.length,
    tasks: tasks.length,
    riskResults: riskResults.length
  })
  console.log('📌 数据来源: 随机生成')
  
  return mockData
}
