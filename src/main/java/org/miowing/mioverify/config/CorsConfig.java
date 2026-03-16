package org.miowing.mioverify.config;

import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.util.DataUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Collections;

/**
 * 跨域配置，通常在生产环境中启用
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter(DataUtil dataUtil) {
        // 未启用跨域，返回空过滤器
        if (!dataUtil.isCorsEnabled()) {
            return new CorsFilter(new UrlBasedCorsConfigurationSource());
        }

        CorsConfiguration config = new CorsConfiguration();

        String allowedOrigin = dataUtil.getCorsAllowedOrigin();

        // 检查配置是否为空
        if (allowedOrigin == null || allowedOrigin.trim().isEmpty()) {
            log.warn("CORS is enabled, but allowed origin is null or empty");
            return new CorsFilter(new UrlBasedCorsConfigurationSource());
        }

        // 设置允许域
        config.setAllowedOriginPatterns(Collections.singletonList(allowedOrigin.trim()));

        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
