package org.miowing.mioverify.pojo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Configuration for a single OAuth 2 provider.
 */
@Data
@Accessors(chain = true)
public class OAuthProviderConfig {
    private String name;
    private boolean enabled;
    private String clientId;
    private String clientSecret;
    private String authorizationUrl;
    private String tokenUrl;
    private String userInfoUrl;
    private String redirectUri;
    private String scopes;
    /**
     * JSON field name for user ID in user info response.
     */
    private String userIdField = "id";
    /**
     * JSON field name for username in user info response.
     */
    private String usernameField = "name";
}
