package org.miowing.mioverify.service;

public interface RedisService {
    void saveToken(String token, String userId);
    boolean checkToken(String token);
    void removeToken(String token);
    void clearToken(String userId);
    void saveSession(String serverId, String token);
    /**
     * Save an OAuth temporary token mapping (temp token -> user ID) in Redis.
     */
    void saveOAuthTempToken(String tempToken, String userId, java.time.Duration expire);
    /**
     * Get the user ID for an OAuth temporary token, then remove it.
     */
    String consumeOAuthTempToken(String tempToken);
}
