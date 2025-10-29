package com.spriskengine.core.window;

import com.spriskengine.core.storage.RiskStorage;

import java.util.Objects;

/**
 * WindowManager applies the configured WindowStrategy around primitive storage operations.
 * Rules call these windowed helpers instead of calling storage directly.
 */
public class WindowManager {

    private final RiskStorage storage;
    private final WindowStrategy strategy;

    public WindowManager(RiskStorage storage, WindowStrategy strategy) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.strategy = strategy == null ? WindowStrategy.SLIDING : strategy;
    }

    /**
     * Increment counter and apply TTL according to strategy.
     */
    public Long incrementInWindow(String key, long windowSeconds) {
        Long value = storage.increment(key);
        applyTtl(key, windowSeconds);
        return value;
    }

    /**
     * Add a distinct member to a set and apply TTL according to strategy.
     */
    public Boolean addDistinctInWindow(String key, String member, long windowSeconds) {
        Boolean added = storage.addMemberToSet(key, member);
        applyTtl(key, windowSeconds);
        return added;
    }

    /**
     * Read-only size (does not alter TTL).
     */
    public Long distinctCount(String key) {
        return storage.getDistinctSetSize(key);
    }

    private void applyTtl(String key, long windowSeconds) {
        if (windowSeconds <= 0) return;
        if (strategy == WindowStrategy.SLIDING) {
            storage.expireKey(key, windowSeconds);
        } else {
            storage.expireKeyIfExists(key, windowSeconds);
        }
    }
}
