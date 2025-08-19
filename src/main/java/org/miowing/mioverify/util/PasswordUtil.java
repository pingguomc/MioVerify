package org.miowing.mioverify.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password encryption utility
 */
@Component
public class PasswordUtil {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Encrypt password using BCrypt
     * @param rawPassword raw password
     * @return encrypted password
     */
    public String encryptPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * Verify password
     * @param rawPassword raw password
     * @param encodedPassword encrypted password
     * @return true if password matches
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * Check if password is already encrypted (BCrypt format)
     * @param password password to check
     * @return true if password is encrypted
     */
    public boolean isPasswordEncrypted(String password) {
        return password != null && password.startsWith("$2a$") && password.length() == 60;
    }
}
