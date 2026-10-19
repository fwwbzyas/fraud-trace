// CSV数据加载器
// 从public/data目录加载CSV文件并转换为应用所需格式

// 解析CSV行（简单解析，假设没有引号内的逗号）
const parseCSVLine = (line) => {
  // 移除BOM字符（如果存在）
  let cleanLine = line
  if (line.charCodeAt(0) === 0xFEFF) {
    cleanLine = line.slice(1)
  }
  // 移除首尾空白和换行符
  cleanLine = cleanLine.trim()
  // 按逗号分割并清理每个字段
  const fields = cleanLine.split(',').map(field => field.trim())
  return fields
}

// 加载CSV文件
const loadCSV = async (filename) => {
  try {
    console.log(`📂 尝试加载文件: /data/${filename}`)
    const response = await fetch(`/data/${filename}`)
    if (!response.ok) {
      throw new Error(`无法加载文件: ${filename}, 状态码: ${response.status}`)
    }
    const text = await response.text()
    // 处理不同的换行符（\n, \r\n, \r）
    const lines = text.split(/\r?\n|\r/).filter(line => line.trim().length > 0)
    console.log(`📄 文件 ${filename} 读取成功，共 ${lines.length} 行`)
    if (lines.length > 0) {
      console.log(`📋 第一行示例: ${lines[0].substring(0, 100)}...`)
      // 检查第一行是否有BOM
      if (lines[0].charCodeAt(0) === 0xFEFF) {
        console.warn(`⚠️ 文件 ${filename} 包含BOM字符，将自动移除`)
      }
    }
    const parsed = lines.map(parseCSVLine)
    if (parsed.length > 0) {
      console.log(`🔍 解析后第一行字段数: ${parsed[0].length}, 字段:`, parsed[0].slice(0, 5))
    }
    return parsed
  } catch (error) {
    console.error(`❌ 加载CSV文件失败 ${filename}:`, error)
    return []
  }
}

// 检查是否是有效的CSV数据（不是HTML错误页面）
const isValidCSVData = (rows) => {
  if (rows.length === 0) return false
  // 检查第一行是否包含HTML标签（说明是错误页面）
  if (rows.length > 0 && rows[0].length > 0) {
    const firstField = rows[0][0]
    if (typeof firstField === 'string' && (
      firstField.includes('<!DOCTYPE') || 
      firstField.includes('<html') || 
      firstField.includes('404') ||
      firstField.includes('Not Found')
    )) {
      return false
    }
  }
  // 检查第一行是否有足够的字段（至少应该有多个字段）
  if (rows.length > 0 && rows[0].length < 5) {
    return false
  }
  return true
}

// 从CSV加载账户数据
// CSV格式: id,accountNo,holderName,idCardNo,phone,accountType,status,bankCode,bankName,province,city,isCorp,createTime,lastActiveTime
export const loadAccountsFromCSV = async () => {
  // 尝试加载 accounts.csv，如果不存在或无效则尝试 account.csv
  console.log('🔍 开始加载账户CSV文件...')
  let rows = await loadCSV('accounts.csv')
  console.log(`📊 accounts.csv 加载结果: ${rows.length} 行`)
  
  // 检查是否是有效的CSV数据
  if (!isValidCSVData(rows)) {
    console.log('⚠️ accounts.csv 无效或不存在，尝试加载 account.csv...')
    rows = await loadCSV('account.csv')
    console.log(`📊 account.csv 加载结果: ${rows.length} 行`)
    
    // 再次检查
    if (!isValidCSVData(rows)) {
      console.warn('❌ account.csv 也无效或不存在')
      return { accounts: [], accountNoMap: new Map() }
    }
  }
  
  if (rows.length === 0) {
    console.warn('❌ 未找到账户数据，将使用空数组')
    return { accounts: [], accountNoMap: new Map() }
  }

  const accounts = []
  const accountNoMap = new Map() // 用于建立accountNo到id的映射

  console.log(`🔍 开始解析账户数据，共 ${rows.length} 行`)
  if (rows.length > 0) {
    console.log(`📋 第一行原始数据:`, rows[0])
    console.log(`📋 第一行字段数: ${rows[0].length}`)
    console.log(`📋 第一行所有字段:`, rows[0])
  }
  let skippedCount = 0
  
  rows.forEach((row, index) => {
    // 跳过空行
    if (!row || row.length === 0) {
      skippedCount++
      return
    }
    
    if (row.length < 14) {
      if (index < 10) { // 显示前10行的错误以便调试
        console.warn(`⚠️ 第 ${index + 1} 行字段数不足 (${row.length}/14):`, row.slice(0, 10))
      }
      skippedCount++
      return // 跳过格式不正确的行
    }

    const accountNo = row[1]
    if (!accountNo || accountNo.trim() === '') {
      if (index < 10) {
        console.warn(`⚠️ 第 ${index + 1} 行账户号为空，完整行:`, row.slice(0, 5))
      }
      skippedCount++
      return
    }

    const account = {
      id: parseInt(row[0]) || index + 1,
      accountNo: accountNo,
      holderName: row[2] || '',
      idCardNo: row[3] || '',
      phone: row[4] || '',
      accountType: parseInt(row[5]) || 0, // 0=个人, 1=企业
      status: parseInt(row[6]) || 1, // 1=正常, 2=可疑, 3=冻结
      bankCode: row[7] || '',
      bankName: row[8] || '',
      province: row[9] || '',
      city: row[10] || '',
      isCorp: row[11] === '1',
      createTime: row[12] || new Date().toISOString(),
      lastActiveTime: row[13] || new Date().toISOString()
    }

    accounts.push(account)
    accountNoMap.set(accountNo, account.id)
  })

  console.log(`✅ 从CSV加载了 ${accounts.length} 条账户数据 (跳过 ${skippedCount} 行无效数据)`)
  if (accounts.length === 0 && rows.length > 0) {
    console.error(`❌ 账户数据解析失败！`)
    console.error(`❌ 总行数: ${rows.length}, 成功解析: ${accounts.length}, 跳过: ${skippedCount}`)
    if (rows.length > 0) {
      console.error(`❌ 第一行数据:`, rows[0])
      console.error(`❌ 第一行字段数: ${rows[0].length}, 期望: 14`)
      // 显示前几行的详细信息
      for (let i = 0; i < Math.min(3, rows.length); i++) {
        console.error(`❌ 第 ${i + 1} 行: 字段数=${rows[i].length}, 内容=`, rows[i].slice(0, 5))
      }
    }
  }
  
  if (accounts.length === 0) {
    console.error(`❌ 严重错误：账户数据为空，将导致转账记录无法匹配！`)
  } else {
    console.log(`✅ 账户数据加载成功，accountNoMap大小: ${accountNoMap.size}`)
  }
  
  return { accounts, accountNoMap }
}

