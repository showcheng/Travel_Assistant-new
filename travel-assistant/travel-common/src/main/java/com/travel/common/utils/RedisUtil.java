package com.travel.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // =============================  基本操作  =============================

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     *
     * @param key 键
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 键集合
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 键
     * @return true: 存在, false: 不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     *
     * @param key 键
     * @return 过期时间（秒）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    // =============================  String 类型  =============================

    /**
     * 递增
     *
     * @param key 键
     * @return 递增后的值
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 递增（指定步长）
     *
     * @param key   键
     * @param step 步长
     * @return 递增后的值
     */
    public Long increment(String key, long step) {
        return redisTemplate.opsForValue().increment(key, step);
    }

    /**
     * 递减
     *
     * @param key 键
     * @return 递减后的值
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 递减（指定步长）
     *
     * @param key   键
     * @param step 步长
     * @return 递减后的值
     */
    public Long decrement(String key, long step) {
        return redisTemplate.opsForValue().decrement(key, step);
    }

    // =============================  Hash 类型  =============================

    /**
     * 获取 Hash 中的值
     *
     * @param key     键
     * @param hashKey Hash 键
     * @return 值
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 设置 Hash 中的值
     *
     * @param key     键
     * @param hashKey Hash 键
     * @param value   值
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 批量设置 Hash
     *
     * @param key 键
     * @param map 值
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取所有 Hash 值
     *
     * @param key 键
     * @return Map
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除 Hash 中的值
     *
     * @param key     键
     * @param hashKeys Hash 键集合
     */
    public Long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 判断 Hash 中是否存在该键
     *
     * @param key     键
     * @param hashKey Hash 键
     * @return true: 存在, false: 不存在
     */
    public Boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    // =============================  List 类型  =============================

    /**
     * 获取 List 缓存的内容
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 值列表
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取 List 缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 在 List 中添加元素（左侧）
     *
     * @param key   键
     * @param value 值
     */
    public Long lLeftPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 在 List 中添加元素（右侧）
     *
     * @param key   键
     * @param value 值
     */
    public Long lRightPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 获取 List 中的元素（左侧弹出）
     *
     * @param key 键
     * @return 值
     */
    public Object lLeftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 获取 List 中的元素（右侧弹出）
     *
     * @param key 键
     * @return 值
     */
    public Object lRightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    // =============================  Set 类型  =============================

    /**
     * 向 Set 中添加元素
     *
     * @param key     键
     * @param values 值集合
     * @return 添加成功的数量
     */
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 获取 Set 中的所有元素
     *
     * @param key 键
     * @return 值集合
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 判断 Set 中是否存在该元素
     *
     * @param key   键
     * @param value 值
     * @return true: 存在, false: 不存在
     */
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取 Set 的长度
     *
     * @param key 键
     * @return 长度
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 删除 Set 中的元素
     *
     * @param key    键
     * @param values 值集合
     * @return 删除成功的数量
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    // =============================  ZSet 类型  =============================

    /**
     * 向 ZSet 中添加元素
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 获取 ZSet 中指定范围的元素（按分数升序）
     *
     * @param key   键
     * @param start 开始分数
     * @param end   结束分数
     * @return 值集合
     */
    public Set<Object> zRangeByScore(String key, double start, double end) {
        return redisTemplate.opsForZSet().rangeByScore(key, start, end);
    }

    /**
     * 获取 ZSet 中指定范围的元素（按分数降序）
     *
     * @param key   键
     * @param start 开始分数
     * @param end   结束分数
     * @return 值集合
     */
    public Set<Object> zReverseRangeByScore(String key, double start, double end) {
        return redisTemplate.opsForZSet().reverseRangeByScore(key, start, end);
    }

    /**
     * 删除 ZSet 中的元素
     *
     * @param key    键
     * @param values 值集合
     * @return 删除成功的数量
     */
    public Long zRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }
}
