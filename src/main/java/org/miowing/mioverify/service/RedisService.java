package org.miowing.mioverify.service;

public interface RedisService {

    void saveToken(String token, String userId);
    boolean checkToken(String token);
    void removeToken(String token);
    void clearToken(String userId);
    void saveSession(String serverId, String token);

    /**
     * 保存 OAuth 临时令牌，用于后续换取正式的 accessToken。
     * 临时令牌需设置较短的过期时间，防止滥用。
     *
     * @param tempToken 生成的临时令牌（通常为 UUID）
     * @param userId    对应的用户 ID
     */
    void saveOAuthTempToken(String tempToken, String userId);

    /**
     * 消费 OAuth 临时令牌。
     * 根据临时令牌获取用户 ID，并立即删除该记录，确保一次性使用。
     *
     * @param tempToken 待消费的临时令牌
     * @return 若令牌有效且未过期，返回对应的用户 ID；否则返回 null
     */
    String consumeOAuthTempToken(String tempToken);

}