package org.miowing.mioverify.pojo.oauth;

import lombok.Data;


/**
 * 手动绑定第三方账号的请求体
 * 用于 POST /oauth/{provider}/bind
 */
@Data
public class OAuthBindReq {

    /** Provider 返回的用户唯一 ID */
    private String providerUserId;

    /** Provider 返回的用户名 */
    private String providerUsername;

}
