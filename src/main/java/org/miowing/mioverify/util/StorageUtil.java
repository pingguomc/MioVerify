package org.miowing.mioverify.util;

import org.miowing.mioverify.service.TextureSourceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.InputStream;

/**
 * Storage manager of textures with dynamic source support
 */
@Component
public class StorageUtil {

    @Autowired
    private TextureSourceManager textureSourceManager;
    public void saveTexture(boolean skin, byte[] content, String hash) {
        textureSourceManager.saveTexture(skin, content, hash);
    }

    public boolean deleteTexture(boolean skin, String hash) {
        return textureSourceManager.deleteTexture(skin, hash);
    }

    public InputStream getTexture(boolean skin, String hash) {
        return textureSourceManager.getTexture(skin, hash);
    }

    public InputStream getTexture(String hash) {
        return textureSourceManager.getTexture(hash);
    }

    public InputStream getDefaultSkin() {
        return textureSourceManager.getDefaultSkin();
    }

    public boolean textureExists(String hash) {
        return textureSourceManager.textureExists(hash);
    }
}