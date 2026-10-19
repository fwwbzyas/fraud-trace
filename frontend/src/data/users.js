// 用户账号数据
// 在实际项目中，这些数据应该存储在数据库中

export const users = [
  {
    username: 'admin',
    password: '123456',
    role: 'admin',
    name: '系统管理员',
    email: 'admin@bank.com',
    createTime: '2024-01-01T00:00:00.000Z'
  },
  {
    username: 'staff001',
    password: 'staff123',
    role: 'staff',
    name: '普通职员',
    email: 'staff001@bank.com',
    createTime: '2024-01-01T00:00:00.000Z'
  }
]

// 根据用户名查找用户
export function findUserByUsername(username) {
  return users.find(user => user.username === username)
}

// 验证用户密码
export function verifyPassword(user, password) {
  return user && user.password === password
}

// 获取所有用户
export function getAllUsers() {
  return users.map(({ password, ...user }) => ({
    ...user,
    password: '***' // 不返回真实密码
  }))
}

