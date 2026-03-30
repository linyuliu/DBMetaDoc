package com.dbmetadoc.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 数据源测试请求对象。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DatasourceTestRequest extends ConnectionRequest {
}
