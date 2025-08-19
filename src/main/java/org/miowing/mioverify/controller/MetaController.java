package org.miowing.mioverify.controller;

import org.miowing.mioverify.pojo.ApiMetadata;
import org.miowing.mioverify.pojo.ServerMeta;
import org.miowing.mioverify.util.ApiDocumentationUtil;
import org.miowing.mioverify.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * API metadata and documentation controller
 */
@RestController
@RequestMapping("/meta")
public class MetaController {
    
    @Autowired
    private Util util;
    
    @Autowired
    private ApiDocumentationUtil apiDocumentationUtil;
    
    /**
     * Get server metadata (Yggdrasil compatible)
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ServerMeta getServerMeta() {
        return util.getServerMeta();
    }
    
    /**
     * Get complete API metadata and documentation
     */
    @GetMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMetadata getApiMetadata() {
        return apiDocumentationUtil.generateApiMetadata();
    }
    
    /**
     * Get API endpoints list
     */
    @GetMapping(value = "/endpoints", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getEndpoints() {
        ApiMetadata metadata = apiDocumentationUtil.generateApiMetadata();
        Map<String, Object> response = new HashMap<>();
        response.put("baseUrl", metadata.getBaseUrl());
        response.put("endpoints", metadata.getEndpoints());
        return response;
    }
    
    /**
     * Get client configuration
     */
    @GetMapping(value = "/client-config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMetadata.ClientConfiguration getClientConfig() {
        return apiDocumentationUtil.generateApiMetadata().getClientConfig();
    }
    
    /**
     * Get server features
     */
    @GetMapping(value = "/features", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getFeatures() {
        ApiMetadata metadata = apiDocumentationUtil.generateApiMetadata();
        Map<String, Object> response = new HashMap<>();
        response.put("features", metadata.getFeatures());
        response.put("version", metadata.getVersion());
        response.put("implementationName", metadata.getImplementationName());
        response.put("implementationVersion", metadata.getImplementationVersion());
        return response;
    }
    
    /**
     * Get API documentation in human-readable format
     */
    @GetMapping(value = "/docs", produces = MediaType.TEXT_HTML_VALUE)
    public String getApiDocs() {
        ApiMetadata metadata = apiDocumentationUtil.generateApiMetadata();
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='zh-CN'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>").append(metadata.getServerName()).append(" - API 文档</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }");
        html.append(".container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        html.append("h1 { color: #333; border-bottom: 3px solid #667eea; padding-bottom: 10px; }");
        html.append("h2 { color: #555; margin-top: 30px; }");
        html.append("h3 { color: #667eea; }");
        html.append(".endpoint { background: #f8f9fa; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #667eea; }");
        html.append(".method { display: inline-block; padding: 2px 8px; border-radius: 3px; color: white; font-weight: bold; margin-right: 10px; }");
        html.append(".get { background: #28a745; } .post { background: #007bff; } .put { background: #ffc107; color: #000; } .delete { background: #dc3545; }");
        html.append(".path { font-family: monospace; background: #e9ecef; padding: 2px 6px; border-radius: 3px; }");
        html.append(".feature { background: #e7f3ff; padding: 10px; margin: 5px 0; border-radius: 5px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        html.append("<h1>").append(metadata.getServerName()).append(" API 文档</h1>");
        
        html.append("<h2>服务器信息</h2>");
        html.append("<p><strong>实现名称:</strong> ").append(metadata.getImplementationName()).append("</p>");
        html.append("<p><strong>版本:</strong> ").append(metadata.getImplementationVersion()).append("</p>");
        html.append("<p><strong>基础URL:</strong> ").append(metadata.getBaseUrl()).append("</p>");
        
        html.append("<h2>功能特性</h2>");
        metadata.getFeatures().forEach((key, value) -> {
            html.append("<div class='feature'>");
            html.append("<strong>").append(key).append(":</strong> ").append(value);
            html.append("</div>");
        });
        
        html.append("<h2>API 端点</h2>");
        metadata.getEndpoints().forEach(endpoint -> {
            html.append("<div class='endpoint'>");
            html.append("<h3>").append(endpoint.getName()).append("</h3>");
            html.append("<p>");
            html.append("<span class='method ").append(endpoint.getMethod().toLowerCase()).append("'>")
                .append(endpoint.getMethod()).append("</span>");
            html.append("<span class='path'>").append(endpoint.getPath()).append("</span>");
            html.append("</p>");
            html.append("<p><strong>描述:</strong> ").append(endpoint.getDescription()).append("</p>");
            if (endpoint.getParameters() != null && !endpoint.getParameters().isEmpty()) {
                html.append("<p><strong>参数:</strong> ").append(String.join(", ", endpoint.getParameters())).append("</p>");
            }
            html.append("<p><strong>响应类型:</strong> ").append(endpoint.getResponseType()).append("</p>");
            html.append("<p><strong>需要认证:</strong> ").append(endpoint.isRequiresAuth() ? "是" : "否").append("</p>");
            html.append("</div>");
        });
        
        html.append("<h2>客户端配置</h2>");
        ApiMetadata.ClientConfiguration clientConfig = metadata.getClientConfig();
        html.append("<p><strong>认证服务器:</strong> ").append(clientConfig.getAuthServerUrl()).append("</p>");
        html.append("<p><strong>会话服务器:</strong> ").append(clientConfig.getSessionServerUrl()).append("</p>");
        html.append("<p><strong>API服务器:</strong> ").append(clientConfig.getApiServerUrl()).append("</p>");
        html.append("<p><strong>材质服务器:</strong> ").append(clientConfig.getTextureServerUrl()).append("</p>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
}
