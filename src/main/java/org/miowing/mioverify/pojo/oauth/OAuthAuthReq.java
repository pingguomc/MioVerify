package org.miowing.mioverify.pojo.oauth;

import lombok.Data;
import org.springframework.lang.Nullable;

/**
 * 用临时 Token 换取正式 accessToken 的请求体
 * 用于 POST /oauth/authenticate
 */
@Data
public class OAuthAuthReq {

    /** OAuth 登录回调后返回的临时 Token */
    private String tempToken;

    /** 客户端 Token，为空时由服务端生成 */
    private @Nullable String clientToken;

    /** 是否在响应中返回用户信息 */
    private boolean requestUser;

}
