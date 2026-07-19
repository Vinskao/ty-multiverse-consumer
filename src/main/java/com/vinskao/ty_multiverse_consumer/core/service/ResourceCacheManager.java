package com.vinskao.ty_multiverse_consumer.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 資源快取管理服務
 * 負責統一管理的資源快取鍵名與清理邏輯
 */
@Service
public class ResourceCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(ResourceCacheManager.class);

    @Autowired(required = false)
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 獲取資源的特定動作快取鍵
     */
    public String getCacheKey(String resourceName, String action) {
        return resourceName + ":" + action;
    }

    /**
     * 获取資源的 "獲取全部" 快取鍵
     */
    public String getGetAllKey(String resourceName) {
        return getCacheKey(resourceName, "getAll");
    }

    /**
     * 清理資源的所有相關快取
     */
    public Mono<Void> evictCache(String resourceName) {
        if (redisService == null) {
            return Mono.empty();
        }

        Mono<?> resourceCaches = Mono.when(
                redisService.delete(getGetAllKey(resourceName)),
                redisService.delete(getCacheKey(resourceName, "names")));

        // 戰力由人物能力與武器共同決定；任一資源異動都必須使戰力快取失效。
        Mono<?> damageCaches = ("people".equals(resourceName) || "weapon".equals(resourceName))
                ? redisService.deleteByPattern("damage-calculations::*")
                : Mono.empty();

        return Mono.when(resourceCaches, damageCaches).then()
                .doOnSuccess(v -> logger.info("🗑️ 已清理資源 [{}] 的相關快取", resourceName));
    }

    /**
     * 獲取快取內容
     */
    public Mono<String> getCache(String key) {
        if (redisService == null) {
            return Mono.empty();
        }
        return redisService.get(key);
    }

    /**
     * 寫入快取 (支援物件自動序列化)
     */
    public Mono<Void> putCache(String key, Object value, Duration ttl) {
        if (redisService == null) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> {
            if (value instanceof String)
                return (String) value;
            return objectMapper.writeValueAsString(value);
        })
                .flatMap(json -> redisService.set(key, json, ttl))
                .then();
    }
}
