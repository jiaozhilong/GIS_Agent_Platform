package com.jiaozhilong.gisagent.common.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiResponse<T>(String code, String message, String requestId, OffsetDateTime timestamp, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", UUID.randomUUID().toString(), OffsetDateTime.now(), data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>("OK", "created", UUID.randomUUID().toString(), OffsetDateTime.now(), data);
    }

    public static ApiResponse<Void> error(String code, String message, String requestId) {
        return new ApiResponse<>(code, message, requestId, OffsetDateTime.now(), null);
    }
}
