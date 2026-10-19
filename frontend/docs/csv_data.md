# 演示数据

维护者确认 `public/data/` 中的数据全部为模拟生成，不代表真实客户、账户或交易。

- `account.csv`：3,000 条模拟账户，包含模拟姓名、证件号码和手机号。
- `logins.csv`：100,000 条模拟登录记录。
- `transactions.csv`：200,000 条模拟交易。

文件无表头；加载器也兼容 `accounts.csv`。这些文件会随前端一起发布，不能在此目录放入真实个人或业务数据。

若需要从其他文件夹复制模拟数据，在 frontend 目录运行：

```sh
node scripts/copy_csv_files.js /path/to/synthetic-data
```

脚本接受 account.csv、accounts.csv 或 account(1).csv，以及 logins.csv 或 logins(1).csv。
