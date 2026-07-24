
我主要负责的代码集中在：

学习服务：tj-learning
主要是课程学习、视频播放进度保存相关代码
Redis缓存播放进度、DelayQueue延迟批量写入MySQL的逻辑主要在这个模块
互动服务：tj-interaction
负责问答评论、点赞相关业务
包括评论发布、点赞记录、MQ异步通知等
积分服务：tj-points
负责签到积分、积分排行榜
Redis Bitmap签到、Redis ZSet排行榜相关代码在这里
营销服务：tj-promotion
负责优惠券相关业务
包括优惠券领取、兑换码生成、优惠券智能推荐等
Redisson分布式锁、Lua脚本、CompletableFuture异步计算主要在这个模块

公共代码，比如 Redis 工具类、MQ封装、异常处理、通用返回对象等放在：

tj-common

如果需要查看具体实现，可以直接从这些模块的 Controller 层进入，然后到 Service 层查看业务逻辑。
