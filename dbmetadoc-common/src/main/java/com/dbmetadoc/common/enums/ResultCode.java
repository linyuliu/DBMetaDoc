package com.dbmetadoc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 统一结果码枚举。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    VALIDATION_FAILED(422, "参数校验失败"),
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),
    BUSINESS_ERROR(1000, "业务处理失败"),
    DATASOURCE_TEST_FAILED(1101, "数据源连接测试失败"),
    DATASOURCE_NOT_FOUND(1102, "数据源不存在"),
    DATASOURCE_DISABLED(1103, "数据源已禁用"),
    UNSUPPORTED_DATABASE(1104, "暂不支持该数据库类型"),
    DOCUMENT_GENERATE_FAILED(1201, "文档生成失败"),
    METADATA_EXTRACT_FAILED(1202, "元数据抽取失败"),
    METADATA_CACHE_ERROR(1203, "元数据缓存处理失败"),
    DATABASE_ERROR(1204, "数据库操作失败");

    private final Integer code;
    private final String message;
}
