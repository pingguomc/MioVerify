package org.miowing.mioverify.pojo.request;

import lombok.Data;

/**
 * Request to register a profile for an OAuth user.
 * The temp token is obtained from the OAuth callback response.
 */
@Data
public class OAuthProfileRegisterReq {
    private String tempToken;
    private String profileName;
    private boolean skinUploadAllow = true;
    private boolean capeUploadAllow = true;
}
