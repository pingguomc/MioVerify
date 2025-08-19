package org.miowing.mioverify.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiMetadata {
    
    private String version;
    private String serverName;
    private String implementationName;
    private String implementationVersion;
    private String baseUrl;
    private List<ApiEndpoint> endpoints;
    private Map<String, Object> features;
    private ClientConfiguration clientConfig;
    
    @Data
    @Accessors(chain = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiEndpoint {
        private String name;
        private String path;
        private String method;
        private String description;
        private List<String> parameters;
        private String responseType;
        private boolean requiresAuth;
    }
    
    @Data
    @Accessors(chain = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientConfiguration {
        private String authServerUrl;
        private String sessionServerUrl;
        private String apiServerUrl;
        private String textureServerUrl;
        private List<String> skinDomains;
        private String signaturePublicKey;
        private Map<String, Object> features;
    }
}
