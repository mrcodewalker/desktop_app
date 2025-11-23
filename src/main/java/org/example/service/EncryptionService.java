package org.example.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

/**
 * Service để xử lý hybrid encryption (RSA + AES)
 */
public class EncryptionService {
    private static EncryptionService instance;
    private PublicKey publicKey;
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    private EncryptionService() {
    }
    
    public static EncryptionService getInstance() {
        if (instance == null) {
            instance = new EncryptionService();
        }
        return instance;
    }
    
    /**
     * Lưu public key từ backend
     */
    public void setPublicKey(String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            
            // Thử dùng standard Java crypto trước, nếu không được thì dùng BouncyCastle
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.publicKey = keyFactory.generatePublic(spec);
            } catch (Exception e) {
                // Fallback to BouncyCastle
                KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
                this.publicKey = keyFactory.generatePublic(spec);
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể load public key: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mã hóa dữ liệu bằng RSA (giữ lại cho tương thích)
     */
    public String encrypt(String data) {
        if (publicKey == null) {
            throw new IllegalStateException("Public key chưa được khởi tạo. Vui lòng gọi API lấy public key trước.");
        }
        
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));
            return Base64.toBase64String(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi mã hóa dữ liệu", e);
        }
    }
    
    /**
     * Mã hóa dữ liệu bằng hybrid encryption (RSA + AES)
     * Trả về object chứa encryptedKey, encryptedData, và iv
     */
    public EncryptionResult encryptHybrid(String data) {
        if (publicKey == null) {
            throw new IllegalStateException("Public key chưa được khởi tạo. Vui lòng gọi API lấy public key trước.");
        }
        
        try {
            // Tạo AES key ngẫu nhiên
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey aesKey = keyGenerator.generateKey();
            
            // Tạo IV ngẫu nhiên
            byte[] iv = new byte[16];
            java.security.SecureRandom random = new java.security.SecureRandom();
            random.nextBytes(iv);
            
            // Mã hóa data bằng AES
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
            byte[] encryptedData = aesCipher.doFinal(data.getBytes("UTF-8"));
            
            // Convert AES key bytes thành hex string (backend expect hex string sau khi decrypt RSA)
            String aesKeyHex = bytesToHex(aesKey.getEncoded());
            
            // Mã hóa AES key (hex string) bằng RSA
            // Backend sẽ decrypt RSA để lấy hex string, rồi convert hex string thành bytes
            // Thử dùng standard Java crypto trước
            Cipher rsaCipher;
            try {
                rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            } catch (Exception e) {
                // Fallback to BouncyCastle
                rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            }
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            // Mã hóa hex string của AES key
            byte[] encryptedKey = rsaCipher.doFinal(aesKeyHex.getBytes("UTF-8"));
            
            return new EncryptionResult(
                Base64.toBase64String(encryptedKey),
                Base64.toBase64String(encryptedData),
                bytesToHex(iv)
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi mã hóa dữ liệu", e);
        }
    }
    
    /**
     * Chuyển đổi byte array sang hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    public boolean isPublicKeyLoaded() {
        return publicKey != null;
    }
    
    /**
     * Class để chứa kết quả mã hóa
     */
    public static class EncryptionResult {
        private final String encryptedKey;
        private final String encryptedData;
        private final String iv;
        
        public EncryptionResult(String encryptedKey, String encryptedData, String iv) {
            this.encryptedKey = encryptedKey;
            this.encryptedData = encryptedData;
            this.iv = iv;
        }
        
        public String getEncryptedKey() {
            return encryptedKey;
        }
        
        public String getEncryptedData() {
            return encryptedData;
        }
        
        public String getIv() {
            return iv;
        }
    }
}

