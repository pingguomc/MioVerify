package org.miowing.mioverify.service;

import org.miowing.mioverify.pojo.oauth.OAuthCallbackResp;

public interface OAuthService {

    /**
     * OAuth2 登录成功后的用户处理（由 SecurityConfig.successHandler 调用）。
     * Spring Security 已自动完成 code 换 token 和获取用户信息，
     * 此方法负责 查找/创建本地用户 并签发临时 Token。
     *
     * @param provider         Provider 名称（github / microsoft / mcjpg / custom）
     * @param providerUserId   Provider 返回的用户唯一 ID
     * @param providerUsername Provider 返回的用户名
     *
     * @return OAuthCallbackResp 包含临时 Token 等信息
     */
    OAuthCallbackResp handleOAuthLogin(String provider, String providerUserId, String providerUsername);

    /**
     * 解绑用户与指定 OAuth 提供商的关联
     *
     * @param userId   用户 ID
     * @param provider 提供商名称（如 "microsoft"）
     */
    void unbind(String userId, String provider);

    /**
     * 将 OAuth provider 账号绑定到已有本地用户。
     *
     * @param userId         要绑定到的本地用户 ID（从 nonce 中取出）
     * @param provider       provider 名称
     * @param providerUserId provider 返回的用户唯一 ID
     */
    void handleOAuthBind(String userId, String provider, String providerUserId);

}
