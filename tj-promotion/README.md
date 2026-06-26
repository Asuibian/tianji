# 促销模块 · Promotion

## 核心职责
- 优惠券领取、兑换
- 兑换码生成与校验
- 智能优惠券组合推荐

## 技术亮点
- Redis + Lua 保证高并发下库存扣减原子性（防超卖、防重复领取）
- Bitmap + ZSet 存储海量兑换码，O(1) 校验与匹配
- RabbitMQ 异步持久化领券记录，削峰填谷
- 策略模式抽象满减、折扣、每满减、无门槛规则，枚举组合后用 CompletableFuture 并行计算最优解

## 快速查看源码
- 领券控制器：[UserCouponController.java](./src/main/java/com/tianji/promotion/controller/UserCouponController.java)
- 兑换码服务：[ExchangeCodeServiceImpl.java](./src/main/java/com/tianji/promotion/service/impl/ExchangeCodeServiceImpl.java)
- 折扣服务：[DiscountServiceImpl.java](./src/main/java/com/tianji/promotion/service/impl/DiscountServiceImpl.java)