// 从CSV加载登录记录
// CSV格式: id,accountNo,loginTime,location,deviceType,os,loginResult,updateTime
export const loadLoginRecordsFromCSV = async (accountNoMap) => {
  const rows = await loadCSV('logins.csv')
  if (rows.length === 0) {
    console.warn('未找到登录记录数据，将使用空数组')
    return []
  }

  const records = []
  const ipPrefixes = ['192.168.1', '192.168.0', '10.0.0', '172.16.0', '202.108.22', '61.135.169', '123.125.114']
  
  // 生成随机IP（因为CSV中没有IP字段）
  const generateIP = () => {
    const prefix = ipPrefixes[Math.floor(Math.random() * ipPrefixes.length)]
    return `${prefix}.${Math.floor(Math.random() * 254) + 1}`
  }

  console.log(`🔍 开始解析登录记录，共 ${rows.length} 行`)
  let skippedCount = 0
  
  rows.forEach((row, index) => {
    if (row.length < 8) {
      if (index < 3) {
        console.warn(`⚠️ 登录记录第 ${index + 1} 行字段数不足 (${row.length}/8):`, row.slice(0, 3))
      }
      skippedCount++
      return
    }

    const accountNo = row[1]
    const accountId = accountNoMap.get(accountNo)
    if (!accountId) {
      if (index < 3) {
        console.warn(`⚠️ 登录记录第 ${index + 1} 行找不到对应账户: ${accountNo}`)
      }
      skippedCount++
      return // 跳过没有对应账户的记录
    }

    const loginTime = new Date(row[2])
    const loginResult = row[6] === 't' || row[6] === 'T' ? 1 : 2 // t=成功, f=失败

    records.push({
      recordId: parseInt(row[0]) || index + 1,
      accountId: accountId,
      accountName: '', // 将在后续关联账户时填充
      accountNo: accountNo,
      phone: '', // 将在后续关联账户时填充
      loginTime: loginTime.toISOString(),
      loginTimeDisplay: loginTime.toLocaleString('zh-CN'),
      loginIp: generateIP(),
      loginResult: loginResult,
      loginResultDisplay: loginResult === 1 ? '成功' : '失败',
      isSuccessful: loginResult === 1,
      isFailed: loginResult === 2,
      location: row[3] || '',
      deviceType: row[4] || '',
      os: row[5] || ''
    })
  })

  // 按时间倒序排序
  records.sort((a, b) => new Date(b.loginTime) - new Date(a.loginTime))

  console.log(`✅ 从CSV加载了 ${records.length} 条登录记录 (跳过 ${skippedCount} 行无效数据)`)
  if (records.length === 0 && rows.length > 0) {
    console.error(`❌ 登录记录解析失败！第一行数据:`, rows[0])
    console.error(`❌ 第一行字段数: ${rows[0].length}, 期望: 8`)
    console.error(`❌ accountNoMap大小: ${accountNoMap.size}`)
  }
  return records
}

