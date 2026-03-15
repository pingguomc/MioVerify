package org.miowing.mioverify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.dao.UserDao;
import org.miowing.mioverify.exception.DuplicateUserNameException;
import org.miowing.mioverify.exception.FeatureNotSupportedException;
import org.miowing.mioverify.pojo.User;
import org.miowing.mioverify.pojo.oauth.OAuthCallbackResp;
import org.miowing.mioverify.service.OAuthService;
import org.miowing.mioverify.service.ProfileService;
import org.miowing.mioverify.service.RedisService;
import org.miowing.mioverify.service.UserService;
import org.miowing.mioverify.util.DataUtil;
import org.miowing.mioverify.util.TokenUtil;
import org.miowing.mioverify.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.BiConsumer;

@Service
@Slf4j
public class OAuthServiceImpl implements OAuthService {

    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private DataUtil dataUtil;
    @Autowired
    private Util util;

    @Autowired
    private RedisService redisService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserDao userDao;


    /** Provider 与数据库字段设置器的映射（用于设置对应的 provider ID） */
    private static final java.util.Map<String, BiConsumer<User, String>> PROVIDER_SETTER = java.util.Map.of(
            "github", User :: setGithubId,
            "microsoft", User :: setMicrosoftId,
            "mcjpg", User :: setMcjpgId,
            "custom", User :: setCustomId
    );

    /** Provider 与数据库字段名的映射（用于查询） */
    private static final java.util.Map<String, String> PROVIDER_FIELD = java.util.Map.of(
            "github", "github_id",
            "microsoft", "microsoft_id",
            "mcjpg", "mcjpg_id",
            "custom", "custom_id"
    );


    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuthCallbackResp handleOAuthLogin(
            @NonNull String provider, @NonNull String providerUserId, String providerUsername) {

        // 参数校验
        BiConsumer<User, String> setter = PROVIDER_SETTER.get(provider);
        String fieldName = PROVIDER_FIELD.get(provider);
        if ( setter == null || fieldName == null ) {
            throw new FeatureNotSupportedException(); // 不支持的 OAuth 提供商
        }
        if ( providerUserId.isBlank() ) {
            throw new IllegalArgumentException(); // 提供商用户 ID 不能为空
        }

        User user = userDao.selectOne(
                new QueryWrapper<User>().eq(fieldName, providerUserId)
        );

        boolean isNewUser = (user == null);
        if ( isNewUser ) {
            user = createUser(provider, providerUserId, providerUsername, setter);
        }

        boolean hasProfile = ! profileService.getByUserId(user.getId()).isEmpty();
        boolean needProfile = isNewUser || ! hasProfile;

        // 生成临时令牌并存入 Redis
        String tempToken = Util.genUUID();
        redisService.saveOAuthTempToken(tempToken, user.getId());

        return new OAuthCallbackResp()
                .setTempToken(tempToken)
                .setProvider(provider)
                .setProviderUsername(providerUsername)
                .setNeedProfile(needProfile)
                .setUserId(needProfile ? user.getId() : null); // 需要创建 Profile 时返回 userId
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(@NonNull String userId, @NonNull String provider) {
        BiConsumer<User, String> setter = PROVIDER_SETTER.get(provider);
        if ( setter == null ) {
            throw new FeatureNotSupportedException(); // 不支持的 OAuth 提供商
        }

        User user = userDao.selectById(userId);

        if ( user == null ) {
            throw new FeatureNotSupportedException(); // 用户不存在
        }

        // 检查是否为唯一认证方式（有密码或有其他绑定的 Provider）
        boolean hasPassword = user.getPassword() != null && ! user.getPassword().isBlank();
        int boundProviderCount = countBoundProviders(user);

        // 如果没有密码且只有一个绑定的 Provider，则不允许解绑
        if ( ! hasPassword && boundProviderCount <= 1 ) {
            throw new FeatureNotSupportedException(); // 无法解绑唯一的认证方式
        }

        // 清空对应的 provider ID
        setter.accept(user, null);
        userDao.updateById(user);

        log.info("User: {} ,Unbind oauth provider: {}", userId, provider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleOAuthBind(String userId, String provider, String providerUserId) {

        BiConsumer<User, String> setter = PROVIDER_SETTER.get(provider);
        String fieldName = PROVIDER_FIELD.get(provider);

        if ( setter == null || fieldName == null ) {
            throw new FeatureNotSupportedException();
        }

        // 检查该第三方账号是否已被其他本地账号绑定
        User existing = userDao.selectOne(
                new QueryWrapper<User>().eq(fieldName, providerUserId)
        );
        if ( existing != null && ! existing.getId().equals(userId) ) {
            throw new DuplicateUserNameException(); // 已被其他账号绑定
        }

        User user = userDao.selectById(userId);
        if ( user == null ) {
            throw new FeatureNotSupportedException();
        }

        setter.accept(user, providerUserId);
        userDao.updateById(user);

        log.info("Oauth User {} bind provider {} -> {}", userId, provider, providerUserId);
    }

    //-----------

    /** 创建新用户 */
    private User createUser(String provider, String providerUserId, String providerUsername,
                            BiConsumer<User, String> setter) {

        User newUser = new User();
        newUser.setId(Util.genUUID()); // 使用工具类生成 ID

        // 用户名处理：优先使用提供商返回的用户名，否则生成默认名
        String baseUsername = (providerUsername != null && ! providerUsername.isBlank())
                ? providerUsername
                : "user_" + providerUserId.substring(0, Math.min(8, providerUserId.length()));

        // 检查用户名是否已存在，如存在则添加随机后缀
        String username = ensureUniqueUsername(baseUsername);

        newUser.setUsername(username);

        // 设置对应的 provider ID
        setter.accept(newUser, providerUserId);

        log.info("New Oauth user register: {}", newUser.getUsername());
        userService.save(newUser);
        return newUser;
    }

    /** 统计用户已绑定的 Provider 数量 */
    private int countBoundProviders(User user) {
        int count = 0;
        if ( user.getGithubId() != null && ! user.getGithubId().isBlank() ) count++;
        if ( user.getMicrosoftId() != null && ! user.getMicrosoftId().isBlank() ) count++;
        if ( user.getMcjpgId() != null && ! user.getMcjpgId().isBlank() ) count++;
        if ( user.getCustomId() != null && ! user.getCustomId().isBlank() ) count++;
        return count;
    }

    /** 确保用户名唯一，如果已存在则添加随机后缀 */
    private String ensureUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int maxAttempts = 10;

        for ( int i = 0; i < maxAttempts; i++ ) {
            User existing = userDao.selectOne(
                    new QueryWrapper<User>().eq("username", username)
            );
            if ( existing == null ) {
                return username; // 用户名可用
            }
            // 用户名已存在，添加随机后缀
            username = baseUsername + "_" + Util.genUUID().substring(0, 6);
        }

        // 极端情况：多次尝试后仍冲突，使用 UUID
        return baseUsername + "_" + Util.genUUID();
    }

}
