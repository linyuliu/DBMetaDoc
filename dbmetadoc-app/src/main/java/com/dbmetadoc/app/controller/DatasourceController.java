package com.dbmetadoc.app.controller;

import com.dbmetadoc.common.dto.DatasourceSaveRequest;
import com.dbmetadoc.common.dto.DatasourceTestRequest;
import com.dbmetadoc.common.dto.IdRequest;
import com.dbmetadoc.common.response.R;
import com.dbmetadoc.common.vo.DatasourceDetailResponse;
import com.dbmetadoc.app.service.DatasourceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * 数据源管理控制器。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasource")
public class DatasourceController {

    private final DatasourceService datasourceService;

    @GetMapping("/list")
    public R<List<DatasourceDetailResponse>> list() {
        return R.ok(datasourceService.list());
    }

    @GetMapping("/detail")
    public R<DatasourceDetailResponse> detail(@RequestParam @NotNull(message = "数据源ID不能为空") Long id) {
        return R.ok(datasourceService.detail(id));
    }

    @PostMapping("/test")
    public CompletableFuture<R<Void>> test(@Valid @RequestBody DatasourceTestRequest request) {
        return datasourceService.testAsync(request).thenApply(ignored -> R.ok());
    }

    @PostMapping("/save")
    public CompletableFuture<R<DatasourceDetailResponse>> save(@Valid @RequestBody DatasourceSaveRequest request) {
        return datasourceService.saveAsync(request).thenApply(R::ok);
    }

    @PostMapping("/remove")
    public R<Void> remove(@Valid @RequestBody IdRequest request) {
        datasourceService.remove(request.getId());
        return R.ok();
    }
}
