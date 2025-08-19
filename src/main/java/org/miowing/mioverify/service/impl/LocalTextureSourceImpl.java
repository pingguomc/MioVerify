package org.miowing.mioverify.service.impl;

import cn.hutool.core.io.FileUtil;
import org.miowing.mioverify.exception.TextureNotFoundException;
import org.miowing.mioverify.service.TextureSourceService;
import org.miowing.mioverify.util.DataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Local file system texture source implementation
 */
@Service
@ConditionalOnProperty(name = "mioverify.texture.sources.local.enabled", havingValue = "true", matchIfMissing = true)
public class LocalTextureSourceImpl implements TextureSourceService {
    
    @Autowired
    private DataUtil dataUtil;
    
    @Override
    public InputStream getTexture(String hash) {
        // Try skin first, then cape
        Path skinPath = dataUtil.getTexturesPath().resolve("skin").resolve(hash);
        File skinFile = skinPath.toFile();
        
        try {
            return new FileInputStream(skinFile);
        } catch (FileNotFoundException e) {
            Path capePath = dataUtil.getTexturesPath().resolve("cape").resolve(hash);
            File capeFile = capePath.toFile();
            try {
                return new FileInputStream(capeFile);
            } catch (FileNotFoundException e0) {
                throw new TextureNotFoundException();
            }
        }
    }
    
    @Override
    public InputStream getTexture(boolean skin, String hash) {
        Path texturePath = dataUtil.getTexturesPath()
                .resolve(skin ? "skin" : "cape")
                .resolve(hash);
        File textureFile = texturePath.toFile();
        
        try {
            return new FileInputStream(textureFile);
        } catch (FileNotFoundException e) {
            throw new TextureNotFoundException();
        }
    }
    
    @Override
    public void saveTexture(boolean skin, byte[] content, String hash) {
        Path texturePath = dataUtil.getTexturesPath()
                .resolve(skin ? "skin" : "cape")
                .resolve(hash);
        
        FileUtil.writeBytes(content, texturePath.toFile());
    }
    
    @Override
    public boolean deleteTexture(boolean skin, String hash) {
        Path texturePath = dataUtil.getTexturesPath()
                .resolve(skin ? "skin" : "cape")
                .resolve(hash);
        File textureFile = texturePath.toFile();
        
        if (textureFile.exists()) {
            return textureFile.delete();
        }
        return false;
    }
    
    @Override
    public boolean textureExists(String hash) {
        Path skinPath = dataUtil.getTexturesPath().resolve("skin").resolve(hash);
        Path capePath = dataUtil.getTexturesPath().resolve("cape").resolve(hash);
        
        return skinPath.toFile().exists() || capePath.toFile().exists();
    }
    
    @Override
    public InputStream getDefaultSkin() {
        File defaultSkin = dataUtil.getDefSkinLoc().toFile();
        try {
            return new FileInputStream(defaultSkin);
        } catch (FileNotFoundException e) {
            throw new TextureNotFoundException();
        }
    }
    
    @Override
    public String getSourceType() {
        return "local";
    }
}
