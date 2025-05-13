package com.banking.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final int SALT_LENGTH = 32;
    private static final int ITERATIONS = 10000;
    
    // Generate a random salt
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    // Hash password with salt using PBKDF2WithHmacSHA256 (simplified version using SHA-256)
    public static String hashPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltBytes);
            
            // Apply multiple iterations for stronger security
            byte[] hashedPassword = md.digest(password.getBytes());
            for (int i = 0; i < ITERATIONS; i++) {
                md.reset();
                hashedPassword = md.digest(hashedPassword);
            }
            
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    // Verify password against stored hash
    public static boolean verifyPassword(String password, String storedHash, String storedSalt) {
        String generatedHash = hashPassword(password, storedSalt);
        return generatedHash.equals(storedHash);
    }
}