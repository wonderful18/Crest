package io.crest.utils;


import io.crest.cache.CrestCacheService;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


@SuppressWarnings("unchecked")
public class CacheUtils {

    private static CrestCacheService cacheService;

    static {
        getService();
    }

    private static CrestCacheService getService() {
        if (ObjectUtils.isEmpty(cacheService)) {
            cacheService = (CrestCacheService) CommonBeanFactory.getBean("crestCacheService");
        }
        return cacheService;
    }

    public static void put(String cacheName, String key, Object val) {
        cacheService.put(cacheName, key, val, 8L, TimeUnit.HOURS);
    }

    public static void put(String cacheName, String key, Object val, Long expTime, TimeUnit unit) {
        cacheService.put(cacheName, key, val, expTime, unit);
    }

    public static Object get(String cacheName, String key) {
        return cacheService.get(cacheName, key);
    }

    public static Boolean keyExist(String cacheName, String key) {
        return cacheService.keyExist(cacheName, key);
    }

    public static void keyRemove(String cacheName, String key) {
        cacheService.keyRemove(cacheName, key);
    }

    public static void remove(String cacheName, String key, Consumer<Object> consumer) {
        cacheService.keyRemove(cacheName, key);
        consumer.accept(null);
        DelayQueueUtils.execute(IDUtils.randomID(16), () -> {
            cacheService.keyRemove(cacheName, key);
        }, 1L);
    }

    public static void remove(String[] cacheNames, String key, Consumer<Object> consumer) {
        Arrays.stream(cacheNames).forEach(cacheName -> cacheService.keyRemove(cacheName, key));
        consumer.accept(null);
        DelayQueueUtils.execute(IDUtils.randomID(16), () -> {
            Arrays.stream(cacheNames).forEach(cacheName -> cacheService.keyRemove(cacheName, key));
        }, 1L);

    }

    public static void remove(String cacheName, List<String> keys, Consumer<Object> consumer) {
        keys.forEach(key -> cacheService.keyRemove(cacheName, key));
        consumer.accept(null);
        DelayQueueUtils.execute(IDUtils.randomID(16), () -> {
            keys.forEach(key -> cacheService.keyRemove(cacheName, key));
        }, 1L);
    }

    public static void remove(String[] cacheNames, List<String> keys, Consumer<Object> consumer) {
        Arrays.stream(cacheNames).forEach(cacheName -> {
            keys.forEach(key -> cacheService.keyRemove(cacheName, key));
        });
        consumer.accept(null);
        DelayQueueUtils.execute(IDUtils.randomID(16), () -> {
            Arrays.stream(cacheNames).forEach(cacheName -> {
                keys.forEach(key -> cacheService.keyRemove(cacheName, key));
            });
        }, 1L);
    }
}
