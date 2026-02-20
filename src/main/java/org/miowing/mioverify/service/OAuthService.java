package org.miowing.mioverify.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface OAuthService {
    /**
     * 获取启用的OAuth提供商列表（内部标识）
     */
    List<String> getEnabledProviders();

//    /** TODO
//     * 获取所有支持的OAuth提供商详情
//     */
//    List<OAuthProviderInfo> getSupportedProviders();

    /**
     * 构建授权跳转URL
     */
    String buildAuthorizationUrl(String provider, boolean bind, HttpServletRequest request);

//    /** TODO
//     * 用授权码换取用户信息
//     */
//    OAuthUserInfo exchangeCodeAndGetUserInfo(String provider, String code);
}
