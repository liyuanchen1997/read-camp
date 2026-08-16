package com.readcamp.common;

import lombok.Getter;

/**
 * 统一响应包装：{code, message, data}，code=0 表示成功
 */
@Getter
public class Result<T> {

    public static final int SUCCESS = 0;

    private final int code;
    private final String message;
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS, "ok", data);
    }

    public static Result<Void> ok() {
        return new Result<>(SUCCESS, "ok", null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
