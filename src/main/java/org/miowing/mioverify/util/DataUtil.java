package org.miowing.mioverify.util;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.time.Duration;

/**
 * All properties in application.yml will be injected to this.
 */
@Component
@Data
@Slf4j
public class DataUtil implements InitializingBean {
    @Value("${server.port}")
    private int port;
    @Value("${server.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${server.ssl.http-port}")
    private int httpPort;
    @Value("${mioverify.texture.storage-loc}")
    private Path texturesPath;
    @Value("${mioverify.texture.default-skin-loc}")
    private Path defSkinLoc;
    @Value("${mioverify.props.use-https}")
    private boolean useHttps;
    @Value("${mioverify.props.server-domain}")
    private String serverDomain;
    @Value("${mioverify.security.sign-algorithm}")
    private String signAlgorithm;
    @Value("${mioverify.token.signature}")
    private String tokenSign;
    @Value("${mioverify.token.expire}")
    private Duration tokenExpire;
    @Value("${mioverify.token.invalid}")
    private Duration tokenInvalid;
    @Value("${mioverify.session.expire}")
    private Duration sessionExpire;
    @Value("${mioverify.security.profile-batch-limit}")
    private int profileBatchLimit;
    @Value("${mioverify.extern.register.enabled}")
    private boolean allowRegister;
    @Value("${mioverify.extern.register.allow-user}")
    private boolean allowRegUser;
    @Value("${mioverify.extern.register.allow-profile}")
    private boolean allowRegProfile;
    @Value("${mioverify.extern.register.permission-key.enabled}")
    private boolean usePermKey;
    @Value("${mioverify.extern.register.permission-key.key}")
    private String permKey;
    @Value("${mioverify.extern.register.profile-strict}")
    private boolean profileStrict;
    @Value("${mioverify.extern.multi-profile-name}")
    private boolean multiProfileName;

    // Auth mode: "local" or "oauth"
    @Value("${mioverify.auth-mode}")
    private String authMode;

    // OAuth state expiration
    @Value("${mioverify.oauth.state-expire}")
    private Duration oauthStateExpire;

    // Microsoft OAuth
    @Value("${mioverify.oauth.providers.microsoft.enabled}")
    private boolean microsoftEnabled;
    @Value("${mioverify.oauth.providers.microsoft.client-id}")
    private String microsoftClientId;
    @Value("${mioverify.oauth.providers.microsoft.client-secret}")
    private String microsoftClientSecret;
    @Value("${mioverify.oauth.providers.microsoft.authorization-url}")
    private String microsoftAuthorizationUrl;
    @Value("${mioverify.oauth.providers.microsoft.token-url}")
    private String microsoftTokenUrl;
    @Value("${mioverify.oauth.providers.microsoft.user-info-url}")
    private String microsoftUserInfoUrl;
    @Value("${mioverify.oauth.providers.microsoft.redirect-uri}")
    private String microsoftRedirectUri;
    @Value("${mioverify.oauth.providers.microsoft.scopes}")
    private String microsoftScopes;

    // GitHub OAuth
    @Value("${mioverify.oauth.providers.github.enabled}")
    private boolean githubEnabled;
    @Value("${mioverify.oauth.providers.github.client-id}")
    private String githubClientId;
    @Value("${mioverify.oauth.providers.github.client-secret}")
    private String githubClientSecret;
    @Value("${mioverify.oauth.providers.github.authorization-url}")
    private String githubAuthorizationUrl;
    @Value("${mioverify.oauth.providers.github.token-url}")
    private String githubTokenUrl;
    @Value("${mioverify.oauth.providers.github.user-info-url}")
    private String githubUserInfoUrl;
    @Value("${mioverify.oauth.providers.github.redirect-uri}")
    private String githubRedirectUri;
    @Value("${mioverify.oauth.providers.github.scopes}")
    private String githubScopes;

    // Custom OAuth
    @Value("${mioverify.oauth.providers.custom.enabled}")
    private boolean customEnabled;
    @Value("${mioverify.oauth.providers.custom.name}")
    private String customName;
    @Value("${mioverify.oauth.providers.custom.client-id}")
    private String customClientId;
    @Value("${mioverify.oauth.providers.custom.client-secret}")
    private String customClientSecret;
    @Value("${mioverify.oauth.providers.custom.authorization-url}")
    private String customAuthorizationUrl;
    @Value("${mioverify.oauth.providers.custom.token-url}")
    private String customTokenUrl;
    @Value("${mioverify.oauth.providers.custom.user-info-url}")
    private String customUserInfoUrl;
    @Value("${mioverify.oauth.providers.custom.redirect-uri}")
    private String customRedirectUri;
    @Value("${mioverify.oauth.providers.custom.scopes}")
    private String customScopes;
    @Value("${mioverify.oauth.providers.custom.user-id-field}")
    private String customUserIdField;
    @Value("${mioverify.oauth.providers.custom.username-field}")
    private String customUsernameField;

    public boolean isOAuthMode() {
        return "oauth".equalsIgnoreCase(authMode);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Reading data from application file...");
        log.info("Auth mode: {}", authMode);
    }
}
