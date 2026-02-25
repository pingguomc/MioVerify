package org.miowing.mioverify.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.dao.UserDao;
import org.miowing.mioverify.exception.FeatureNotSupportedException;
import org.miowing.mioverify.exception.InvalidTokenException;
import org.miowing.mioverify.exception.NoProfileException;
import org.miowing.mioverify.exception.UnauthorizedException;
import org.miowing.mioverify.pojo.AToken;
import org.miowing.mioverify.pojo.Profile;
import org.miowing.mioverify.pojo.ProfileShow;
import org.miowing.mioverify.pojo.User;
import org.miowing.mioverify.pojo.oauth.OAuthAuthReq;
import org.miowing.mioverify.pojo.oauth.OAuthProviderListResp;
import org.miowing.mioverify.pojo.oauth.OAuthStatusResp;
import org.miowing.mioverify.pojo.response.AuthResp;
import org.miowing.mioverify.service.OAuthService;
import org.miowing.mioverify.service.ProfileService;
import org.miowing.mioverify.service.RedisService;
import org.miowing.mioverify.service.UserService;
import org.miowing.mioverify.util.DataUtil;
import org.miowing.mioverify.util.TokenUtil;
import org.miowing.mioverify.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OAuth 2.0 控制器
 * 所有端点均以 /oauth 开头，处理第三方登录、绑定、解绑等操作
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
public class OAuthController {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserService userService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private OAuthService oAuthService;

    @Autowired
    private DataUtil dataUtil;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private Util util;

    /**
     * <h2>OAuth 状态查询</h2>
     * 检查 OAuth 是否启用及可用的提供商列表
     */
    @GetMapping("/status")
    public OAuthStatusResp status() {
        if ( ! dataUtil.isOAuthEnabled() ) {
            return new OAuthStatusResp().setEnabled(false).setProviders(null);
        }
        List<String> providers = new ArrayList<>();
        if ( dataUtil.isOAuthMicrosoftEnabled() ) providers.add("microsoft");
        if ( dataUtil.isOAuthGitHubEnabled() ) providers.add("github");
        if ( dataUtil.isOAuthMcjpgEnabled() ) providers.add("mcjpg");
        if ( dataUtil.isOAuthCustomEnabled() ) providers.add("custom");
        return new OAuthStatusResp().setEnabled(true).setProviders(providers);
    }

    /**
     * 用临时 token 换取应用 accessToken（Yggdrasil 风格）
     */
    @PostMapping("/authenticate")
    public AuthResp authenticate(@RequestBody OAuthAuthReq req) {
        if ( ! dataUtil.isOAuthEnabled() ) {
            throw new FeatureNotSupportedException();
        }

        // 消费临时 token
        String userId = redisService.consumeOAuthTempToken(req.getTempToken());
        if ( userId == null ) {
            throw new InvalidTokenException();
        }

        User user = userService.getById(userId);
        if ( user == null ) {
            throw new InvalidTokenException();
        }

        // 检查角色
        List<Profile> aProfiles = profileService.getByUserId(user.getId());
        if ( aProfiles.isEmpty() ) {
            throw new NoProfileException();
        }

        List<ProfileShow> profiles = util.profileToShow(aProfiles, true);
        ProfileShow bindProfile = profiles.get(0);

        String cToken = req.getClientToken() == null ? TokenUtil.genClientToken() : req.getClientToken();
        String aToken = tokenUtil.genAccessToken(user, cToken, bindProfile.getId());
        redisService.saveToken(aToken, user.getId());

        log.info("New login with OAuth came: {}", user.getUsername());

        return new AuthResp()
                .setAccessToken(aToken)
                .setClientToken(cToken)
                .setAvailableProfiles(profiles)
                .setSelectedProfile(bindProfile)
                .setUser(req.getRequestUser() ? util.userToShow(user) : null);
    }

