package com.readcamp.common;

import lombok.Getter;

/**
 * 业务异常：抛出处指定 HTTP 语义，GlobalExceptionHandler 统一转换
 */
@Getter
public class ApiException extends RuntimeException {

    /** HTTP 状态码，默认 400 */
    private final int httpStatus;
    /** 业务码，默认 40000 */
    private final int code;

    public ApiException(String message) {
        this(400, 40000, message);
    }

    public ApiException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(401, 40100, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(403, 40300, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, 40400, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, 40900, message);
    }
}
