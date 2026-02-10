package org.miowing.mioverify.pojo;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * User information returned from an OAuth 2 provider.
 */
@Data
@Accessors(chain = true)
public class OAuthUserInfo {
    private String provider;
    private String providerUserId;
    private @Nullable String providerUsername;
}
