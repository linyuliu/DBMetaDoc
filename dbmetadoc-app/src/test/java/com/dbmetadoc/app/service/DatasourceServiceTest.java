package com.dbmetadoc.app.service;

import com.dbmetadoc.common.dto.DatasourceSaveRequest;
import com.dbmetadoc.common.entity.DatasourceProfile;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.core.MetadataExtractorRegistry;
import com.dbmetadoc.app.repository.DatasourceProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasourceServiceTest {

    @Mock
    private DatasourceProfileRepository datasourceProfileRepository;

    @Mock
    private TargetDatabaseService targetDatabaseService;

    @Mock
    private MetadataExtractorRegistry metadataExtractorRegistry;

    @InjectMocks
    private DatasourceService datasourceService;

    @Test
    void shouldTestConnectionBeforeSavingDatasource() {
        DatasourceSaveRequest request = new DatasourceSaveRequest();
        request.setName("test-mysql");
        request.setDbType("MYSQL");
        request.setHost("127.0.0.1");
        request.setPort(3306);
        request.setDatabase("demo");
        request.setUsername("root");
        request.setPassword("123456");

        when(datasourceProfileRepository.save(any())).thenAnswer(invocation -> {
            DatasourceProfile profile = invocation.getArgument(0);
            profile.setId(1L);
            return profile;
        });
        when(metadataExtractorRegistry.getDriverDescriptor(DatabaseType.MYSQL)).thenReturn(DatabaseType.MYSQL.toDescriptor());

        var response = datasourceService.save(request);

        InOrder inOrder = inOrder(targetDatabaseService, datasourceProfileRepository);
        inOrder.verify(targetDatabaseService).testConnection(any());
        inOrder.verify(datasourceProfileRepository).save(any());
        assertEquals(1L, response.getId());
        assertEquals("MYSQL", response.getDbType());
        assertEquals("com.mysql.cj.jdbc.Driver", response.getDriverClass());
    }
}
