package org.miowing.mioverify.service;

import java.io.InputStream;

/**
 * Texture source service interface for dynamic texture resource management
 */
public interface TextureSourceService {
    
    /**
     * Get texture by hash
     * @param hash texture hash
     * @return texture input stream
     */
    InputStream getTexture(String hash);
    
    /**
     * Get texture by type and hash
     * @param skin true for skin, false for cape
     * @param hash texture hash
     * @return texture input stream
     */
    InputStream getTexture(boolean skin, String hash);
    
    /**
     * Save texture
     * @param skin true for skin, false for cape
     * @param content texture content
     * @param hash texture hash
     */
    void saveTexture(boolean skin, byte[] content, String hash);
    
    /**
     * Delete texture
     * @param skin true for skin, false for cape
     * @param hash texture hash
     * @return true if deleted successfully
     */
    boolean deleteTexture(boolean skin, String hash);
    
    /**
     * Check if texture exists
     * @param hash texture hash
     * @return true if texture exists
     */
    boolean textureExists(String hash);
    
    /**
     * Get default skin
     * @return default skin input stream
     */
    InputStream getDefaultSkin();
    
    /**
     * Get texture source type
     * @return source type (local, http, cloud)
     */
    String getSourceType();
}
