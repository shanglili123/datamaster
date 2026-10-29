package com.datamaster.module.ai.service.profile;

import com.datamaster.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** Versioned authenticated encryption. No built-in key and no plaintext fallback. */
@Component
public class AiModelProfileCipher {
    private static final String PREFIX = "gcm1:";
    private final String configuredKey;
    private final SecureRandom random = new SecureRandom();

    public AiModelProfileCipher(@Value("${ai.model-profiles.encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public String encrypt(String plaintext, Long spaceId) {
        try {
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad(spaceId));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ignored) {
            throw new ServiceException("模型凭据加密不可用，请检查服务端加密密钥配置");
        }
    }

    public String decrypt(String encrypted, Long spaceId) {
        try {
            if (encrypted == null || !encrypted.startsWith(PREFIX)) throw new IllegalArgumentException();
            byte[] payload = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
            if (payload.length < 29) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, payload, 0, 12));
            cipher.updateAAD(aad(spaceId));
            String plaintext = new String(cipher.doFinal(payload, 12, payload.length - 12), StandardCharsets.UTF_8);
            if (plaintext.trim().isEmpty()) throw new IllegalArgumentException();
            return plaintext;
        } catch (Exception ignored) {
            throw new ServiceException("模型凭据解密不可用，请检查服务端加密密钥配置");
        }
    }

    private SecretKeySpec key() {
        byte[] decoded = Base64.getDecoder().decode(configuredKey == null ? "" : configuredKey.trim());
        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) throw new IllegalArgumentException();
        return new SecretKeySpec(decoded, "AES");
    }

    private byte[] aad(Long spaceId) {
        if (spaceId == null || spaceId <= 0) throw new IllegalArgumentException();
        return ("ai_model_profile:" + spaceId).getBytes(StandardCharsets.UTF_8);
    }
}
