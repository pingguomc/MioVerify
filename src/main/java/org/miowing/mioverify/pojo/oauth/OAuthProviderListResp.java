package org.miowing.mioverify.pojo.oauth;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class OAuthProviderListResp {

    private List<ProviderInfo> providers;

    @Data
    @Accessors(chain = true)
    public static class ProviderInfo {

        /** Provider 名称 */
        private String provider;
        /** 当前用户是否已绑定该 Provider */
        private boolean bound;

    }

}