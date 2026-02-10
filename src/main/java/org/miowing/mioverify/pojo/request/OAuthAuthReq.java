package org.miowing.mioverify.pojo.request;

import lombok.Data;
import org.springframework.lang.Nullable;

/**
 * Request for OAuth-based Yggdrasil authenticate.
 * Used after OAuth callback to exchange the temporary token
 * for a standard Yggdrasil access token + client token.
 */
@Data
public class OAuthAuthReq {
    /**
     * The temporary token returned after OAuth callback.
     */
    private String tempToken;
    private @Nullable String clientToken;
    private Boolean requestUser = false;
}
