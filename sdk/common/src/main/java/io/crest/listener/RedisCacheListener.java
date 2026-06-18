package io.crest.listener;

import io.crest.runtime.CrestRuntimeRole;
import io.crest.utils.LogUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ConditionalOnExpression("'${spring.cache.type}'.equals('redis')")
@Component
@Order(100)
@SuppressWarnings({"deprecation", "unchecked"})
// 定义当前业务对象的核心职责
public class RedisCacheListener implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private Environment environment;

    @Override
    // 执行当前服务方法并返回处理结果
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!shouldClearLegacyCache()) {
            return;
        }
        try {
            deleteKeysContainingString(redisTemplate, "crest_v1_");
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
        }
    }

    private boolean shouldClearLegacyCache() {
        if (!CrestRuntimeRole.from(environment).runsScheduler()) {
            return false;
        }
        String explicit = StringUtils.defaultIfBlank(environment.getProperty("crest.cache.clear-v1-on-startup"),
                environment.getProperty("CREST_CACHE_CLEAR_V1_ON_STARTUP"));
        if (StringUtils.isNotBlank(explicit)) {
            return Boolean.parseBoolean(explicit.trim());
        }
        return !environment.getProperty("crest.production-mode", Boolean.class,
                environment.getProperty("CREST_PRODUCTION_MODE", Boolean.class, false));
    }


    // 执行当前服务方法并返回处理结果
    public void deleteKeysContainingString(RedisTemplate<String, String> redisTemplate, String searchString) {
        // 扫描所有的key
        ScanOptions scanOptions = ScanOptions.scanOptions().match("*" + searchString + "*").count(1000).build();
        Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(scanOptions);

        List<byte[]> keysToDelete = new ArrayList<>();
        while (cursor.hasNext()) {
            keysToDelete.add(cursor.next());
        }

        if (!keysToDelete.isEmpty()) {
            List<String> keys = new ArrayList<>(keysToDelete.size());
            for (byte[] key : keysToDelete) {
                keys.add(new String(key, StandardCharsets.UTF_8));
            }
            redisTemplate.delete(keys);
        }
    }
}
