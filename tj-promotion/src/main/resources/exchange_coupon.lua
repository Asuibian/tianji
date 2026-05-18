-- 兑换码领取优惠券        的lua脚本
-- 再原本的java代码中、没有使用getbiit命令而选择setbit是为了保证线程安全、若get和set、之间的空挡会被其它线程进入修改
if (redis.call('GETBIT', KEYS[1], ARGV[1]) == 1)
then
    return 1
end

-- 获取序列号对应的无序集合中的成员、就是优惠券id
-- 最大分数、、、最小分数、、、从0开始找、找一个、再区间内的值
local arr = redis.call('ZRANGEBYSCORE', KEYS[2], ARGV[1], ARGV[2], 'LIMIT', 0, 1);
if (#arr == 0) -- #arr 是获取数组（table）长度
then
    return 2
end

--
-- .. 是 Lua 的拼接运算符
local cid = arr[1]
local _k1 = "prs:coupon:" .. cid
local _k2 = "prs:user:coupon:" .. cid
if (redis.call('EXISTS', _k1) == 0)
then
    return 3
end

-- redis 中缓存的时候存储的是 过期时间的 秒级时间戳
-- time [1] 获取当前时间的秒级时间戳
if (tonumber(redis.call('time')[1]) >
        tonumber(redis.call('hget', _k1, 'issueEndTime'))
        or
        tonumber(redis.call('time')[1]) <
                tonumber(redis.call('hget', _k1, 'issueBeginTime'))
)
then
    return 4
end

if (tonumber(redis.call('HGET', _k1, 'userLimit')) < redis.call('HINCRBY', _k2, ARGV[3], 1))
then
    return 5
end

redis.call('SETBIT', KEYS[1], ARGV[1], "1")
return cid