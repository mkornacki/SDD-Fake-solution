package com.acme.reservation.adapters.outbound.pii;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Field-level AES-256-GCM encryption/tokenization service for PII data.
 * Encryption key is externalized via environment variable / config property.
 * GDPR: raw PII values never stored; only tokenized references in domain entities.
 */
@Service
public class PiiEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final SecretKey secretKey;

    public PiiEncryptionService(
            @Value("${reservation.pii.encryption-key:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=}")
            String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "PII encryption key must be 256-bit (32 bytes, Base64-encoded)");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts a plaintext PII value and returns a Base64-encoded token
     * containing IV + ciphertext suitable for storage.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new PiiEncryptionException("Failed to encrypt PII field", e);
        }
    }

    /**
     * Decrypts a Base64-encoded PII token back to plaintext.
     */
    public String decrypt(String token) {
        if (token == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(token);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new PiiEncryptionException("Failed to decrypt PII token", e);
        }
    }

    /**
     * Masks a token for display when caller lacks PII scope.
     * Returns "***" for any non-null value.
     */
    public String mask(String token) {
        return (token != null) ? "***" : null;
    }

    public static class PiiEncryptionException extends RuntimeException {
        public PiiEncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
