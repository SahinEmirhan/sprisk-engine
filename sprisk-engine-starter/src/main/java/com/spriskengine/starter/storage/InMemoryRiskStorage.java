package com.spriskengine.starter.storage;

import com.spriskengine.core.storage.RiskStorage;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRiskStorage implements RiskStorage {

    private static class CounterEntry {
        long value; long expireAt;
        CounterEntry(long value, long expireAt) { this.value = value; this.expireAt = expireAt; }
    }
    private static class SetEntry {
        final Set<String> members = ConcurrentHashMap.newKeySet();
        long expireAt;
    }

    private final Map<String, CounterEntry> counters = new ConcurrentHashMap<>();
    private final Map<String, SetEntry> sets = new ConcurrentHashMap<>();

    private long now() { return Instant.now().getEpochSecond(); }

    @Override
    public Long increment(String key) {
        long t = now();
        counters.compute(key, (k, e) -> {
            if (e == null || e.expireAt < t) return new CounterEntry(1, 0);
            e.value++; return e;
        });
        return counters.get(key).value;
    }

    @Override
    public Boolean addMemberToSet(String key, String member) {
        long t = now();
        sets.compute(key, (k, e) -> {
            if (e == null || e.expireAt < t) {
                SetEntry ne = new SetEntry();
                ne.members.add(member);
                return ne;
            }
            e.members.add(member);
            return e;
        });
        return true; // we do not differentiate already existed
    }

    @Override
    public Long getDistinctSetSize(String key) {
        long t = now();
        SetEntry e = sets.get(key);
        if (e == null || (e.expireAt > 0 && e.expireAt < t)) {
            sets.remove(key); return 0L;
        }
        return (long) e.members.size();
    }

    @Override
    public void expireKey(String key, long seconds) {
        long exp = now() + seconds;
        CounterEntry c = counters.get(key);
        if (c != null) c.expireAt = exp;
        SetEntry s = sets.get(key);
        if (s != null) s.expireAt = exp;
    }

    @Override
    public void expireKeyIfExists(String key, long seconds) {
        long exp = now() + seconds;
        CounterEntry c = counters.get(key);
        if (c != null && c.expireAt > 0) c.expireAt = exp;
        SetEntry s = sets.get(key);
        if (s != null && s.expireAt > 0) s.expireAt = exp;
    }
}

