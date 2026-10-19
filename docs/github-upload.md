# 首次上传 GitHub

## 1. 创建空仓库

登录 GitHub，打开 https://github.com/new，仓库名填写 `fraud-trace`。
按需要选择 Public 或 Private。**不要勾选自动添加 README、.gitignore 或 License**，本地已有项目文件。

以下假设仓库属于 `fwwbzyas`；如果使用其他账号或仓库名，修改 remote URL。

## 2. 初始化和检查

在终端运行：

```sh
cd /Users/guoxinyu/Projects/fraud-trace
git init -b main
git add .
git status --short
git diff --cached --stat
```

确认暂存内容中没有 `.env`、`.env.local`、`node_modules`、`target`、`dist` 或私钥。`.env.example` 是无秘密的配置模板，应保留。CSV 为已确认的模拟数据，可以上传。

这几条命令只准备本地提交，不会上传。

## 3. 提交并推送

```sh
git commit -m "feat: add financial fraud tracing demo system"
git remote add origin https://github.com/fwwbzyas/fraud-trace.git
git push -u origin main
```

本机刚刚成功推送过同一账号下的另一个仓库，若凭据仍有效且该仓库归属正确，通常会复用已有登录，不需要重新创建令牌。

如果提示 `remote origin already exists`，先运行 `git remote -v` 确认地址；不要盲目重复添加。如果提示远端已有提交，不要使用强制推送，先检查是否误勾选了远端初始化文件。

如果提示身份未知，仅设置当前项目的 `git config user.name` 和 `git config user.email`；它们是提交署名，不是身份验证令牌。

## 4. 后续更新

```sh
git add .
git diff --cached --stat
git commit -m "fix: describe your change"
git push
```

只运行 `git commit` 不会上传；`git push` 才会同步到 GitHub。

参考：[GitHub 官方说明](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)。
