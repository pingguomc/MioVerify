package org.miowing.mioverify.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Slf4j
public class RedisListenerConfig {
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory factory) {
        try {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(factory);
            return container;
        } catch (Exception e) {
            log.warn("Failed to create Redis listener container: " + e.getMessage());
            return new RedisMessageListenerContainer();
        }
    }
}