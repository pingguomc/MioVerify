package org.miowing.mioverify.service;

import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.exception.TextureNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Texture source manager that handles multiple texture sources
 */
@Service
@Slf4j
public class TextureSourceManager {
    
    @Value("${mioverify.texture.sources.primary:local}")
    private String primarySource;
    
    @Autowired
    private List<TextureSourceService> textureSources;
    
    /**
     * Get primary texture source
     */
    private TextureSourceService getPrimarySource() {
        return textureSources.stream()
                .filter(source -> source.getSourceType().equals(primarySource))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Primary texture source '{}' not found, using first available", primarySource);
                    return textureSources.get(0);
                });
    }
    
    /**
     * Get fallback sources (all sources except primary)
     */
    private List<TextureSourceService> getFallbackSources() {
        return textureSources.stream()
                .filter(source -> !source.getSourceType().equals(primarySource))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get texture with fallback support
     */
    public InputStream getTexture(String hash) {
        // Try primary source first
        try {
            return getPrimarySource().getTexture(hash);
        } catch (TextureNotFoundException e) {
            log.debug("Texture {} not found in primary source {}, trying fallbacks", hash, primarySource);
        }
        
        // Try fallback sources
        for (TextureSourceService fallbackSource : getFallbackSources()) {
            try {
                log.debug("Trying fallback source: {}", fallbackSource.getSourceType());
                return fallbackSource.getTexture(hash);
            } catch (TextureNotFoundException e) {
                log.debug("Texture {} not found in fallback source {}", hash, fallbackSource.getSourceType());
            }
        }
        
        throw new TextureNotFoundException();
    }
    
    /**
     * Get texture by type with fallback support
     */
    public InputStream getTexture(boolean skin, String hash) {
        // Try primary source first
        try {
            return getPrimarySource().getTexture(skin, hash);
        } catch (TextureNotFoundException e) {
            log.debug("Texture {} not found in primary source {}, trying fallbacks", hash, primarySource);
        }
        
        // Try fallback sources
        for (TextureSourceService fallbackSource : getFallbackSources()) {
            try {
                return fallbackSource.getTexture(skin, hash);
            } catch (TextureNotFoundException e) {
                log.debug("Texture {} not found in fallback source {}", hash, fallbackSource.getSourceType());
            }
        }
        
        throw new TextureNotFoundException();
    }
    
    /**
     * Save texture to primary source
     */
    public void saveTexture(boolean skin, byte[] content, String hash) {
        getPrimarySource().saveTexture(skin, content, hash);
    }
    
    /**
     * Delete texture from primary source
     */
    public boolean deleteTexture(boolean skin, String hash) {
        return getPrimarySource().deleteTexture(skin, hash);
    }
    
    /**
     * Check if texture exists in any source
     */
    public boolean textureExists(String hash) {
        return textureSources.stream()
                .anyMatch(source -> source.textureExists(hash));
    }
    
    /**
     * Get default skin with fallback support
     */
    public InputStream getDefaultSkin() {
        // Try primary source first
        try {
            return getPrimarySource().getDefaultSkin();
        } catch (TextureNotFoundException e) {
            log.debug("Default skin not found in primary source {}, trying fallbacks", primarySource);
        }
        
        // Try fallback sources
        for (TextureSourceService fallbackSource : getFallbackSources()) {
            try {
                return fallbackSource.getDefaultSkin();
            } catch (TextureNotFoundException e) {
                log.debug("Default skin not found in fallback source {}", fallbackSource.getSourceType());
            }
        }
        
        throw new TextureNotFoundException();
    }
}
