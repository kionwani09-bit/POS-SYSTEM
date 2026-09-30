package com.pos.db;

import com.pos.domain.User;

public class SessionContext {
    private static final ThreadLocal<SessionData> CONTEXT = new ThreadLocal<>();

    private SessionContext() {}

    public static void set(User user, long sessionId) {
        CONTEXT.set(new SessionData(user, sessionId));
    }

    public static SessionData get() {
        return CONTEXT.get();
    }

    public static User getCurrentUser() {
        SessionData data = CONTEXT.get();
        return data != null ? data.user() : null;
    }

    public static long getCurrentSessionId() {
        SessionData data = CONTEXT.get();
        return data != null ? data.sessionId() : -1L;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record SessionData(User user, long sessionId) {}
}
