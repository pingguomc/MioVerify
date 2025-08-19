package org.miowing.mioverify.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.service.RedisService;
import org.miowing.mioverify.util.DataUtil;
import org.miowing.mioverify.util.SessionUtil;
import org.miowing.mioverify.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private DataUtil dataUtil;
    @Override
    public void saveToken(String token, String userId) {
        try {
            redisTemplate.opsForValue().set(TokenUtil.TOKEN_PREF + token, userId);
            redisTemplate.opsForValue().set(TokenUtil.TMARK_PREF + token, "", dataUtil.getTokenInvalid());
            redisTemplate.opsForHash().put(TokenUtil.USERID_PREF + userId, token, "");
        } catch (Exception e) {
            log.warn("Failed to save token to Redis: " + e.getMessage());
        }
    }
    @Override
    public boolean checkToken(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(TokenUtil.TOKEN_PREF + token));
        } catch (Exception e) {
            log.warn("Failed to check token in Redis: " + e.getMessage());
            return false;
        }
    }
    @Override
    public void removeToken(String token) {
        try {
            String userId = redisTemplate.opsForValue().get(TokenUtil.TOKEN_PREF + token);
            redisTemplate.delete(TokenUtil.TMARK_PREF + token);
            redisTemplate.delete(TokenUtil.TOKEN_PREF + token);
            HashOperations<String, Object, Object> hops = redisTemplate.opsForHash();
            String userIdP = TokenUtil.USERID_PREF + userId;
            if (hops.size(userIdP) < 2) {
                redisTemplate.delete(userIdP);
            } else {
                hops.delete(userIdP, token);
            }
        } catch (Exception e) {
            log.warn("Failed to remove token from Redis: " + e.getMessage());
        }
    }
    @Override
    public void clearToken(String userId) {
        try {
            String userIdP = TokenUtil.USERID_PREF + userId;
            Set<Object> keys = redisTemplate.opsForHash().keys(userIdP);
            for (Object key : keys) {
                redisTemplate.delete(TokenUtil.TMARK_PREF + key);
                redisTemplate.delete(TokenUtil.TOKEN_PREF + key);
            }
            redisTemplate.delete(userIdP);
        } catch (Exception e) {
            log.warn("Failed to clear tokens from Redis: " + e.getMessage());
        }
    }
    @Override
    public void saveSession(String serverId, String token) {
        try {
            redisTemplate.opsForValue().set(SessionUtil.SESSION_PREF + serverId, token, dataUtil.getSessionExpire());
        } catch (Exception e) {
            log.warn("Failed to save session to Redis: " + e.getMessage());
        }
    }
}