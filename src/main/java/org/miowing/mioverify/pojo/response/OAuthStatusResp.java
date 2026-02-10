package org.miowing.mioverify.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * Response with OAuth status information.
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthStatusResp {
    private boolean oauthEnabled;
    private Set<String> enabledProviders;
}
