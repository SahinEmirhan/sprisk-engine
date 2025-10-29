package io.github.sahinemirhan.starter.storage;


import io.github.sahinemirhan.core.storage.RiskStorage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis-backed implementation of RiskStorage.
 */
public class RedisRiskStorage implements RiskStorage {

    private final StringRedisTemplate redis;

    public RedisRiskStorage(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Long increment(String key) {
        return redis.opsForValue().increment(key);
    }

    @Override
    public Boolean addMemberToSet(String key, String member) {
        Long added = redis.opsForSet().add(key, member);
        return added != null ? (added > 0) : null;
    }

    @Override
    public Long getDistinctSetSize(String key) {
        return redis.opsForSet().size(key);
    }

    @Override
    public void expireKey(String key, long seconds) {
        redis.expire(key, Duration.ofSeconds(seconds));
    }

    @Override
    public void expireKeyIfExists(String key, long seconds) {
        Long ttl = redis.getExpire(key);
        // -2 = key does not exist, -1 = key exists but no expire
        if (ttl != null && ttl != -2) {
            redis.expire(key, Duration.ofSeconds(seconds));
        }
    }
}

