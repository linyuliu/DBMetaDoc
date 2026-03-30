package com.dbmetadoc.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 主键请求对象。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
public class IdRequest {

    @NotNull(message = "ID不能为空")
    private Long id;
}