// 从CSV加载转账记录
// CSV格式: id,fromAccountNo,toAccountNo,transferNo,amount,type,status,transferTime,isLargeAmount,riskFlag,updateTime
export const loadTransferRecordsFromCSV = async (accounts) => {
  const rows = await loadCSV('transactions.csv')
  if (rows.length === 0) {
    console.warn('未找到转账记录数据，将使用空数组')
    return []
  }

  const records = []
  const accountMap = new Map() // accountNo -> account对象
  accounts.forEach(acc => {
    accountMap.set(acc.accountNo, acc)
  })

  console.log(`🔍 开始解析转账记录，共 ${rows.length} 行，账户映射大小: ${accountMap.size}`)
  let skippedCount = 0
  
  rows.forEach((row, index) => {
    if (row.length < 11) {
      if (index < 3) {
        console.warn(`⚠️ 转账记录第 ${index + 1} 行字段数不足 (${row.length}/11):`, row.slice(0, 3))
      }
      skippedCount++
      return
    }

    const fromAccountNo = row[1]
    const toAccountNo = row[2]
    const fromAccount = accountMap.get(fromAccountNo)
    const toAccount = accountMap.get(toAccountNo)

    if (!fromAccount || !toAccount) {
      if (index < 3) {
        console.warn(`⚠️ 转账记录第 ${index + 1} 行找不到对应账户: from=${fromAccountNo}, to=${toAccountNo}`)
      }
      skippedCount++
      return // 跳过没有对应账户的记录
    }

    const transferTime = new Date(row[7])
    const amount = parseFloat(row[4]) || 0
    const isLargeAmount = row[8] === 't' || row[8] === 'T'

    records.push({
      transferId: parseInt(row[0]) || index + 1,
      fromAccountId: fromAccount.id,
      toAccountId: toAccount.id,
      transferNo: row[3] || `TR${Date.now()}${index}`,
      amount: amount,
      amountDisplay: amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
      isLargeAmount: isLargeAmount,
      transferTime: transferTime.toISOString(),
      transferTimeDisplay: transferTime.toLocaleString('zh-CN'),
      fromAccountName: fromAccount.holderName,
      toAccountName: toAccount.holderName,
      fromAccountNo: fromAccountNo,
      toAccountNo: toAccountNo,
      fromAccountPhone: fromAccount.phone,
      toAccountPhone: toAccount.phone,
      type: row[5] || 'TRANSFER',
      status: row[6] || 'SUCCESS',
      riskFlag: row[9] || 'N'
    })
  })

  // 按时间倒序排序
  records.sort((a, b) => new Date(b.transferTime) - new Date(a.transferTime))

  console.log(`✅ 从CSV加载了 ${records.length} 条转账记录 (跳过 ${skippedCount} 行无效数据)`)
  if (records.length === 0 && rows.length > 0) {
    console.error(`❌ 转账记录解析失败！第一行数据:`, rows[0])
    console.error(`❌ 第一行字段数: ${rows[0].length}, 期望: 11`)
    console.error(`❌ accountMap大小: ${accountMap.size}`)
  }
  return records
}

// 关联账户信息到登录记录
const enrichLoginRecords = (loginRecords, accounts) => {
  const accountMap = new Map()
  accounts.forEach(acc => {
    accountMap.set(acc.id, acc)
  })

  loginRecords.forEach(record => {
    const account = accountMap.get(record.accountId)
    if (account) {
      record.accountName = account.holderName
      record.phone = account.phone
    }
  })

  return loginRecords
}

// 主加载函数：从CSV加载所有数据
export const loadAllDataFromCSV = async () => {
  console.log('🔄 从CSV文件加载数据...')
  
  try {
    // 1. 加载账户数据（必须先加载，其他数据依赖账户数据）
    console.log('📥 步骤1: 开始加载账户数据...')
    const accountResult = await loadAccountsFromCSV()
    const { accounts, accountNoMap } = accountResult
    console.log(`📊 账户数据加载结果: ${accounts.length} 条账户, accountNoMap大小: ${accountNoMap.size}`)
    
    // 如果账户数据为空，给出严重警告
    if (accounts.length === 0) {
      console.error('❌ 严重错误：账户数据为空！转账记录和登录记录将无法匹配账户！')
      console.error('❌ 请检查 account.csv 或 accounts.csv 文件是否存在且格式正确')
    }
    
    // 2. 加载登录记录（依赖账户数据）
    console.log('📥 步骤2: 开始加载登录记录...')
    let loginRecords = await loadLoginRecordsFromCSV(accountNoMap)
    loginRecords = enrichLoginRecords(loginRecords, accounts)
    console.log(`📊 登录记录加载结果: ${loginRecords.length} 条`)
    
    // 3. 加载转账记录（依赖账户数据）
    console.log('📥 步骤3: 开始加载转账记录...')
    const transferRecords = await loadTransferRecordsFromCSV(accounts)
    console.log(`📊 转账记录加载结果: ${transferRecords.length} 条`)
    
    console.log('✅ 所有CSV数据加载完成:', {
      accounts: accounts.length,
      loginRecords: loginRecords.length,
      transferRecords: transferRecords.length
    })
    
    return {
      accounts,
      loginRecords,
      transferRecords
    }
  } catch (error) {
    console.error('❌ 加载CSV数据失败:', error)
    return {
      accounts: [],
      loginRecords: [],
      transferRecords: []
    }
  }
}

