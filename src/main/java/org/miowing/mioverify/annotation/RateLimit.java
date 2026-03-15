package org.miowing.mioverify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求频率限制注解
 * 用于保护敏感接口免受暴力破解和滥用攻击
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 时间窗口内允许的最大请求次数
     */
    int maxRequests() default 5;
    
    /**
     * 时间窗口大小（秒）
     */
    int windowSeconds() default 60;
    
    /**
     * 限流的 key 前缀，用于区分不同的接口
     */
    String keyPrefix() default "rate_limit";
    
    /**
     * 限流策略：IP / USER / GLOBAL
     */
    LimitType limitType() default LimitType.IP;
    
    enum LimitType {
        /**
         * 基于 IP 地址限流
         */
        IP,
        /**
         * 基于用户 ID 限流（需要用户已登录）
         */
        USER,
        /**
         * 全局限流
         */
        GLOBAL
    }
}
