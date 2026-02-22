package org.miowing.mioverify.pojo.oauth;

import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class OAuthState {

    private String provider;
    private boolean bind;         // 是否为绑定模式
    private @Nullable String userId;        // 绑定模式下的当前用户ID
    private String redirectUri;   // 登录成功后重定向地址

}
