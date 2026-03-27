package com.dbmetadoc.app.service;

import com.dbmetadoc.common.vo.DriverInfoResponse;
import com.dbmetadoc.db.MetadataModules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverServiceTest {

    private final DriverService driverService = new DriverService(MetadataModules.createRegistry());

    @Test
    void shouldReturnAllConfiguredDrivers() {
        List<DriverInfoResponse> drivers = driverService.list();

        assertEquals(5, drivers.size());
        assertTrue(drivers.stream().anyMatch(item -> "MYSQL".equals(item.getType()) && "SELECT 1".equals(item.getTestSql())));
        assertTrue(drivers.stream().anyMatch(item -> "POSTGRESQL".equals(item.getType()) && item.getPgLike()));
        assertTrue(drivers.stream().anyMatch(item -> "ORACLE".equals(item.getType()) && item.getOracleLike()));
        assertTrue(drivers.stream().anyMatch(item ->
                "KINGBASE".equals(item.getType())
                        && item.getDomestic()
                        && item.getPgLike()
                        && item.getMysqlLike()
                        && item.getOracleLike()
                        && item.getTestSql().contains("自动识别兼容模式")
                        && item.getMetadataStrategy().contains("模式探测")));
        assertTrue(drivers.stream().anyMatch(item -> "DAMENG".equals(item.getType()) && "SELECT 1 FROM DUAL".equals(item.getTestSql())));
        assertTrue(drivers.stream().allMatch(item -> assertDescriptor(item)));
    }

    private boolean assertDescriptor(DriverInfoResponse response) {
        assertNotNull(response.getDriverClass());
        assertNotNull(response.getLabel());
        assertNotNull(response.getDefaultPort());
        assertNotNull(response.getMetadataStrategy());
        assertNotNull(response.getSupportsJdbcUrl());
        return true;
    }
}
