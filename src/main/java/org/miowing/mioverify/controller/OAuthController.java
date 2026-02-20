package org.miowing.mioverify.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.exception.FeatureNotSupportedException;
import org.miowing.mioverify.service.ProfileService;
import org.miowing.mioverify.service.RedisService;
import org.miowing.mioverify.service.UserService;
import org.miowing.mioverify.util.DataUtil;
import org.miowing.mioverify.util.TokenUtil;
import org.miowing.mioverify.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth 2.0 / OIDC 相关控制器
 * 所有端点均以 /oauth 开头，处理第三方登录、绑定、解绑等操作
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
public class OAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private DataUtil dataUtil;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private Util util;


    /**
     * 跳转到 OAuth 提供商授权页
     * 可选参数 bind=true 表示此次授权用于绑定到当前已登录账号
     */
    @GetMapping("/authorize/{provider}")
    public ResponseEntity<?> authorize(
            @PathVariable String provider,
            @RequestParam(value = "bind", defaultValue = "false") boolean bind,
            HttpServletRequest request) {
        if (!dataUtil.isOAuthMode()) {
            throw new FeatureNotSupportedException();
        }
        // 构建授权 URL，将 bind 状态和当前登录用户信息存入 state TODO
//        String url = OAuthService.buildAuthorizationUrl(provider, bind, request);
        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }



}
