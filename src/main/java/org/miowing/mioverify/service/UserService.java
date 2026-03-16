package org.miowing.mioverify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.miowing.mioverify.pojo.User;

public interface UserService extends IService<User> {
    User getLogin(String username, String password);
    User getLogin(String username, String password, boolean exception);
    User getLoginNoPwd(String username);

    /**
     * 按 provider 的用户ID 查询用户（对应各字段的 UNIQUE 查询）
     *
     * @param providerUserId 用户 ID
     *
     * @return {@link User} 实例
     */
    User getByProviderUserId(String provider, String providerUserId);
}