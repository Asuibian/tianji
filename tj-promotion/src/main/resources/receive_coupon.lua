-- 手动 领取优惠券校验的lua脚本

-- EXISTS mykey 判断key、不存在返回 0
-- 判断优惠券缓存   存在不存在
if (redis.call('exists', KEYS[1]) == 0)
then
    return 1
end

-- HGET <某个key> totalNum  获取hash结构中的totalNumfield字段的属性
-- tonumber获取到字符串转换成数字类型、用于比较
-- 库存小于等于0表示库存不足、返回2
if (tonumber(redis.call('hget', KEYS[1], 'totalNum')) <= 0)
then
    return 2
end

-- redis 中缓存的时候存储的是 过期时间的 秒级时间戳
-- time [1] 获取当前时间的秒级时间戳
if (tonumber(redis.call('time')[1]) >
        tonumber(redis.call('hget', KEYS[1], 'issueEndTime'))
        or
        tonumber(redis.call('time')[1]) <
                tonumber(redis.call('hget', KEYS[1], 'issueBeginTime'))
)
then
    return 3
end

-- 判断用户限领数量
-- hincrby key field 自增的数量、、、返回自增后的数量
if (tonumber(redis.call('hget', KEYS[1], 'userLimit'))  -- 获取优惠券基本信息中的限领的数量
        <
        redis.call('hincrby', KEYS[2], ARGV[1], 1)) -- 缓存用户领取的优惠券的信息、不存在就创建并从0开始自增、返回自增后的值
then
    return 4
end

-- 减少缓存中的优惠券的库存
redis.call('hincrby', KEYS[1], "totalNum", "-1")
return 0