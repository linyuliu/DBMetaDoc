package com.dbmetadoc.app.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.app.properties.PasswordCipherProperties;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 模板密码加解密服务。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordCipherService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String DEFAULT_SECRET = "dbmetadoc-dev-secret-20260328";

    private final PasswordCipherProperties passwordCipherProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText) {
        if (StrUtil.isBlank(plainText)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, buildSecretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.encode(payload);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DATABASE_ERROR, "模板密码加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            return null;
        }
        try {
            byte[] payload = Base64.decode(cipherText);
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不合法");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DATABASE_ERROR, "模板密码解密失败，请检查加密密钥是否变更", e);
        }
    }

    public boolean isUsingDefaultSecret() {
        return DEFAULT_SECRET.equals(StrUtil.blankToDefault(passwordCipherProperties.getPasswordSecret(), DEFAULT_SECRET));
    }

    private SecretKeySpec buildSecretKey() throws Exception {
        String secret = StrUtil.blankToDefault(passwordCipherProperties.getPasswordSecret(), DEFAULT_SECRET);
        if (DEFAULT_SECRET.equals(secret)) {
            log.warn("当前模板密码加密仍在使用默认开发密钥，建议通过 DBMETADOC_PASSWORD_SECRET 覆盖");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}


