package com.asg.spindleserp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private ApiResponse.Obj<T> obj;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Obj<T> {
        private T defaultData;
    }

    public static <T> ApiResponse<T> ok(String msg, T data) {
        return ApiResponse.<T>builder()
                .success(true).message(msg)
                .obj(ApiResponse.Obj.<T>builder().defaultData(data).build())
                .build();
    }

    public static <T> ApiResponse<T> error(String msg) {
        return ApiResponse.<T>builder().success(false).message(msg).build();
    }
}
