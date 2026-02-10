package org.miowing.mioverify.controller;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.exception.FeatureNotSupportedException;
import org.miowing.mioverify.exception.InvalidTokenException;
import org.miowing.mioverify.exception.LoginFailedException;
import org.miowing.mioverify.exception.NoProfileException;
import org.miowing.mioverify.pojo.*;
import org.miowing.mioverify.pojo.request.OAuthAuthReq;
import org.miowing.mioverify.pojo.response.AuthResp;
import org.miowing.mioverify.pojo.response.OAuthCallbackResp;
import org.miowing.mioverify.pojo.response.OAuthStatusResp;
import org.miowing.mioverify.service.*;
import org.miowing.mioverify.util.DataUtil;
import org.miowing.mioverify.util.TokenUtil;
import org.miowing.mioverify.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * OAuth 2 controller. Provides endpoints for:
 * - GET /oauth/status - check if OAuth is enabled and which providers are available
 * - GET /oauth/authorize/{provider} - redirect to OAuth provider's authorization page
 * - GET /oauth/callback/{provider} - handle OAuth callback, return temp token
 * - POST /authserver/oauth-authenticate - exchange temp token for Yggdrasil tokens
 */
@RestController
@Slf4j
public class OAuthController {
    @Autowired
    private DataUtil dataUtil;
    @Autowired
    private OAuthService oAuthService;
    @Autowired
    private OAuthUserService oAuthUserService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private Util util;

    /**
     * Check OAuth status and available providers.
     */
    @GetMapping("/oauth/status")
    public OAuthStatusResp status() {
        return new OAuthStatusResp()
                .setOauthEnabled(dataUtil.isOAuthMode())
                .setEnabledProviders(dataUtil.isOAuthMode() ? oAuthService.getEnabledProviders() : null);
    }

    /**
     * Redirect user to the OAuth provider's authorization page.
     */
    @GetMapping("/oauth/authorize/{provider}")
    public ResponseEntity<?> authorize(@PathVariable String provider) {
        if (!dataUtil.isOAuthMode()) {
            throw new FeatureNotSupportedException();
        }
        String url = oAuthService.buildAuthorizationUrl(provider);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * OAuth callback. Exchanges code for user info, creates or finds the local user,
     * and returns a temporary token for the client to use in oauth-authenticate.
     */
    @GetMapping("/oauth/callback/{provider}")
    public OAuthCallbackResp callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state
    ) {
        if (!dataUtil.isOAuthMode()) {
            throw new FeatureNotSupportedException();
        }

        // Validate state
        String validatedProvider = oAuthService.validateState(state);
        if (!validatedProvider.equalsIgnoreCase(provider)) {
            throw new FeatureNotSupportedException();
        }

        // Exchange code for user info
        OAuthUserInfo userInfo = oAuthService.exchangeCodeAndGetUserInfo(provider, code);
        log.info("OAuth callback from {}: providerUserId={}, username={}",
                provider, userInfo.getProviderUserId(), userInfo.getProviderUsername());

        // Check if this OAuth user already has a binding
        OAuthUser oAuthUser = oAuthUserService.getByProviderAndProviderUserId(
                userInfo.getProvider(), userInfo.getProviderUserId());

        User user;
        boolean isNewUser = false;

        if (oAuthUser != null) {
            // Existing binding - find the local user
            user = userService.getByUserId(oAuthUser.getBindUser());
            if (user == null) {
                throw new LoginFailedException();
            }
            // Update provider username if changed
            if (userInfo.getProviderUsername() != null
                    && !userInfo.getProviderUsername().equals(oAuthUser.getProviderUsername())) {
                oAuthUser.setProviderUsername(userInfo.getProviderUsername());
                oAuthUserService.updateById(oAuthUser);
            }
        } else {
            // New OAuth user - create local user and binding
            isNewUser = true;
            String userId = Util.genUUID();
            String username = userInfo.getProviderUsername() != null
                    ? userInfo.getProviderUsername()
                    : provider + "_" + userInfo.getProviderUserId();

            // Ensure username uniqueness
            if (userService.getLoginNoPwd(username) != null) {
                username = username + "_" + IdUtil.simpleUUID().substring(0, 6);
            }

            user = new User()
                    .setId(userId)
                    .setUsername(username)
                    .setPassword(null)
                    .setAuthType("OAUTH");
            userService.save(user);

            oAuthUser = new OAuthUser()
                    .setId(Util.genUUID())
                    .setProvider(userInfo.getProvider())
                    .setProviderUserId(userInfo.getProviderUserId())
                    .setProviderUsername(userInfo.getProviderUsername())
                    .setBindUser(userId);
            oAuthUserService.save(oAuthUser);

            log.info("Created new OAuth user: {} (provider: {}, providerUserId: {})",
                    username, provider, userInfo.getProviderUserId());
        }

        // Check if user has profiles
        List<Profile> profiles = profileService.getByUserId(user.getId());
        boolean needProfile = profiles.isEmpty();

        // Generate a temporary token stored in Redis
        String tempToken = IdUtil.simpleUUID();
        redisService.saveOAuthTempToken(tempToken, user.getId(), dataUtil.getOauthStateExpire());

        return new OAuthCallbackResp()
                .setTempToken(tempToken)
                .setProvider(provider)
                .setProviderUsername(userInfo.getProviderUsername())
                .setNeedProfile(needProfile)
                .setUserId(needProfile ? user.getId() : null);
    }

    /**
     * OAuth-based Yggdrasil authenticate.
     * Exchange a temp token (from OAuth callback) for standard Yggdrasil accessToken + clientToken.
     */
    @PostMapping("/authserver/oauth-authenticate")
    public AuthResp oauthAuthenticate(@RequestBody OAuthAuthReq req) {
        if (!dataUtil.isOAuthMode()) {
            throw new FeatureNotSupportedException();
        }

        // Consume temp token
        String userId = redisService.consumeOAuthTempToken(req.getTempToken());
        if (userId == null) {
            throw new InvalidTokenException();
        }

        User user = userService.getByUserId(userId);
        if (user == null) {
            throw new InvalidTokenException();
        }

        // Check profiles
        List<Profile> aProfiles = profileService.getByUserId(user.getId());
        if (aProfiles.isEmpty()) {
            throw new NoProfileException();
        }

        List<ProfileShow> profiles = util.profileToShow(aProfiles, true);
        ProfileShow bindProfile = profiles.get(0);

        String cToken = req.getClientToken() == null ? TokenUtil.genClientToken() : req.getClientToken();
        String aToken = tokenUtil.genAccessToken(user, cToken, bindProfile.getId());
        redisService.saveToken(aToken, user.getId());

        log.info("OAuth authenticate success: {}", user.getUsername());

        return new AuthResp()
                .setAccessToken(aToken)
                .setClientToken(cToken)
                .setAvailableProfiles(profiles)
                .setSelectedProfile(bindProfile)
                .setUser(req.getRequestUser() ? util.userToShow(user) : null);
    }
}
