# 互动模块 · Remark

## 核心职责
- 问答系统（提问、回复、采纳）
- 多维度点赞（课程、评论、问答）

## 技术亮点
- Redis Set 存储点赞关系，保证高并发下幂等性
- 异步持久化点赞数据，确保最终一致性
- 问答采用 MySQL + Redis 缓存热数据，提高响应速度

## 快速查看源码
- 点赞接口：[LikesController.java](./src/main/java/com/tianji/remark/controller/LikesController.java) （请根据实际文件名调整）
