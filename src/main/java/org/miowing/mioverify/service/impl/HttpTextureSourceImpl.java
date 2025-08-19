package org.miowing.mioverify.service.impl;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.exception.TextureNotFoundException;
import org.miowing.mioverify.service.TextureSourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

/**
 * HTTP remote texture source implementation
 */
@Service
@ConditionalOnProperty(name = "mioverify.texture.sources.http.enabled", havingValue = "true")
@Slf4j
public class HttpTextureSourceImpl implements TextureSourceService {
    
    @Value("${mioverify.texture.sources.http.base-url}")
    private String baseUrl;
    
    @Value("${mioverify.texture.sources.http.timeout:5000}")
    private int timeout;
    
    @Value("${mioverify.texture.sources.http.cache-enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${mioverify.texture.sources.http.cache-duration:1h}")
    private Duration cacheDuration;
    
    private final StringRedisTemplate redisTemplate;
    private static final String CACHE_PREFIX = "texture:http:";
    
    public HttpTextureSourceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public InputStream getTexture(String hash) {
        // Check cache first
        if (cacheEnabled) {
            String cachedData = redisTemplate.opsForValue().get(CACHE_PREFIX + hash);
            if (cachedData != null) {
                byte[] data = java.util.Base64.getDecoder().decode(cachedData);
                return new ByteArrayInputStream(data);
            }
        }
        
        // Try to fetch from HTTP
        try {
            String url = baseUrl + "/hash/" + hash;
            byte[] data = HttpUtil.downloadBytes(url);
            
            // Cache the result
            if (cacheEnabled && data != null) {
                String encodedData = java.util.Base64.getEncoder().encodeToString(data);
                redisTemplate.opsForValue().set(CACHE_PREFIX + hash, encodedData, cacheDuration);
            }
            
            return new ByteArrayInputStream(data);
        } catch (Exception e) {
            log.warn("Failed to fetch texture from HTTP source: {}", e.getMessage());
            throw new TextureNotFoundException();
        }
    }
    
    @Override
    public InputStream getTexture(boolean skin, String hash) {
        // For HTTP source, we use the same method as getTexture(hash)
        // The remote server should handle the skin/cape distinction
        return getTexture(hash);
    }
    
    @Override
    public void saveTexture(boolean skin, byte[] content, String hash) {
        // HTTP source is read-only, cannot save textures
        throw new UnsupportedOperationException("HTTP texture source is read-only");
    }
    
    @Override
    public boolean deleteTexture(boolean skin, String hash) {
        // HTTP source is read-only, cannot delete textures
        return false;
    }
    
    @Override
    public boolean textureExists(String hash) {
        try {
            String url = baseUrl + "/hash/" + hash;
            int responseCode = HttpUtil.createGet(url).timeout(timeout).execute().getStatus();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public InputStream getDefaultSkin() {
        try {
            String url = baseUrl + "/skin/default";
            byte[] data = HttpUtil.downloadBytes(url);
            return new ByteArrayInputStream(data);
        } catch (Exception e) {
            log.warn("Failed to fetch default skin from HTTP source: {}", e.getMessage());
            throw new TextureNotFoundException();
        }
    }
    
    @Override
    public String getSourceType() {
        return "http";
    }
}
