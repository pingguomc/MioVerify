package org.miowing.mioverify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.miowing.mioverify.dao.OAuthUserDao;
import org.miowing.mioverify.pojo.OAuthUser;
import org.miowing.mioverify.service.OAuthUserService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OAuthUserServiceImpl extends ServiceImpl<OAuthUserDao, OAuthUser> implements OAuthUserService {
    @Override
    public @Nullable OAuthUser getByProviderAndProviderUserId(String provider, String providerUserId) {
        LambdaQueryWrapper<OAuthUser> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OAuthUser::getProvider, provider)
                .eq(OAuthUser::getProviderUserId, providerUserId);
        return getOne(lqw);
    }

    @Override
    public List<OAuthUser> getByUserId(String userId) {
        LambdaQueryWrapper<OAuthUser> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OAuthUser::getBindUser, userId);
        return list(lqw);
    }
}
