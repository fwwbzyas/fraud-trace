/**
 * 姓名脱敏工具函数
 * 为了保护用户隐私，对姓名进行脱敏处理
 * - 两个字的名字：第二个字变成*号（例如：张三 -> 张*）
 * - 三个字的名字：中间那个字变成*号（例如：李四王 -> 李*王）
 * - 其他情况：保持原样或只显示第一个字+*
 */

/**
 * 检测是否是公司名称
 * @param {string} name - 名称
 * @returns {boolean} - 是否是公司名称
 */
function isCompanyName(name) {
  const companyKeywords = [
    '公司', '有限', '股份', '集团', '企业', '科技', '贸易', 
    '实业', '投资', '控股', '服务', '咨询', '管理', '开发',
    '建设', '工程', '制造', '销售', '经营', '商贸', '物流'
  ]
  return companyKeywords.some(keyword => name.includes(keyword))
}

/**
 * 提取公司类型后缀（如"有限公司"、"股份有限公司"等）
 * @param {string} name - 公司名称
 * @returns {string} - 公司类型后缀
 */
function extractCompanySuffix(name) {
  const suffixes = [
    '股份有限公司', '有限公司', '有限责任公司', 
    '集团有限公司', '企业集团', '实业集团',
    '科技股份有限公司', '投资有限公司'
  ]
  
  for (const suffix of suffixes) {
    if (name.endsWith(suffix)) {
      return suffix
    }
  }
  
  // 如果没有匹配到完整后缀，尝试匹配部分
  if (name.endsWith('公司')) {
    return '公司'
  }
  if (name.endsWith('集团')) {
    return '集团'
  }
  
  return ''
}

/**
 * 对姓名进行脱敏处理
 * @param {string} name - 原始姓名
 * @returns {string} - 脱敏后的姓名
 */
export function maskName(name) {
  if (!name || typeof name !== 'string') {
    return name || ''
  }
  
  // 去除首尾空格
  const trimmedName = name.trim()
  
  // 如果为空或只有一个字符，直接返回
  if (trimmedName.length === 0) {
    return name
  }
  
  if (trimmedName.length === 1) {
    return trimmedName
  }
  
  // 检测是否是公司名称
  if (isCompanyName(trimmedName)) {
    return maskCompanyName(trimmedName)
  }
  
  // 个人姓名脱敏逻辑
  // 两个字的名字：第二个字变成*
  if (trimmedName.length === 2) {
    return trimmedName[0] + '*'
  }
  
  // 三个字的名字：中间那个字变成*
  if (trimmedName.length === 3) {
    return trimmedName[0] + '*' + trimmedName[2]
  }
  
  // 四个字或以上的名字：中间部分用*替代（保留首尾各一个字）
  if (trimmedName.length >= 4) {
    const firstChar = trimmedName[0]
    const lastChar = trimmedName[trimmedName.length - 1]
    const middleStars = '*'.repeat(trimmedName.length - 2)
    return firstChar + middleStars + lastChar
  }
  
  return trimmedName
}

/**
 * 对公司名称进行脱敏处理
 * 保留前2-3个字符和公司类型后缀，中间用少量*号替代
 * @param {string} companyName - 公司名称
 * @returns {string} - 脱敏后的公司名称
 */
function maskCompanyName(companyName) {
  const suffix = extractCompanySuffix(companyName)
  const nameWithoutSuffix = suffix ? companyName.slice(0, -suffix.length) : companyName
  
  // 如果去除后缀后名称很短（<=3个字符），只保留第一个字符
  if (nameWithoutSuffix.length <= 3) {
    return nameWithoutSuffix[0] + '**' + suffix
  }
  
  // 如果去除后缀后名称较短（4-6个字符），保留前2个字符
  if (nameWithoutSuffix.length <= 6) {
    const prefix = nameWithoutSuffix.substring(0, 2)
    const stars = '*'.repeat(Math.min(2, nameWithoutSuffix.length - 2))
    return prefix + stars + suffix
  }
  
  // 如果去除后缀后名称较长（>6个字符），保留前3个字符
  const prefix = nameWithoutSuffix.substring(0, 3)
  const stars = '*'.repeat(Math.min(3, nameWithoutSuffix.length - 3))
  return prefix + stars + suffix
}

/**
 * 身份证号脱敏工具函数
 * 为了保护用户隐私，对身份证号进行脱敏处理
 * - 18位身份证：前6位（地区码）+ 8位*号（生日YYYYMMDD）+ 后4位（顺序码和校验码）
 * - 15位身份证：前6位（地区码）+ 6位*号（生日YYMMDD）+ 后3位（顺序码）
 * @param {string} idCardNo - 原始身份证号
 * @returns {string} - 脱敏后的身份证号
 */
export function maskIdCard(idCardNo) {
  if (!idCardNo || typeof idCardNo !== 'string') {
    return idCardNo || ''
  }
  
  // 去除首尾空格
  const trimmedIdCard = idCardNo.trim()
  
  // 如果为空，直接返回
  if (trimmedIdCard.length === 0) {
    return idCardNo
  }
  
  // 18位身份证号：前6位 + 8位*号 + 后4位
  if (trimmedIdCard.length === 18) {
    const prefix = trimmedIdCard.substring(0, 6)  // 前6位地区码
    const suffix = trimmedIdCard.substring(14)     // 后4位
    return prefix + '********' + suffix
  }
  
  // 15位身份证号：前6位 + 6位*号 + 后3位
  if (trimmedIdCard.length === 15) {
    const prefix = trimmedIdCard.substring(0, 6)  // 前6位地区码
    const suffix = trimmedIdCard.substring(12)    // 后3位
    return prefix + '******' + suffix
  }
  
  // 其他长度：如果长度在10-17之间，尝试脱敏中间部分
  if (trimmedIdCard.length >= 10 && trimmedIdCard.length <= 17) {
    const prefix = trimmedIdCard.substring(0, 6)
    const suffix = trimmedIdCard.substring(trimmedIdCard.length - 4)
    const middleStars = '*'.repeat(trimmedIdCard.length - 10)
    return prefix + middleStars + suffix
  }
  
  // 长度不符合身份证号格式，返回原值
  return trimmedIdCard
}

