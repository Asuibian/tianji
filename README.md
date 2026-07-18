# 天机学堂（TianJi Learning Platform）

> 基于 Spring Cloud Alibaba 微服务架构开发的在线职业技能教育平台  
> 面向成年人职业技能培训场景，提供课程学习、互动交流、积分体系、优惠券营销等核心业务能力。

[项目地址](https://github.com/Asuibian/tianji)

---

## 📖 项目介绍

天机学堂是一款面向成年人的在线职业技能培训平台，采用**微服务架构**设计。

系统主要分为：

- **学生端**
  - 课程学习
  - 视频播放
  - 学习进度记录
  - 问答评论
  - 点赞互动
  - 积分体系
  - 优惠券领取与使用

- **管理端**
  - 课程管理
  - 教师管理
  - 用户管理
  - 营销活动管理

项目基于 Spring Cloud Alibaba 微服务体系进行开发，通过 Redis、RabbitMQ、XXL-JOB 等中间件解决高并发、异步处理、数据一致性等实际业务问题。

---

# 🏗️ 项目架构

```
                    Nginx
                      |
                Gateway网关
                      |
 ------------------------------------------------
 |        |          |          |          |
用户服务 课程服务 学习服务 互动服务 营销服务
 |        |          |          |          |
MySQL   MySQL      Redis     MQ       Redis
                         |
                    RabbitMQ
                         |
                    积分服务
```

---

# 🛠️ 技术栈

## 后端技术

| 技术 | 使用场景 |
|----|----|
| Java | 后端开发语言 |
| Spring Boot | 微服务开发基础框架 |
| Spring Cloud Alibaba | 微服务治理 |
| Nacos | 服务注册与配置中心 |
| OpenFeign | 服务间调用 |
| MyBatis-Plus | 数据访问层开发 |
| MySQL | 业务数据存储 |
| Redis | 缓存、高并发优化 |
| Redisson | 分布式锁 |
| RabbitMQ | 异步消息通信 |
| XXL-JOB | 分布式任务调度 |
| Docker | 服务部署 |
| Git | 版本管理 |


---

# 📂 服务模块

```
tianji
├── tj-gateway        网关服务
├── tj-auth           认证服务
├── tj-user           用户服务
├── tj-course         课程服务
├── tj-learning       学习服务
├── tj-interaction    互动服务
├── tj-points         积分服务
├── tj-promotion      营销服务
└── tj-common         公共模块
```

---

# ⭐ 核心功能实现

## 1. 学习服务 - 断点续播

### 业务场景

用户观看视频过程中，需要实时保存播放进度。

如果每次播放进度变化都直接更新数据库，会造成大量数据库写压力。


### 实现方案

采用：

- Redis 暂存播放进度
- Redis 合并写请求
- DelayQueue 延迟处理


流程：

```
用户播放视频
      |
      ↓
更新播放进度
      |
      ↓
Redis缓存最新进度
      |
      ↓
DelayQueue延迟消费
      |
      ↓
批量更新MySQL
```


优化效果：

- 降低数据库写压力
- 控制播放进度误差在15秒以内


---

# 2. 问答评论模块

### 功能

用户可以：

- 发布问题
- 回复评论
- 选择匿名发布


### 技术实现

业务数据保存后：

```
用户操作
   |
   ↓
问答服务
   |
   ↓
RabbitMQ
   |
   ↓
积分服务
```


通过 MQ 实现：

- 异步解耦
- 提升接口响应速度
- 降低服务之间依赖


---

# 3. 点赞服务

### 功能

实现课程、评论点赞。


设计：

- Redis记录点赞数量
- MySQL保存点赞关系
- 定时任务同步业务数据


流程：

```
用户点赞

 ↓

Redis增加点赞数

 ↓

定时任务

 ↓

MQ通知业务方

 ↓

更新数据库
```


---

# 4. 签到功能

### 技术方案

使用 Redis Bitmap 保存用户签到记录。


优势：

- 空间占用低
- 查询效率高


流程：

```
用户签到

 ↓

Bitmap记录日期

 ↓

RabbitMQ发送积分消息

 ↓

积分系统增加积分
```


---

# 5. 积分排行榜


### 实现方案

实时排行：

```
Redis ZSet
```

利用：

- score 保存积分
- member 保存用户ID


历史排行：

```
XXL-JOB分片任务

        ↓

查询Redis排行榜

        ↓

MySQL分库分表保存
```


通过 MyBatis-Plus 动态表名插件实现分表。


---

# 6. 优惠券兑换码生成


### 技术方案

采用：

- 按位加权求和算法
- 异步线程生成兑换码
- Redis Bitmap校验兑换状态


解决：

- 兑换码重复问题
- 暴力枚举问题
- 大批量生成效率问题


---

# 7. 优惠券领取


### 问题

高并发情况下：

多个用户同时领取有限库存优惠券。


### 解决方案


使用：

- Redis预扣库存
- Redisson分布式锁
- 乐观锁


保证：

- 库存安全
- 防止超卖


同时通过 AspectJ 动态代理解决：

由于事务边界导致分布式锁失效的问题。


---

# 8. 通用分布式锁封装


基于：

- AOP
- 自定义注解
- 工厂模式
- 策略模式


实现统一分布式锁组件。


使用方式：

```java
@DistributedLock(
    name="coupon"
)
public void receiveCoupon(){

}
```


降低业务代码侵入。


---

# 9. 优惠券智能使用


### 业务流程


用户下单：

```
查询用户优惠券

 ↓

初筛

 ↓

细筛

 ↓

优惠券排列组合

 ↓

CompletableFuture并行计算

 ↓

选择最优优惠方案
```


使用 CompletableFuture：

- 提升组合计算效率
- 降低接口响应时间


---

# 🚀 项目启动

## 环境要求

```
JDK 8+
MySQL 8+
Redis 6+
RabbitMQ
Nacos
Maven
Docker
```


## 启动步骤


### 1. 克隆项目

```bash
git clone https://github.com/Asuibian/tianji.git
```


### 2. 初始化数据库

执行：

```
sql/
```

目录下 SQL 文件。


### 3. 启动基础服务

启动：

- MySQL
- Redis
- RabbitMQ
- Nacos


### 4. Maven启动


```bash
mvn clean package
```


启动各微服务。


---

# 📌 项目总结


通过该项目实践，掌握：

- Spring Cloud Alibaba 微服务开发
- Redis高并发优化方案
- RabbitMQ异步解耦
- 分布式锁设计
- 分布式任务调度
- MySQL分库分表
- 高并发业务设计


该项目主要用于学习和实践企业级 Java 后端开发流程。


---

# 👨‍💻 Author

**李赞王**

Java Backend Developer Intern
