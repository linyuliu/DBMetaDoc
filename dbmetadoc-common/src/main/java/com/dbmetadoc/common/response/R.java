package com.dbmetadoc.common.response;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.dbmetadoc.common.enums.ResultCode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return ok(ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> ok(String message) {
        return ok(message, null);
    }

    public static <T> R<T> ok(String message, T data) {
        return R.<T>builder()
            .code(ResultCode.SUCCESS.getCode())
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return fail(resultCode, resultCode.getMessage());
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return R.<T>builder()
                .code(resultCode.getCode())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> R<T> fail(Integer code, String message) {
        return R.<T>builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
