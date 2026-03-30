package com.dbmetadoc.app.service;

import com.dbmetadoc.app.properties.PasswordCipherProperties;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordCipherServiceTest {

    @Test
    void shouldEncryptAndDecryptPassword() {
        PasswordCipherProperties properties = new PasswordCipherProperties();
        properties.setPasswordSecret("unit-test-secret");
        PasswordCipherService passwordCipherService = new PasswordCipherService(properties);

        String cipher = passwordCipherService.encrypt("P@ssw0rd!");

        assertNotEquals("P@ssw0rd!", cipher);
        assertEquals("P@ssw0rd!", passwordCipherService.decrypt(cipher));
    }

    @Test
    void shouldThrowBusinessExceptionWhenDecryptingInvalidCipher() {
        PasswordCipherProperties properties = new PasswordCipherProperties();
        properties.setPasswordSecret("unit-test-secret");
        PasswordCipherService passwordCipherService = new PasswordCipherService(properties);

        BusinessException exception = assertThrows(BusinessException.class, () -> passwordCipherService.decrypt("invalid-cipher"));

        assertEquals(ResultCode.DATABASE_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("模板密码解密失败"));
    }
}