    /**
     * 获取所有支持的 OAuth 提供商及当前用户的绑定状态
     */
    @GetMapping("/providers")
    public OAuthProviderListResp getProviders(@RequestHeader("Authorization") String authorization) {
        if ( ! dataUtil.isOAuthEnabled() ) {
            throw new FeatureNotSupportedException();
        }

        User user = getUserFromAuthorization(authorization);

        List<OAuthProviderListResp.ProviderInfo> providerInfos = new ArrayList<>();

        if ( dataUtil.isOAuthMicrosoftEnabled() ) {
            providerInfos.add(new OAuthProviderListResp.ProviderInfo()
                    .setProvider("microsoft")
                    .setBound(user.getMicrosoftId() != null));
        }
        if ( dataUtil.isOAuthGitHubEnabled() ) {
            providerInfos.add(new OAuthProviderListResp.ProviderInfo()
                    .setProvider("github")
                    .setBound(user.getGithubId() != null));
        }
        if ( dataUtil.isOAuthMcjpgEnabled() ) {
            providerInfos.add(new OAuthProviderListResp.ProviderInfo()
                    .setProvider("mcjpg")
                    .setBound(user.getMcjpgId() != null));
        }
        if ( dataUtil.isOAuthCustomEnabled() ) {
            providerInfos.add(new OAuthProviderListResp.ProviderInfo()
                    .setProvider("custom")
                    .setBound(user.getCustomId() != null));
        }

        return new OAuthProviderListResp().setProviders(providerInfos);
    }

    /**
     * 绑定第三方账号到当前用户（需要登录）
     * <br>
     * 1. 校验用户 Token
     * 2. 生成 bindNonce 存入 Redis
     * 3. 返回带有 bind_nonce 参数的 OAuth 授权 URL
     * 4. 前端拿到 URL 后直接跳转，剩下的全交给 Spring Security
     */
    @PostMapping("/bind/{provider}")
    public ResponseEntity<?> bindProvider(
            @PathVariable String provider,
            @RequestHeader("Authorization") String authorization) {
        if ( ! dataUtil.isOAuthEnabled() ) {
            throw new FeatureNotSupportedException();
        }

        boolean providerEnabled = switch ( provider ) {
            case "github" -> dataUtil.isOAuthGitHubEnabled();
            case "microsoft" -> dataUtil.isOAuthMicrosoftEnabled();
            case "mcjpg" -> dataUtil.isOAuthMcjpgEnabled();
            case "custom" -> dataUtil.isOAuthCustomEnabled();
            default -> false;
        };
        if ( ! providerEnabled ) {
            throw new FeatureNotSupportedException();
        }

        User user = getUserFromAuthorization(authorization);

        String nonce = Util.genUUID();
        redisService.saveOAuthBindNonce(nonce, user.getId());

        String serverUrl = (dataUtil.isUseHttps() ? "https://" : "http://")
                + dataUtil.getServerDomain() + ":" + dataUtil.getPort();
        String authUrl = serverUrl + "/oauth/authorize/" + provider
                + "?bind_nonce=" + nonce;

        log.info("User {} initiate bind for provider: {}", user.getId(), provider);

        return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
    }

    /**
     * 解绑第三方账号（需要登录）
     */
    @DeleteMapping("/bind/{provider}")
    public ResponseEntity<?> unbindProvider(
            @PathVariable String provider,
            @RequestHeader("Authorization") String authorization) {
        if ( ! dataUtil.isOAuthEnabled() ) {
            throw new FeatureNotSupportedException();
        }

        User user = getUserFromAuthorization(authorization);

        oAuthService.unbind(user.getId(), provider); // 取消成功和未绑定均返回 204

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 登出方法
     * //TODO 临时设置，后续更改为更合适的登出方法
     *
     * @see AuthController
     */
    @Deprecated
    @PostMapping("/signout")
    public ResponseEntity<?> signOut(@RequestHeader("Authorization") String authorization) {
        if ( ! dataUtil.isOAuthEnabled() ) {
            throw new FeatureNotSupportedException();
        }

        User user = getUserFromAuthorization(authorization);
        redisService.clearToken(user.getId());
        log.info("Oauth logout: {}", user.getUsername());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    /**
     * 从 Authorization 头提供的 accessToken 解析用户
     *
     * @param authorization Http 请求 Authorization头
     *
     * @return {@link User} 实例
     */
    private @NonNull User getUserFromAuthorization(String authorization) {
        if ( authorization == null || ! authorization.startsWith("Bearer ") ) {
            throw new UnauthorizedException(); // 401
        }

        String accessToken = authorization.substring(7);
        AToken aToken = tokenUtil.verifyAccessToken(accessToken, null, true);

        if ( aToken == null ) {
            throw new UnauthorizedException(); // 401
        }

        User user = userDao.selectOne(new LambdaQueryWrapper<User>().eq(User :: getUsername, aToken.name()));

        if ( user == null ) {
            throw new UnauthorizedException(); // 401
        }

        return user;
    }

}
