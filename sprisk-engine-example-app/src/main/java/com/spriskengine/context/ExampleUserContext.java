package com.spriskengine.context;

/**
 * Thread-local demo context that mimics a security context / session scoped storage.
 */
public final class ExampleUserContext {

    private static final ThreadLocal<String> USER = new ThreadLocal<>();

    private ExampleUserContext() {
    }

    public static void set(String userId) {
        USER.set(userId);
    }

    public static String get() {
        return USER.get();
    }

    public static void clear() {
        USER.remove();
    }
}

