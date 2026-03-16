package org.miowing.mioverify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.miowing.mioverify.dao.UserDao;
import org.miowing.mioverify.exception.LoginFailedException;
import org.miowing.mioverify.pojo.User;
import org.miowing.mioverify.service.UserService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {
    @Override
    public User getLogin(String username, String password) {
       return getLogin(username, password, false);
        //
    }

    @Override
    public User getLogin(String username, String password, boolean exception) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getUsername, username).eq(User::getPassword, password);
        User user = getOne(lqw);
        if (user == null && exception) {
            throw new LoginFailedException();
        }
        return user;
    }

    @Override
    public @Nullable User getLoginNoPwd(String username) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getUsername, username);
        return getOne(lqw);
    }

    @Override
    public User getByProviderUserId(String provider, String providerUserId) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        switch ( provider.toLowerCase() ) {
            case "github" -> lqw.eq(User :: getGithubId, providerUserId);
            case "microsoft" -> lqw.eq(User :: getMicrosoftId, providerUserId);
            case "mcjpg" -> lqw.eq(User :: getMcjpgId, providerUserId);
            case "custom" -> lqw.eq(User :: getCustomId, providerUserId);
            default -> {
                return null;
            }
        }
        return getOne(lqw);
    }

}