package org.miowing.mioverify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.miowing.mioverify.pojo.OAuthUser;
import org.springframework.lang.Nullable;

public interface OAuthUserService extends IService<OAuthUser> {
    /**
     * Find an existing OAuth binding by provider and provider user ID.
     */
    @Nullable
    OAuthUser getByProviderAndProviderUserId(String provider, String providerUserId);

    /**
     * Find all OAuth bindings for a given local user.
     */
    java.util.List<OAuthUser> getByUserId(String userId);
}
