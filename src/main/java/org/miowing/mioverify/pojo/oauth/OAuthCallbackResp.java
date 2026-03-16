package org.miowing.mioverify.pojo.oauth;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 并非 OAuth 回调响应 (临时作为数据中转类)
 * 用于返回临时令牌和用户信息
 */
@Data
@Accessors(chain = true)
public class OAuthCallbackResp {

    private String tempToken;           // 临时令牌，用于后续换取 accessToken
    private String provider;            // 提供商名称 (github/microsoft/mcjpg/custom)
    private String providerUsername;    // 第三方平台上的用户名
    private boolean needProfile;        // 是否需要创建角色（profile）
    private String userId;              // 当 needProfile=true 时返回用户ID，便于前端后续创建角色

}
