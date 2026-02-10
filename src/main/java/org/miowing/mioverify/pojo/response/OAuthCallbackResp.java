package org.miowing.mioverify.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Response from the OAuth callback endpoint.
 * Contains a temporary token that the client uses to call the
 * /authserver/oauth-authenticate endpoint.
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthCallbackResp {
    /**
     * Temporary token for the client to exchange for a real Yggdrasil token.
     */
    private String tempToken;
    /**
     * The OAuth provider name.
     */
    private String provider;
    /**
     * The provider username (for display).
     */
    private @Nullable String providerUsername;
    /**
     * Whether the user needs to create/bind a Profile before authenticating.
     */
    private boolean needProfile;
    /**
     * The bound user ID (for profile registration).
     */
    private @Nullable String userId;
}
