# ============================================================
# Vue 前端 Dockerfile
# ============================================================

# 第一阶段：构建
FROM node:22-alpine AS builder

WORKDIR /app

# 复制 package.json
COPY package*.json ./

# 安装依赖
RUN npm ci

# 复制源码
COPY . .

ARG VITE_DEMO_MODE=false
ARG VITE_DIFY_ENABLED=false
ARG VITE_DIFY_BASE_URL
ARG VITE_DIFY_EMBED_TOKEN
ENV VITE_DEMO_MODE=$VITE_DEMO_MODE VITE_DIFY_ENABLED=$VITE_DIFY_ENABLED VITE_DIFY_BASE_URL=$VITE_DIFY_BASE_URL VITE_DIFY_EMBED_TOKEN=$VITE_DIFY_EMBED_TOKEN

# 构建生产版本
RUN npm run build

# 第二阶段：运行
FROM nginx:alpine

# 复制构建产物到 Nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]

