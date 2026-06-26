# 学习模块 · Learning

## 核心职责
- 课程学习进度追踪
- 章节解锁
- 学习时长统计
- 与积分服务联动

## 技术亮点
- 使用 Redis 缓存用户最新进度，异步写入 MySQL
- 学习行为通过 MQ 通知积分服务，实现最终一致性
- XXL-JOB 定时汇总学习数据，配合赛季归档

## 快速查看源码
- 积分排行榜相关：[PointsBoardSeasonServiceImpl.java](./src/main/java/com/tianji/learning/service/impl/PointsBoardSeasonServiceImpl.java)
- 积分记录服务：[PointsRecordServiceImpl.java](./src/main/java/com/tianji/learning/service/impl/PointsRecordServiceImpl.java)
