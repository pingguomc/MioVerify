package org.miowing.mioverify.pojo.oauth;

import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class OAuthUserInfo {

    private String provider;                    // 提供商名称
    private String providerUserId;              // 提供商返回的唯一用户ID
    private @Nullable String providerUsername;  // 提供商返回的用户名
    private @Nullable String email;             // 邮箱

}
