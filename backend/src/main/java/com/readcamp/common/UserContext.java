package com.readcamp.common;

import com.readcamp.entity.User;

/**
 * 当前登录用户上下文（ThreadLocal，由 AuthInterceptor 写入）
 * Controller 通过 UserContext.get() 获取当前用户，禁止从请求参数取 userId
 */
public final class UserContext {

    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        User user = HOLDER.get();
        if (user == null) {
            throw ApiException.unauthorized("未登录或登录已过期");
        }
        return user;
    }

    public static Long userId() {
        return get().getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
