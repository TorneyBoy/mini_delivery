# Docker 启动指南

## 前置要求

- 安装 Docker Desktop（Windows/Mac）或 Docker Engine（Linux）
- 安装 Docker Compose

## 快速启动

### 方式一：使用 Docker Compose（推荐）

在项目根目录执行：

```bash
docker-compose up -d
```

这将启动：
- MySQL 8.0 数据库（端口 3306）
- Spring Boot 后端服务（端口 8081）

### 方式二：仅启动 MySQL

如果需要本地开发调试后端，可以仅启动 MySQL：

```bash
docker-compose up -d mysql
```

然后本地运行后端：

```bash
cd server
mvn spring-boot:run
```

## 服务说明

| 服务    | 端口 | 说明                 |
| ------- | ---- | -------------------- |
| MySQL   | 3306 | 数据库服务           |
| Backend | 8081 | Spring Boot 后端 API |

## 数据库连接信息

- 主机：localhost（Docker 内部使用 mysql）
- 端口：3306
- 数据库：delivery
- 用户名：root
- 密码：mima0221

## 常用命令

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看后端日志
docker-compose logs -f backend

# 停止所有服务
docker-compose down

# 停止并删除数据卷（清除数据库数据）
docker-compose down -v

# 重新构建后端镜像
docker-compose build backend

# 重启后端服务
docker-compose restart backend
```

## 数据持久化

MySQL 数据存储在 Docker 数据卷 `mysql_data` 中，即使删除容器数据也不会丢失。

如需清除所有数据：

```bash
docker-compose down -v
```

## 微信小程序配置

在微信开发者工具中，确保 `miniprogram/app.js` 中的 `baseUrl` 配置正确：

```javascript
globalData: {
  baseUrl: 'http://localhost:8081/api'
}
```

## 登录信息

- 总管理账号：13800000000
- 密码：123456

## 故障排除

### 1. 端口被占用

如果端口被占用，可以修改 `docker-compose.yml` 中的端口映射：

```yaml
ports:
  - "3307:3306"  # MySQL
  - "8082:8081"  # Backend
```

### 2. MySQL 连接失败

等待 MySQL 完全启动（约 30 秒），后端服务会自动重试连接。

### 3. 后端启动失败

查看后端日志：

```bash
docker-compose logs backend
```

### 4. 数据库初始化失败

如果数据库初始化失败，可以手动执行 SQL：

```bash
# 进入 MySQL 容器
docker exec -it delivery-mysql mysql -uroot -pmima0221

# 执行初始化脚本
source /docker-entrypoint-initdb.d/init.sql
```
