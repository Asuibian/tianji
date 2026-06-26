# 天机学堂 · Tianji Academy

> 基于 Spring Cloud Alibaba 微服务架构的在线教育平台，提供课程学习、营销推广、社交互动等功能。

## 👤 我的核心贡献

我作为后端开发，独立负责以下三个核心板块：

### 1. 促销服务 · tj-promotion / tj-trade
- 高并发优惠券领取/兑换，采用 Redis + Lua 原子扣减库存、防超卖、防重复领取。
- 兑换码基于 Bitmap + ZSet 存储，支持海量码的快速校验与匹配。
- 通过 RabbitMQ 异步持久化领券记录，削峰填谷，保证最终一致性。
- 智能优惠券推荐：实现满减、折扣、每满减、无门槛等策略，初筛/细筛后枚举组合，多线程并行计算最优解，选出优惠最大且用券最少的最优组合。

### 2. 学习服务 · tj-learning
- 实现课程学习进度追踪、章节解锁、学习时长统计等核心功能。
- 与积分服务异步通信：利用 MQ 解耦学习行为与积分累积，实现最终一致性。
- 配合 XXL-JOB 定时任务进行学习数据汇总与赛季归档。

### 3. 积分排行榜 · 分布式实时排名
- 基于 Redis ZSet 实现千万级用户的实时积分排序，通过 ZINCRBY 原子更新分数，ZREVRANK 高速查询排名。
- 利用 XXL-JOB 分片广播，在赛季结束时将上月榜单批量持久化至 MySQL，支撑历史排行榜查询。

## 🧱 技术栈
`Spring Cloud Alibaba` `Nacos` `RabbitMQ` `Redis` `MySQL` `XXL-JOB` `CompletableFuture`

## 📂 项目结构
| 模块 | 说明 | 我的贡献 |
|------|------|----------|
| tj-promotion / tj-trade | 促销引擎、优惠券、兑换码、智能推荐 | ✅ 全部后端 |
| tj-learning | 学习进度、章节解锁 | ✅ 全部后端 |
| tj-* 其他模块 | 网关、认证、支付等 | 团队协作 |

## 🚀 快速启动
见各模块下的 application.yml，需配置 Nacos 注册中心、MySQL、Redis、RabbitMQ 等。
