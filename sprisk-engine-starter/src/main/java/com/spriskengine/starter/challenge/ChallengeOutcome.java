package com.spriskengine.starter.challenge;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Rich outcome information that can be shared alongside a {@link ChallengeResolution}.
 * <p>
 * Application developers can leverage the optional metadata to drive custom storage,
 * messaging or analytics pipelines (for example persisting a temporary block record).
 */
public final class ChallengeOutcome {

    public enum Status {
        ALLOW,
        CHALLENGE,
        BLOCK
    }

    private final Status status;
    private final String message;
    private final Duration ttl;
    private final boolean permanent;
    private final Map<String, Object> metadata;

    private ChallengeOutcome(Status status,
                             String message,
                             Duration ttl,
                             boolean permanent,
                             Map<String, Object> metadata) {
        this.status = Objects.requireNonNull(status, "status");
        this.message = message;
        this.ttl = ttl;
        this.permanent = permanent;
        this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    public Status status() {
        return status;
    }

    public String message() {
        return message;
    }

    public Duration ttl() {
        return ttl;
    }

    public boolean permanent() {
        return permanent;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public static Builder allow() {
        return new Builder(Status.ALLOW);
    }

    public static Builder challenge() {
        return new Builder(Status.CHALLENGE);
    }

    public static Builder block() {
        return new Builder(Status.BLOCK);
    }

    public static final class Builder {

        private final Status status;
        private String message;
        private Duration ttl;
        private boolean permanent;
        private final Map<String, Object> metadata = new LinkedHashMap<>();

        private Builder(Status status) {
            this.status = status;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        /**
         * Marks the outcome as permanent. Useful when escalating a temporary block to a blacklist entry.
         */
        public Builder permanent(boolean permanent) {
            this.permanent = permanent;
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        public ChallengeOutcome build() {
            return new ChallengeOutcome(status, message, ttl, permanent, metadata);
        }
    }
}

