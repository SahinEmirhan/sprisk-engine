package com.spriskengine.core.storage;

/**
 * Minimal storage SPI. No windowing logic here.
 */
public interface RiskStorage {

    /**
     * Atomic counter increment for the given key.
     * @return current counter value, or null on failure
     */
    Long increment(String key);

    /**
     * Add a member to a set (for distinct counting).
     * @return true if newly added, false if already existed, null if not supported
     */
    default Boolean addMemberToSet(String key, String member) { return null; }

    /**
     * Get set size (distinct member count).
     * @return size, or null if not supported
     */
    default Long getDistinctSetSize(String key) { return null; }

    /**
     * Apply TTL to the key, setting/overwriting expiration.
     */
    void expireKey(String key, long seconds);

    /**
     * Apply TTL only if the key already exists.
     */
    void expireKeyIfExists(String key, long seconds);
}
