# Rubbish Cloud - 微服务分布式系统

基于 Spring Cloud Alibaba 的企业级微服务分布式系统脚手架。

## 技术栈

### 核心框架
- Spring Boot 2.7.18
- Spring Cloud 2021.0.8
- Spring Cloud Alibaba 2021.0.5.0

### 服务注册与发现
- Nacos 2.2.0 - 服务注册、配置中心

### 服务网关
- Spring Cloud Gateway
- Gateway 限流、熔断、降级

### 服务调用
- OpenFeign
- Sentinel 1.8.6 - 流量控制、熔断降级

### 分布式事务
- Seata 1.5.2 - AT 模式分布式事务

### 消息队列
- RabbitMQ 3.12 - 异步消息、解耦

### 持久化
- MyBatis Plus 3.5.3.1
- MySQL 8.0
- Druid 1.2.15

### 缓存
- Redis 7.x

### 链路追踪
- SkyWalking 8.12.0
- Zipkin

## 项目结构

```
rubbish-cloud/
├── rubbish-common          # 公共模块
│   ├── constant           # 常量定义
│   ├── exception          # 异常处理
│   ├── result             # 统一返回结果
│   ├── utils              # 工具类
│   ├── feign              # Feign 配置
│   └── annotations        # 自定义注解
│
├── rubbish-gateway        # 网关服务
│   ├── filter             # 网关过滤器
│   ├── config             # 网关配置
│   └── handler            # 全局异常处理
│
├── rubbish-auth           # 认证服务
│   ├── controller         # 控制器
│   ├── service            # 服务层
│   ├── mapper             # 数据访问层
│   ├── feign              # Feign 客户端
│   └── security           # 安全配置
│
├── rubbish-user           # 用户服务
│   ├── controller         # 控制器
│   ├── service            # 服务层
│   ├── mapper             # 数据访问层
│   ├── entity             # 实体类
│   ├── dto                # 数据传输对象
│   ├── vo                 # 视图对象
│   └── config             # 配置类
│
├── rubbish-order          # 订单服务
│   ├── controller         # 控制器
│   ├── service            # 服务层
│   ├── mapper             # 数据访问层
│   ├── entity             # 实体类
│   ├── dto                # 数据传输对象
│   ├── vo                 # 视图对象
│   ├── feign              # Feign 客户端
│   └── consumer           # 消息消费者
│
├── sql/                   # 数据库脚本
├── seata/                 # Seata 配置
└── docker-compose.yml     # Docker Compose 编排文件
```

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.x
- Docker & Docker Compose (可选)

### 1. 启动基础服务（使用 Docker Compose）

```bash
# 启动所有基础服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

服务访问地址：
- Nacos: http://localhost:8848/nacos (nacos/nacos)
- Sentinel: http://localhost:8858 (sentinel/sentinel)
- RabbitMQ Management: http://localhost:15672 (guest/guest)
- Zipkin: http://localhost:9411
- SkyWalking UI: http://localhost:8088

### 2. 初始化数据库

```bash
# 连接 MySQL 并执行初始化脚本
mysql -u root -p < sql/init.sql
```

### 3. 启动微服务

```bash
# 编译打包
mvn clean install

# 启动网关服务
cd rubbish-gateway
mvn spring-boot:run

# 启动认证服务
cd rubbish-auth
mvn spring-boot:run

# 启动用户服务
cd rubbish-user
mvn spring-boot:run

# 启动订单服务
cd rubbish-order
mvn spring-boot:run
```

### 4. 测试接口

```bash
# 用户登录
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 查询用户列表
curl -X GET http://localhost:9000/api/user/list \
  -H "Authorization: Bearer {token}"

# 创建订单
curl -X POST http://localhost:9000/api/order \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"orderNo":"ORD001","userId":1,"productName":"测试商品","quantity":1,"unitPrice":99.99}'
```

## 功能特性

### 服务治理
- 服务注册与发现
- 配置中心
- 服务健康检查

### 流量控制
- 接口限流
- 熔断降级
- 系统保护

### 分布式事务
- Seata AT 模式
- 自动回滚
- 事务日志

### 安全认证
- JWT 认证
- Token 刷新
- 黑名单机制

### 链路追踪
- 请求链路追踪
- 性能分析
- 异常定位

## 配置说明

### Nacos 配置中心

在 Nacos 中创建以下配置：

**gateway-common.yml**
```yaml
gateway:
  white-list:
    - /api/auth/login
    - /api/auth/register
```

**user-common.yml**
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
```

**Sentinel 流控规则**
- 创建流控规则：`rubbish-gateway-flow-rules`
- 创建降级规则：`rubbish-gateway-degrade-rules`

### Seata 配置

1. 确保 Seata Server 已启动
2. 在业务库中创建 `undo_log` 表
3. 配置事务组：`rubbish-tx-group`

## 开发规范

### 代码规范
- 遵循阿里巴巴 Java 开发规范
- 使用 Lombok 简化代码
- 统一异常处理
- 统一返回结果

### 接口规范
- RESTful 风格
- 统一前缀：`/api/{service}`
- 统一返回格式：`Result<T>`
- 统一异常处理

### 数据库规范
- 表名前缀：`sys_` (系统表)、`biz_` (业务表)
- 必须字段：`id`, `create_time`, `update_time`, `deleted`
- 逻辑删除：`deleted` 字段

## 部署指南

### Docker 部署

```bash
# 构建镜像
./build.sh

# 启动服务
docker-compose up -d
```

### Kubernetes 部署

```bash
# 创建命名空间
kubectl create namespace rubbish

# 部署服务
kubectl apply -f k8s/
```

## 常见问题

### 1. Nacos 无法启动
- 检查 MySQL 连接是否正常
- 确保 `nacos_config` 数据库已创建

### 2. Seata 事务回滚失败
- 检查 `undo_log` 表是否存在
- 确认 Seata Server 连接正常

### 3. RabbitMQ 消息发送失败
- 检查 RabbitMQ 连接
- 确认 Exchange、Queue 和 Binding 已创建

## 联系方式

- 项目地址：https://github.com/yourusername/rubbish-cloud
- 问题反馈：https://github.com/yourusername/rubbish-cloud/issues

## 许可证

[Apache License 2.0](LICENSE)
