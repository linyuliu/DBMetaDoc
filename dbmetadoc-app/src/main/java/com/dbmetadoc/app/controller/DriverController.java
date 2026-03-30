package com.dbmetadoc.app.controller;

import com.dbmetadoc.common.response.R;
import com.dbmetadoc.common.vo.DriverInfoResponse;
import com.dbmetadoc.app.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 数据库驱动控制器。
 *
 * @author mumu
 * @date 2026-03-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public R<List<DriverInfoResponse>> list() {
        return R.ok(driverService.list());
    }
}
