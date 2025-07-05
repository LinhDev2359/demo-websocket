package com.wallet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service để mã hóa và giải mã địa chỉ ví
 * Sử dụng thuật toán AES-128 để bảo mật thông tin
 */
@Service
public class CryptoService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    @Value("${wallet.encryption.key:MySecretKey12345}")
    private String encryptionKey;

    /**
     * Mã hóa địa chỉ ví trước khi lưu vào database
     * 
     * @param walletAddress địa chỉ ví gốc
     * @return địa chỉ ví đã được mã hóa dạng Base64
     */
    public String encryptWalletAddress(String walletAddress) {
        try {
            if (walletAddress == null || walletAddress.trim().isEmpty()) {
                return walletAddress;
            }
            
            // Tạo khóa mã hóa
            SecretKeySpec secretKey = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8), ALGORITHM
            );
            
            // Khởi tạo Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            // Mã hóa và chuyển về Base64
            byte[] encryptedBytes = cipher.doFinal(walletAddress.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi mã hóa địa chỉ ví: " + e.getMessage(), e);
        }
    }

    /**
     * Giải mã địa chỉ ví khi lấy từ database
     * 
     * @param encryptedWalletAddress địa chỉ ví đã mã hóa
     * @return địa chỉ ví gốc
     */
    public String decryptWalletAddress(String encryptedWalletAddress) {
        try {
            if (encryptedWalletAddress == null || encryptedWalletAddress.trim().isEmpty()) {
                return encryptedWalletAddress;
            }
            
            // Tạo khóa giải mã
            SecretKeySpec secretKey = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8), ALGORITHM
            );
            
            // Khởi tạo Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            // Giải mã từ Base64
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedWalletAddress);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi giải mã địa chỉ ví: " + e.getMessage(), e);
        }
    }
}