package com.spriskengine.core.constants;
/**
 * Centralized key naming conventions for all redis/in-memory keys used by Sprisk.
 */
public final class KeyNames {

    private KeyNames() {}

    // counters
    public static final String COUNTER_PREFIX = "sprisk:counter:";
    public static final String COUNTER_FAIL_USER_PREFIX = "sprisk:fail:user:"; // + userId
    public static final String COUNTER_FAIL_IP_PREFIX = "sprisk:fail:ip:";     // + ip
    public static final String COUNTER_IP_VELOCITY_PREFIX = "sprisk:velocity:";
    public static final String COUNTER_USER_VELOCITY_PREFIX = "sprisk:velocity:user:";
    // sets
    public static final String SET_FAIL_IP_USERS_PREFIX = "sprisk:fail:ip:users:"; // + ip

    // generic helpers
    public static String counterForUser(String userId) {
        return COUNTER_FAIL_USER_PREFIX + userId;
    }

    public static String counterForIpVelocity(String ip){
        return COUNTER_IP_VELOCITY_PREFIX + ip;
    }

    public static String counterForUserVelocity(String userId){
        return COUNTER_IP_VELOCITY_PREFIX + userId;
    }

    public static String counterForIp(String ip) {
        return COUNTER_FAIL_IP_PREFIX + ip;
    }

    public static String setForIpUsers(String ip) {
        return SET_FAIL_IP_USERS_PREFIX + ip;
    }
}

