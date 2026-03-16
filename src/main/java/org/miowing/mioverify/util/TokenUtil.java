package org.miowing.mioverify.util;

import cn.hutool.core.util.IdUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.miowing.mioverify.pojo.AToken;
import org.miowing.mioverify.pojo.Profile;
import org.miowing.mioverify.pojo.User;
import org.miowing.mioverify.service.ProfileService;
import org.miowing.mioverify.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Operations of tokens.
 */
@Component
public class TokenUtil {
    @Autowired
    private DataUtil dataUtil;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ProfileService profileService;

    //TODO 控制优化
    public static String TOKEN_PREF = "tk_";
    public static String TMARK_PREF = "tm_";
    public static String USERID_PREF = "tv_";

    /**
     * 生成随机的客户端令牌
     *
     * @return 客户端令牌字符串
     */
    public static String genClientToken() {
        return IdUtil.simpleUUID();
        //
    }

    /**
     * 生成访问令牌（JWT）
     * @param user 用户对象
     * @param clientToken 客户端令牌（可为 null，此时自动生成）
     * @param bindProfile 绑定的角色 ID
     * @return JWT 访问令牌
     */
    public String genAccessToken(User user, @Nullable String clientToken, String bindProfile) {
        Date invalidAt = new Date(System.currentTimeMillis() + dataUtil.getTokenInvalid().toMillis());
        return JWT.create()
                .withClaim("wexp", System.currentTimeMillis() + dataUtil.getTokenExpire().toMillis())
                .withClaim("ct", clientToken == null ? genClientToken() : clientToken)
                .withClaim("name", user.getUsername())
                .withClaim("bindp", bindProfile)
                .withExpiresAt(invalidAt)
                .sign(Algorithm.HMAC256(dataUtil.getTokenSign()));
    }

    /**
     * 验证访问令牌的有效性
     * @param accessToken 访问令牌字符串
     * @param clientToken 客户端令牌（可为 null，不校验）
     * @param strictExpire 是否严格检查弱过期时间（wexp）
     * @return 如果验证通过返回 {@link AToken} 对象，否则返回 null
     */
    public @Nullable AToken verifyAccessToken(String accessToken, @Nullable String clientToken, boolean strictExpire) {
        try {
            DecodedJWT dJWT = JWT
                    .require(Algorithm.HMAC256(dataUtil.getTokenSign()))
                    .build()
                    .verify(accessToken);
            String cToken = dJWT.getClaim("ct").asString();
            //check clientToken
            if (clientToken != null && !cToken.equals(clientToken)) {
                return null;
            }
            //check weak expires
            if (strictExpire) {
                long expiresAt = dJWT.getClaim("wexp").asLong();
                if (System.currentTimeMillis() > expiresAt) {
                    return null;
                }
            }
            //If LOGOUT, INVALIDATE or REFRESH occurs (or expires), the token won't be in the redis.
            if (!redisService.checkToken(accessToken)) {
                return null;
            }
            return new AToken(
                    cToken,
                    dJWT.getClaim("name").asString(),
                    dJWT.getClaim("bindp").asString()
            );
        } catch (JWTDecodeException | SignatureVerificationException | TokenExpiredException |
                 AlgorithmMismatchException e) {
            return null;
        }
    }

    /**
     * 从 HTTP 请求中解析当前登录用户的 ID
     * <p>
     * 从 Authorization 头中提取 Bearer 令牌，验证令牌有效性，并通过绑定的角色 ID 获取用户 ID。
     * 令牌必须有效且未过期（严格模式）。
     * </p>
     *
     * @param request HTTP 请求对象
     *
     * @return 当前登录用户的 ID，如果未登录或令牌无效则返回 null
     */
    public @Nullable String getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if ( authHeader == null || ! authHeader.startsWith("Bearer ") ) {
            return null;
        }
        String token = authHeader.substring(7);
        // 验证 token，严格检查过期时间（strictExpire = true）
        AToken aToken = verifyAccessToken(token, null, true);
        if ( aToken == null ) {
            return null;
        }
        // 通过绑定的 Profile ID 获取用户 ID
        Profile profile = profileService.getById(aToken.bindProfile());
        return profile != null ? profile.getBindUser() : null;
    }

}