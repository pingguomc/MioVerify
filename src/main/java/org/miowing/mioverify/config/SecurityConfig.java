package org.miowing.mioverify.config;

import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.pojo.oauth.OAuthCallbackResp;
import org.miowing.mioverify.service.OAuthService;
import org.miowing.mioverify.service.RedisService;
import org.miowing.mioverify.util.DataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * <h1>Spring Security 全局安全配置类</h1>
 *
 * <p>本类承担以下职责：</p>
 * <ul>
 *   <li>禁用 CSRF（项目为无状态 REST API，使用自定义 Token 鉴权）</li>
 *   <li>放行所有接口</li>
 *   <li>根据配置文件中各 Provider 的 {@code enabled} 开关，在 Spring 容器启动阶段
 *       <strong>提前过滤</strong>，仅将已启用的 Provider 注册进
 *       {@link ClientRegistrationRepository}，避免 Spring Boot 自动配置在启动时
 *       请求所有 Provider 的 {@code issuer-uri}（主要是微软）导致启动失败</li>
 *   <li>接管 {@code /oauth/authorize/{provider}} 和 {@code /oauth/callback/{provider}}
 *       两个端点，由 Spring Security 自动完成授权跳转与 code 换 token 流程</li>
 *   <li>OAuth2 登录成功后，通过 {@code successHandler} 调用业务层完成用户查找/创建并签发临时 Token</li>
 * </ul>
 *
 * <h2>与原有账密登录的关系</h2>
 * <p>与原有账密登录（{@code /authserver/**}）完全独立，互不影响。</p>
 *
 * <h2>启动顺序说明</h2>
 * <ol>
 *   <li>{@link #clientRegistrationRepository} Bean 在容器启动时被创建，
 *       仅包含 {@code enabled=true} 的 Provider</li>
 *   <li>Spring Boot 的 {@code OAuth2ClientRegistrationRepositoryConfiguration}
 *       自动配置因检测到已存在同类型 Bean 而跳过，不会再尝试解析任何 Provider 的 (主要是微软)
 *       {@code issuer-uri}</li>
 *   <li>{@link #filterChain} 注入 Repository</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    /** OAuth2 客户端配置，从 {@code application.yml} 自动绑定。包含所有 Provider 的原始配置（无论是否启用）。 */
    @Autowired
    private OAuth2ClientProperties oAuth2ClientProperties;
    @Autowired
    private OAuthService oAuthService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private DataUtil dataUtil;

    /**
     * 创建仅包含已启用 Provider 的 {@link ClientRegistrationRepository} Bean。
     *
     * <p>避免启动时因请求无效的 {@code issuer-uri} 而抛出异常。</p>
     *
     * <ol>
     *   <li>读取 {@link DataUtil} 中各 Provider 的 {@code enabled} 开关</li>
     *   <li>从 {@link OAuth2ClientProperties} 中移除未启用 Provider 的配置</li>
     *   <li>用过滤后的配置构建 {@link InMemoryClientRegistrationRepository}</li>
     * </ol>
     *
     * @return 仅包含已启用 Provider 的 {@link ClientRegistrationRepository}
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        // OAuth 关闭，直接返回 null
        if ( ! dataUtil.isOAuthEnabled() ) {
            log.info("OAuth2 is disabled, skipping all provider registration.");
            return registrationId -> null;
        }

        Map<String, Boolean> enabledMap = Map.of(
                "github", dataUtil.isOAuthGitHubEnabled(),
                "microsoft", dataUtil.isOAuthMicrosoftEnabled(),
                "mcjpg", dataUtil.isOAuthMcjpgEnabled(),
                "custom", dataUtil.isOAuthCustomEnabled()
        );

        // 从原始配置中移除未启用的 Provider，避免触发 issuer-uri 解析
        enabledMap.forEach((id, enabled) -> {
            if ( ! enabled ) {
                oAuth2ClientProperties.getRegistration().remove(id);
                oAuth2ClientProperties.getProvider().remove(id);
                log.info("OAuth2 Provider disabled, skipped: {}", id);
            }
        });

        List<ClientRegistration> activeList = new OAuth2ClientPropertiesMapper(oAuth2ClientProperties)
                .asClientRegistrations()
                .values()
                .stream()
                .peek(r -> log.info("OAuth2 Provider enabled: {}", r.getRegistrationId()))
                .toList();

        if ( activeList.isEmpty() ) {
            log.warn("OAuth2 is enabled but no provider is enabled!");
            return registrationId -> null;
        }

        return new InMemoryClientRegistrationRepository(activeList);
    }

    /**
     * 配置 Spring Security 过滤链。
     *
     * <p>过滤链处理顺序：</p>
     * <ol>
     *   <li>禁用 CSRF</li>
     *   <li>设置无状态 Session（不创建 HttpSession）</li>
     *   <li>放行所有请求</li>
     *   <li>若 OAuth 总开关开启，注册 OAuth2 登录流程</li>
     * </ol>
     *
     * @param http                          Spring Security 的 HttpSecurity 构造器
     * @param clientRegistrationRepository 已过滤的 Provider 注册仓库，由 {@link #clientRegistrationRepository()} 提供
     * @return 构建完成的 SecurityFilterChain
     *
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer :: disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() //放行全部接口
                );

        if ( dataUtil.isOAuthEnabled() ) {

            http.oauth2Login(oauth2 -> oauth2
                    // 接管 /oauth/authorize/{provider}
                    .authorizationEndpoint(auth -> auth
                            .baseUri("/oauth/authorize")
                            .authorizationRequestResolver(buildAuthorizationRequestResolver(clientRegistrationRepository))
                    )
                    // 接管 /oauth/callback/{provider}
                    .redirectionEndpoint(redirect -> redirect
                            .baseUri("/oauth/callback/*")
                    )
                    // 只注册配置文件中 enabled=true 的 Provider
                    .clientRegistrationRepository(clientRegistrationRepository)
                    .successHandler(oAuth2SuccessHandler())
                    // 登录失败
                    .failureHandler((request, response, exception) -> {
                        String frontendErrorUri = dataUtil.getOauthFrontendRedirectUri(); // 可配置
                        String redirectUrl = UriComponentsBuilder.fromHttpUrl(frontendErrorUri)
                                .queryParam("error", exception.getMessage())
                                .build().toUriString();
                        response.setStatus(HttpStatus.FOUND.value());
                        response.setHeader("Location", redirectUrl);
                    })
            );
        }

        return http.build();
    }

    /**
     * OAuth2 登录成功处理器。
     *
     * <p>当 Spring Security 完成以下工作后，本处理器被调用：</p>
     * <ul>
     *   <li>用 code 向 Provider 换取 Access Token</li>
     *   <li>用 Access Token 调用 UserInfo 端点获取用户信息</li>
     * </ul>
     *
     * <p>本处理器负责：</p>
     * <ul>
     *   <li>从 {@link OAuth2User} 中提取 {@code providerUserId} 和 {@code providerUsername}</li>
     *   <li>从 {@code state} 参数中识别是否为账号绑定流程（携带 {@code bindnonce}）</li>
     *   <li>绑定流程：校验 nonce 有效性，调用 {@link OAuthService#handleOAuthBind} 完成绑定</li>
     *   <li>登录流程：调用 {@link OAuthService#handleOAuthLogin} 完成用户查找或创建，
     *       签发临时 Token 并重定向前端</li>
     * </ul>
     *
     * @return {@link AuthenticationSuccessHandler} 实例
     */
    private AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = token.getPrincipal();
            String provider = token.getAuthorizedClientRegistrationId();

            // 不同 Provider 的用户名字段名不同，按优先级取
            String providerUserId = oAuth2User.getName();
            String providerUsername = resolveUsername(oAuth2User);
            String frontendRedirectUri = dataUtil.getOauthFrontendRedirectUri();

            // 从 state 参数里尝试提取 bindNonce
            String state = request.getParameter("state");
            String bindNonce = extractBindNonce(state);

            if ( bindNonce != null ) {
                // 绑定流程
                String userId = redisService.consumeOAuthBindNonce(bindNonce);

                if ( userId == null ) {
                    // nonce 已过期或不存在
                    redirect(response, frontendRedirectUri, "error", "bind_nonce_expired");
                    return;
                }

                try {
                    oAuthService.handleOAuthBind(userId, provider, providerUserId);
                } catch (Exception e) {
                    log.warn("OAuth bind failed: {}", e.getClass().getSimpleName());
                    redirect(response, frontendRedirectUri, "error", "bind_failed");
                    return;
                }

                log.info("OAuth2 bind success: provider={}, userId={}", provider, userId);
                redirect(response, frontendRedirectUri, "bindSuccess", "true");

            } else {
                log.info("OAuth2 authorization success: provider={}, userId={}, username={}",
                        provider, providerUserId, providerUsername);

                // 交给 OAuthService 处理用户查找/创建，返回临时 Token
                OAuthCallbackResp resp = oAuthService.handleOAuthLogin(
                        provider, providerUserId, providerUsername
                );

                // 构建重定向 URL，添加必要参数
                String redirectUrl = UriComponentsBuilder.fromHttpUrl(frontendRedirectUri)
                        .queryParam("tempToken", resp.getTempToken())
                        .queryParam("provider", provider)
                        .queryParam("needProfile", resp.isNeedProfile())
                        .queryParam("userId", resp.getUserId() != null ? resp.getUserId() : "")
                        .build().toUriString();

                // 发送重定向
                response.setStatus(HttpStatus.FOUND.value());
                response.setHeader("Location", redirectUrl);
            }
        };
    }


    /**
     * 构建自定义授权请求解析器。
     *
     * <p>当请求 {@code /oauth/authorize/{provider}?bind_nonce=xxx} 时，
     * 将 {@code bind_nonce} 附加到 OAuth {@code state} 参数中，格式为：</p>
     * <pre>{@code 原始state,bindnonce:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</pre>
     *
     * <p>{@link #oAuth2SuccessHandler()} 收到回调后，通过 {@link #extractBindNonce}
     * 从 {@code state} 中解析出 nonce，识别当前是绑定模式而非登录模式。</p>
     *
     * @param clientRegistrationRepository 已过滤的 Provider 注册仓库
     * @return 配置完成的 {@link OAuth2AuthorizationRequestResolver}
     */
    private OAuth2AuthorizationRequestResolver buildAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth/authorize"
                );

        resolver.setAuthorizationRequestCustomizer(customizer -> {
            // 从当前请求里取 bind_nonce 参数
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if ( attributes == null ) return;

            jakarta.servlet.http.HttpServletRequest request =
                    (jakarta.servlet.http.HttpServletRequest)
                            attributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
            if ( request == null ) return;

            String bindNonce = request.getParameter("bind_nonce");
            if ( bindNonce != null && ! bindNonce.isBlank() ) {
                // 附加到 state 末尾，以逗号分隔
                String originalState = customizer.build().getState();
                customizer.state(originalState + ",bindnonce:" + bindNonce);
            }
        });

        return resolver;
    }


    /**
     * 根据不同 Provider 解析用户名。
     *
     * <p>各 Provider 返回的用户名字段不同，按以下优先级依次尝试：</p>
     * <ol>
     *   <li>{@code login}：GitHub 专用字段</li>
     *   <li>{@code displayName}：MCJPG 专用字段</li>
     *   <li>{@code name}：Microsoft / 其他 OIDC Provider 的通用字段</li>
     * </ol>
     *
     * @param oAuth2User Spring Security 封装的 OAuth2 用户对象
     * @return 解析到的用户名，若所有字段均为空则返回 {@code null}
     */
    private String resolveUsername(OAuth2User oAuth2User) {
        if ( oAuth2User.getAttribute("login") != null )
            return oAuth2User.getAttribute("login"); // GitHub
        if ( oAuth2User.getAttribute("displayName") != null )
            return oAuth2User.getAttribute("displayName"); // MCJPG
        return oAuth2User.getAttribute("name");  // Microsoft / 其他
    }

    /** 从 state 中提取 bindNonce，格式：<pre>{@code 原始state,bindnonce:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</pre> */
    private String extractBindNonce(String state) {
        if ( state == null ) return null;
        for ( String part : state.split(",") ) {
            if ( part.startsWith("bindnonce:") ) {
                return part.substring("bindnonce:".length());
            }
        }
        return null;
    }

    /** 工具方法：重定向并带一个查询参数 */
    private void redirect(
            jakarta.servlet.http.HttpServletResponse response,
            String baseUrl, String key, String value) throws java.io.IOException {

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam(key, value)
                .build().toUriString();
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader("Location", url);
    }

}
