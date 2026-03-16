package org.miowing.mioverify.pojo.oauth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * OAuth 状态获取响应
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthStatusResp {

    /** OAuth 是否已经启用 */
    private boolean enabled;

    /** Provider 列表 */
    private List<String> providers;

}
