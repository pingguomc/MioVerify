package org.miowing.mioverify.util;

import org.miowing.mioverify.pojo.ApiMetadata;
import org.miowing.mioverify.pojo.ServerMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API documentation utility for generating API metadata
 */
@Component
public class ApiDocumentationUtil {
    
    @Autowired
    private Util util;
    
    @Autowired
    private DataUtil dataUtil;
    
    /**
     * Generate complete API metadata
     */
    public ApiMetadata generateApiMetadata() {
        ServerMeta serverMeta = util.getServerMeta();
        String baseUrl = util.getServerURL();
        
        ApiMetadata metadata = new ApiMetadata()
                .setVersion("1.3.0")
                .setServerName(serverMeta.getMeta().getServerName())
                .setImplementationName(serverMeta.getMeta().getImplementationName())
                .setImplementationVersion(serverMeta.getMeta().getImplementationVersion())
                .setBaseUrl(baseUrl)
                .setEndpoints(generateEndpoints())
                .setFeatures(generateFeatures())
                .setClientConfig(generateClientConfig(baseUrl, serverMeta));
        
        return metadata;
    }
    
    /**
     * Generate API endpoints documentation
     */
    private List<ApiMetadata.ApiEndpoint> generateEndpoints() {
        List<ApiMetadata.ApiEndpoint> endpoints = new ArrayList<>();
        
        // Authentication endpoints
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("authenticate")
                .setPath("/authserver/authenticate")
                .setMethod("POST")
                .setDescription("用户登录认证")
                .setParameters(List.of("username", "password", "clientToken", "requestUser"))
                .setResponseType("application/json")
                .setRequiresAuth(false));
        
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("refresh")
                .setPath("/authserver/refresh")
                .setMethod("POST")
                .setDescription("刷新访问令牌")
                .setParameters(List.of("accessToken", "clientToken", "requestUser"))
                .setResponseType("application/json")
                .setRequiresAuth(false));
        
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("validate")
                .setPath("/authserver/validate")
                .setMethod("POST")
                .setDescription("验证访问令牌")
                .setParameters(List.of("accessToken", "clientToken"))
                .setResponseType("application/json")
                .setRequiresAuth(false));
        
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("invalidate")
                .setPath("/authserver/invalidate")
                .setMethod("POST")
                .setDescription("注销访问令牌")
                .setParameters(List.of("accessToken", "clientToken"))
                .setResponseType("application/json")
                .setRequiresAuth(false));
        
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("signout")
                .setPath("/authserver/signout")
                .setMethod("POST")
                .setDescription("用户登出")
                .setParameters(List.of("username", "password"))
                .setResponseType("application/json")
                .setRequiresAuth(false));
        
        // Session endpoints
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("join")
                .setPath("/sessionserver/session/minecraft/join")
                .setMethod("POST")
                .setDescription("加入游戏服务器")
                .setParameters(List.of("accessToken", "selectedProfile", "serverId"))
                .setResponseType("application/json")
                .setRequiresAuth(true));
        
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("hasJoined")
                .setPath("/sessionserver/session/minecraft/hasJoined")
                .setMethod("GET")
                .setDescription("验证玩家是否已加入服务器")
                .setParameters(List.of("username", "serverId"))
                .setResponseType("application/json")
                .setRequiresAuth(false));
        
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("profile")
                .setPath("/sessionserver/session/minecraft/profile/{uuid}")
                .setMethod("GET")
                .setDescription("获取角色信息")
                .setParameters(List.of("uuid", "unsigned"))
                .setResponseType("application/json")
                .setRequiresAuth(false));
        
        // API endpoints
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("profiles")
                .setPath("/api/profiles/minecraft")
                .setMethod("POST")
                .setDescription("批量获取角色信息")
                .setParameters(List.of("names"))
                .setResponseType("application/json")
                .setRequiresAuth(false));
        
        // Texture endpoints
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("uploadTexture")
                .setPath("/api/user/profile/{uuid}/{textureType}")
                .setMethod("PUT")
                .setDescription("上传材质")
                .setParameters(List.of("uuid", "textureType", "model", "file"))
                .setResponseType("application/json")
                .setRequiresAuth(true));
        
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("deleteTexture")
                .setPath("/api/user/profile/{uuid}/{textureType}")
                .setMethod("DELETE")
                .setDescription("删除材质")
                .setParameters(List.of("uuid", "textureType"))
                .setResponseType("application/json")
                .setRequiresAuth(true));
        
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("getTexture")
                .setPath("/texture/hash/{hash}")
                .setMethod("GET")
                .setDescription("获取材质文件")
                .setParameters(List.of("hash"))
                .setResponseType("image/png")
                .setRequiresAuth(false));
        
        endpoints.add(new ApiMetadata.ApiEndpoint()
                .setName("getDefaultSkin")
                .setPath("/texture/skin/default")
                .setMethod("GET")
                .setDescription("获取默认皮肤")
                .setParameters(List.of())
                .setResponseType("image/png")
                .setRequiresAuth(false));
        
        // External endpoints
        if (dataUtil.isAllowRegister()) {
            endpoints.add(new ApiMetadata.ApiEndpoint()
                    .setName("registerUser")
                    .setPath("/extern/register/user")
                    .setMethod("POST")
                    .setDescription("注册用户")
                    .setParameters(List.of("username", "password", "preferredLang", "key"))
                    .setResponseType("application/json")
                    .setRequiresAuth(false));
            
            endpoints.add(new ApiMetadata.ApiEndpoint()
                    .setName("registerProfile")
                    .setPath("/extern/register/profile")
                    .setMethod("POST")
                    .setDescription("注册角色")
                    .setParameters(List.of("profileName", "username", "password", "skinUploadAllow", "capeUploadAllow", "key"))
                    .setResponseType("application/json")
                    .setRequiresAuth(false));
        }
        
        return endpoints;
    }
    
    /**
     * Generate features map
     */
    private Map<String, Object> generateFeatures() {
        Map<String, Object> features = new HashMap<>();
        features.put("registration", dataUtil.isAllowRegister());
        features.put("userRegistration", dataUtil.isAllowRegUser());
        features.put("profileRegistration", dataUtil.isAllowRegProfile());
        features.put("multiProfileName", dataUtil.isMultiProfileName());
        features.put("textureUpload", true);
        features.put("textureDelete", true);
        features.put("adminInterface", true);
        features.put("dynamicTextureSources", true);
        features.put("passwordEncryption", true);
        return features;
    }
    
    /**
     * Generate client configuration
     */
    private ApiMetadata.ClientConfiguration generateClientConfig(String baseUrl, ServerMeta serverMeta) {
        Map<String, Object> clientFeatures = new HashMap<>();
        clientFeatures.put("non_email_login", serverMeta.getMeta().getFeatureNonEmailLogin());
        clientFeatures.put("legacy_skin_api", serverMeta.getMeta().getFeatureLegacySkinApi());
        clientFeatures.put("no_mojang_namespace", serverMeta.getMeta().getFeatureNoMojangNamespace());
        
        return new ApiMetadata.ClientConfiguration()
                .setAuthServerUrl(baseUrl + "/authserver")
                .setSessionServerUrl(baseUrl + "/sessionserver")
                .setApiServerUrl(baseUrl + "/api")
                .setTextureServerUrl(baseUrl + "/texture")
                .setSkinDomains(serverMeta.getSkinDomains())
                .setSignaturePublicKey(serverMeta.getSignaturePublicKey())
                .setFeatures(clientFeatures);
    }
}
