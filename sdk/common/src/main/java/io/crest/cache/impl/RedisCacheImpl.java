package io.crest.cache.impl;

import io.crest.cache.CrestCacheService;
import io.crest.utils.CommonBeanFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的缓存服务实现。
 */
@ConditionalOnExpression("'${spring.cache.type}'.equals('redis')")
@Component("crestCacheService")
@SuppressWarnings("unchecked")
public class RedisCacheImpl implements CrestCacheService {

    private static final String SEPARATOR = "::";

    @Resource
    private RedisTemplate redisTemplate;

    private static CacheManager cacheManager;


    /**
     * 延迟获取 Spring 缓存管理器。
     */
    private static CacheManager getCacheManager() {
        if (cacheManager == null)
            cacheManager = CommonBeanFactory.getBean(CacheManager.class);
        return cacheManager;
    }


    /**
     * 获取 Redis 字符串值操作对象。
     */
    private ValueOperations ops() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        return valueOperations;
    }

    /**
     * 写入 Redis 缓存键并按需设置过期时间。
     */
    @Override
    public void put(String cacheName, String key, Object value, Long expTime, TimeUnit unit) {
        ValueOperations ops = ops();
        String realKey = cacheName + SEPARATOR + key;
        if (expTime <= 0) {
            ops.set(realKey, value);
            return;
        }
        if (ObjectUtils.isEmpty(unit)) {
            unit = TimeUnit.SECONDS;
        }
        ops.set(realKey, value, expTime, unit);
    }

    /**
     * 获取 Redis 缓存键对应的值。
     */
    @Override
    public Object get(String cacheName, String key) {
        ValueOperations ops = ops();
        return ops.get(cacheName + SEPARATOR + key);
    }

    /**
     * Redis 缓存区域按需创建，因此默认认为存在。
     */
    @Override
    public boolean cacheExist(String cacheName) {
        return true;
    }

    /**
     * 判断指定 Redis 缓存键是否存在。
     */
    @Override
    public boolean keyExist(String cacheName, String key) {
        return redisTemplate.hasKey(cacheName + SEPARATOR + key);
    }

    /**
     * 同时通过 Spring Cache 和 RedisTemplate 移除缓存键。
     */
    @Override
    public void keyRemove(String cacheName, String key) {
        Cache cache = getCacheManager().getCache(cacheName);
        if (null == cache) return;
        cache.evictIfPresent(key);
        // cache.clear();
        redisTemplate.delete(cacheName + SEPARATOR + key);
    }

    /**
     * 保留 Redis 缓存实现的初始化入口。
     */
    @PostConstruct
    public void init() {
    }
}
