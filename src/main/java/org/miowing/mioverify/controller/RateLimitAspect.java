package org.miowing.mioverify.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.miowing.mioverify.annotation.RateLimit;
import org.miowing.mioverify.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 请求频率限制切面
 * 基于 Redis 实现滑动窗口限流
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    
    private static final String RATE_LIMIT_PREFIX = "mioverify:rate_limit:";
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Around("@annotation(org.miowing.mioverify.annotation.RateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        
        String key = buildKey(rateLimit, method);
        int maxRequests = rateLimit.maxRequests();
        int windowSeconds = rateLimit.windowSeconds();
        
        // 检查并增加请求计数
        if (!tryAcquire(key, maxRequests, windowSeconds)) {
            log.warn("Rate limit exceeded for key: {}", key);
            throw new RateLimitExceededException(windowSeconds);
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 构建限流 key
     */
    private String buildKey(RateLimit rateLimit, Method method) {
        StringBuilder keyBuilder = new StringBuilder(RATE_LIMIT_PREFIX);
        keyBuilder.append(rateLimit.keyPrefix()).append(":");
        keyBuilder.append(method.getDeclaringClass().getSimpleName()).append(":");
        keyBuilder.append(method.getName()).append(":");
        
        switch (rateLimit.limitType()) {
            case IP -> keyBuilder.append(getClientIp());
            case USER -> keyBuilder.append(getCurrentUserId());
            case GLOBAL -> keyBuilder.append("global");
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 尝试获取请求许可（基于 Redis 计数器实现）
     */
    private boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            
            if (currentCount == null) {
                return false;
            }
            
            // 首次请求，设置过期时间
            if (currentCount == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            
            return currentCount <= maxRequests;
        } catch (Exception e) {
            log.error("Rate limit check failed: {}", e.getMessage());
            // Redis 故障时放行请求，避免影响正常服务
            return true;
        }
    }
    
    /**
     * 获取客户端真实 IP 地址
     * 支持代理和 CDN 环境
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        
        HttpServletRequest request = attributes.getRequest();
        
        // 按优先级检查各种可能的 IP 头
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP",  // Cloudflare
            "True-Client-IP",    // Akamai
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 获取当前用户 ID（用于 USER 类型限流）
     */
    private String getCurrentUserId() {
        // 对于未登录的用户，降级为 IP 限流
        // 实际的用户 ID 需要从认证上下文中获取
        return getClientIp();
    }
}
