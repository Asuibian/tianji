我主要负责的代码集中在：

- [学习服务：tj-learning](https://github.com/Asuibian/tianji/blob/dev/tj-learning/src/main/java/com/tianji/learning/service/impl/LearningRecordServiceImpl.java)
  - 主要是课程学习、视频播放进度保存相关代码。
  - Redis缓存播放进度、DelayQueue延迟批量写入MySQL的逻辑主要在这个模块。

- [积分服务：tj-points](https://github.com/Asuibian/tianji/tree/dev/tj-points)
  - 负责签到积分、积分排行榜。
  - Redis Bitmap签到、Redis ZSet排行榜相关代码在这里。

- [营销服务：tj-promotion](https://github.com/Asuibian/tianji/tree/dev/tj-promotion)
  - 负责优惠券相关业务。
  - 包括优惠券领取、兑换码生成、优惠券智能推荐等。
  - Redisson分布式锁、Lua脚本、CompletableFuture异步计算主要在这个模块。
