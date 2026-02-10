package org.miowing.mioverify.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.exception.FeatureNotSupportedException;
import org.miowing.mioverify.pojo.OAuthProviderConfig;
import org.miowing.mioverify.pojo.OAuthUserInfo;
import org.miowing.mioverify.util.DataUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Core OAuth 2 service. Manages provider configurations, builds authorization URLs,
 * exchanges authorization codes for tokens, and fetches user info.
 */
@Service
@Slf4j
public class OAuthService implements InitializingBean {
    @Autowired
    private DataUtil dataUtil;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String OAUTH_STATE_PREF = "oauth_state_";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, OAuthProviderConfig> providers = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        // Register Microsoft
        if (dataUtil.isMicrosoftEnabled()) {
            OAuthProviderConfig microsoft = new OAuthProviderConfig()
                    .setName("microsoft")
                    .setEnabled(true)
                    .setClientId(dataUtil.getMicrosoftClientId())
                    .setClientSecret(dataUtil.getMicrosoftClientSecret())
                    .setAuthorizationUrl(dataUtil.getMicrosoftAuthorizationUrl())
                    .setTokenUrl(dataUtil.getMicrosoftTokenUrl())
                    .setUserInfoUrl(dataUtil.getMicrosoftUserInfoUrl())
                    .setRedirectUri(dataUtil.getMicrosoftRedirectUri())
                    .setScopes(dataUtil.getMicrosoftScopes())
                    .setUserIdField("id")
                    .setUsernameField("displayName");
            providers.put("microsoft", microsoft);
            log.info("OAuth provider registered: microsoft");
        }

        // Register GitHub
        if (dataUtil.isGithubEnabled()) {
            OAuthProviderConfig github = new OAuthProviderConfig()
                    .setName("github")
                    .setEnabled(true)
                    .setClientId(dataUtil.getGithubClientId())
                    .setClientSecret(dataUtil.getGithubClientSecret())
                    .setAuthorizationUrl(dataUtil.getGithubAuthorizationUrl())
                    .setTokenUrl(dataUtil.getGithubTokenUrl())
                    .setUserInfoUrl(dataUtil.getGithubUserInfoUrl())
                    .setRedirectUri(dataUtil.getGithubRedirectUri())
                    .setScopes(dataUtil.getGithubScopes())
                    .setUserIdField("id")
                    .setUsernameField("login");
            providers.put("github", github);
            log.info("OAuth provider registered: github");
        }

        // Register Custom
        if (dataUtil.isCustomEnabled()) {
            String name = dataUtil.getCustomName();
            OAuthProviderConfig custom = new OAuthProviderConfig()
                    .setName(name)
                    .setEnabled(true)
                    .setClientId(dataUtil.getCustomClientId())
                    .setClientSecret(dataUtil.getCustomClientSecret())
                    .setAuthorizationUrl(dataUtil.getCustomAuthorizationUrl())
                    .setTokenUrl(dataUtil.getCustomTokenUrl())
                    .setUserInfoUrl(dataUtil.getCustomUserInfoUrl())
                    .setRedirectUri(dataUtil.getCustomRedirectUri())
                    .setScopes(dataUtil.getCustomScopes())
                    .setUserIdField(dataUtil.getCustomUserIdField())
                    .setUsernameField(dataUtil.getCustomUsernameField());
            providers.put(name, custom);
            log.info("OAuth provider registered: {}", name);
        }
    }

    /**
     * Get provider config by name.
     */
    public OAuthProviderConfig getProvider(String providerName) {
        OAuthProviderConfig config = providers.get(providerName.toLowerCase());
        if (config == null || !config.isEnabled()) {
            throw new FeatureNotSupportedException();
        }
        return config;
    }

    /**
     * Get all enabled provider names.
     */
    public java.util.Set<String> getEnabledProviders() {
        return providers.keySet();
    }

    /**
     * Build the authorization URL for a provider. Generates and stores a random state in Redis.
     */
    public String buildAuthorizationUrl(String providerName) {
        OAuthProviderConfig config = getProvider(providerName);
        String state = IdUtil.simpleUUID();

        // Save state in Redis with expiration
        redisTemplate.opsForValue().set(
                OAUTH_STATE_PREF + state,
                providerName,
                dataUtil.getOauthStateExpire()
        );

        String url = config.getAuthorizationUrl()
                + "?client_id=" + encode(config.getClientId())
                + "&redirect_uri=" + encode(config.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + encode(config.getScopes())
                + "&state=" + encode(state);

        log.info("Built OAuth authorization URL for provider: {}", providerName);
        return url;
    }

    /**
     * Validate the state parameter from the callback. Returns the provider name if valid.
     */
    public String validateState(String state) {
        String key = OAUTH_STATE_PREF + state;
        String providerName = redisTemplate.opsForValue().get(key);
        if (providerName == null) {
            throw new FeatureNotSupportedException();
        }
        redisTemplate.delete(key);
        return providerName;
    }

    /**
     * Exchange authorization code for access token, then fetch user info.
     */
    public OAuthUserInfo exchangeCodeAndGetUserInfo(String providerName, String code) {
        OAuthProviderConfig config = getProvider(providerName);
        String accessToken = exchangeCode(config, code);
        return fetchUserInfo(config, accessToken);
    }

    /**
     * Exchange authorization code for an access token.
     */
    private String exchangeCode(OAuthProviderConfig config, String code) {
        try {
            HttpRequest request = HttpRequest.post(config.getTokenUrl())
                    .header("Accept", "application/json")
                    .form("grant_type", "authorization_code")
                    .form("client_id", config.getClientId())
                    .form("client_secret", config.getClientSecret())
                    .form("code", code)
                    .form("redirect_uri", config.getRedirectUri());

            HttpResponse response = request.execute();
            String body = response.body();
            log.debug("Token exchange response for {}: {}", config.getName(), body);

            JsonNode json = objectMapper.readTree(body);

            if (json.has("access_token")) {
                return json.get("access_token").asText();
            }

            log.error("Failed to get access_token from provider {}: {}", config.getName(), body);
            throw new RuntimeException("OAuth token exchange failed for provider: " + config.getName());
        } catch (Exception e) {
            log.error("OAuth token exchange error for provider {}: {}", config.getName(), e.getMessage());
            throw new RuntimeException("OAuth token exchange failed", e);
        }
    }

    /**
     * Fetch user info from the provider using the access token.
     */
    private OAuthUserInfo fetchUserInfo(OAuthProviderConfig config, String accessToken) {
        try {
            HttpResponse response = HttpRequest.get(config.getUserInfoUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .execute();

            String body = response.body();
            log.debug("User info response for {}: {}", config.getName(), body);

            JsonNode json = objectMapper.readTree(body);

            String userId = json.has(config.getUserIdField())
                    ? json.get(config.getUserIdField()).asText()
                    : null;
            String username = json.has(config.getUsernameField())
                    ? json.get(config.getUsernameField()).asText()
                    : null;

            if (userId == null) {
                log.error("User ID field '{}' not found in response from {}", config.getUserIdField(), config.getName());
                throw new RuntimeException("Failed to get user ID from provider: " + config.getName());
            }

            return new OAuthUserInfo()
                    .setProvider(config.getName())
                    .setProviderUserId(userId)
                    .setProviderUsername(username);
        } catch (Exception e) {
            log.error("OAuth user info fetch error for provider {}: {}", config.getName(), e.getMessage());
            throw new RuntimeException("OAuth user info fetch failed", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
