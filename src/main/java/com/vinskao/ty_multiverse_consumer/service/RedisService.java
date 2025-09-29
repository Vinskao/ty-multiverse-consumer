package com.vinskao.ty_multiverse_consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    private final ReactiveValueOperations<String, String> values;

    @Autowired
    public RedisService(ReactiveRedisOperations<String, String> redisOperations) {
        this.values = redisOperations.opsForValue();
    }

    public Mono<Boolean> setIfAbsent(String key, String value, Duration ttl) {
        return values.setIfAbsent(key, value, ttl)
                .doOnNext(set -> logger.debug("Redis SETNX {} => {} (ttl={}s)", key, set, ttl != null ? ttl.toSeconds() : null));
    }

    public Mono<String> get(String key) {
        return values.get(key)
                .doOnNext(val -> logger.debug("Redis GET {} => {}", key, val));
    }

    public Mono<Boolean> set(String key, String value, Duration ttl) {
        if (ttl != null) {
            return values.set(key, value, ttl).doOnNext(ok -> logger.debug("Redis SET {} ttl={}s", key, ttl.toSeconds()));
        }
        return values.set(key, value).doOnNext(ok -> logger.debug("Redis SET {}", key));
    }
}



