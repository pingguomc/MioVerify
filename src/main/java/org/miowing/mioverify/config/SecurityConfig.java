package org.miowing.mioverify.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.miowing.mioverify.pojo.oauth.OAuthCallbackResp;
import org.miowing.mioverify.service.OAuthService;
import org.miowing.mioverify.util.DataUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <h1>Spring Security 全局安全配置类</h1>
 *
 * <p>本类承担以下职责：</p>
 * <ul>
 *   <li>禁用 CSRF（项目为无状态 REST API，使用自定义 Token 鉴权）</li>
 *   <li>放行所有接口（鉴权逻辑由各 Controller 内部通过 TokenUtil 自行处理）</li>
 *   <li>根据配置文件中的开关，动态注册启用的 OAuth2 Provider</li>
 *   <li>接管 {@code /oauth/authorize/{provider}} 和 {@code /oauth/callback/{provider}} 两个端点，
 *       由 Spring Security 自动完成授权跳转与 code 换 token 流程</li>
 *   <li>OAuth2 登录成功后，通过 {@code successHandler} 调用业务层完成用户查找/创建并签发临时 Token</li>
 * </ul>
 *
 * <p>与原有账密登录（{@code /authserver/**}）完全独立，互不影响。</p>
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    /**
     * Spring 自动装配的 ClientRegistrationRepository。
     * 包含配置文件中 Provider 的注册信息（无论是否启用）。
     */
    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private OAuthService oAuthService;
    @Autowired
    private DataUtil dataUtil;


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
     * @param http Spring Security 的 HttpSecurity 构造器
     *
     * @return 构建完成的 SecurityFilterChain
     *
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer :: disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() //放行全部接口
                );

        if ( dataUtil.isOAuthEnabled() ) {

            http.oauth2Login(oauth2 -> oauth2
                    // 接管 /oauth/authorize/{provider}
                    .authorizationEndpoint(auth -> auth
                            .baseUri("/oauth/authorize")
                    )
                    // 接管 /oauth/callback/{provider}
                    .redirectionEndpoint(redirect -> redirect
                            .baseUri("/oauth/callback/*")
                    )
                    // 只注册配置文件中 enabled=true 的 Provider
                    .clientRegistrationRepository(filteredRepository())
                    .successHandler(oAuth2SuccessHandler())
                    // 登录失败
                    .failureUrl("/oauth/error")
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
     *   <li>用 Access Token ( OAuth 的 Token ) 调用 UserInfo 端点获取用户信息</li>
     * </ul>
     *
     * <p>本处理器负责：</p>
     * <ul>
     *   <li>从 {@link OAuth2User} 中提取 providerUserId 和 providerUsername</li>
     *   <li>调用 {@link org.miowing.mioverify.service.OAuthService#handleOAuthLogin} 完成用户查找或创建</li>
     *   <li>签发临时 Token，返回给前端</li>
     *   <li>前端用临时 Token 调用 {@code POST /oauth/authenticate} 换取正式 accessToken</li>
     * </ul>
     *
     * @return {@link AuthenticationSuccessHandler}  实例
     */
    private AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = token.getPrincipal();
            String provider = token.getAuthorizedClientRegistrationId();

            // 不同 Provider 的用户名字段名不同，按优先级取
            String providerUserId = oAuth2User.getName();
            String providerUsername = resolveUsername(oAuth2User);

            log.info("OAuth2 authorization success: provider={}, userId={}, username={}",
                    provider, providerUserId, providerUsername);

            // 交给 OAuthService 处理用户查找/创建，返回临时 Token
            OAuthCallbackResp resp = oAuthService.handleOAuthLogin(
                    provider, providerUserId, providerUsername
            );

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpStatus.OK.value());
            // 直接复用 OAuthCallbackResp，Jackson 序列化
            response.getWriter().write(new ObjectMapper().writeValueAsString(resp));
        };
    }

    /**
     * 根据不同 Provider 解析用户名。
     *
     * <ul>
     *   <li>GitHub：使用 {@code login} 字段</li>
     *   <li>OIDC（mcjpg / microsoft）：使用 {@code preferred_username} 或 {@code name}</li>
     * </ul>
     *
     * @param oAuth2User Spring Security 封装的 OAuth2 用户对象
     *
     * @return 解析到的用户名，可能为 null
     */
    private String resolveUsername(OAuth2User oAuth2User) {
        if ( oAuth2User.getAttribute("login") != null )
            return oAuth2User.getAttribute("login"); // GitHub
        if ( oAuth2User.getAttribute("displayName") != null )
            return oAuth2User.getAttribute("displayName"); // MCJPG
        return oAuth2User.getAttribute("name");  // Microsoft / 其他
    }

    /**
     * 根据配置文件中各 Provider 的 {@code enabled} 开关，
     * 过滤出实际启用的 Provider，构建新的 {@link ClientRegistrationRepository}。
     *
     * <p>未启用的 Provider 不会参与 OAuth2 流程，
     * 即使配置文件中写了 client-id / client-secret 也不会生效。</p>
     *
     * @return 仅包含已启用 Provider 的 ClientRegistrationRepository
     */
    private ClientRegistrationRepository filteredRepository() {
        Map<String, Boolean> enabledMap = Map.of(
                "github", dataUtil.isOAuthGitHubEnabled(),
                "microsoft", dataUtil.isOAuthMicrosoftEnabled(),
                "mcjpg", dataUtil.isOAuthMcjpgEnabled(),
                "custom", dataUtil.isOAuthCustomEnabled()
        );

        List<ClientRegistration> activeList = enabledMap.entrySet().stream()
                .filter(Map.Entry :: getValue)
                .map(e -> clientRegistrationRepository.findByRegistrationId(e.getKey()))
                .filter(Objects :: nonNull)
                .peek(r -> log.info("OAuth2 Provider enabled: {}", r.getRegistrationId()))
                .toList();

        if ( activeList.isEmpty() ) {
            log.warn("OAuth2 enabled but no provider is enabled!");
            throw new IllegalStateException("OAuth2 enabled but no provider is enabled!");
        }

        return new InMemoryClientRegistrationRepository(activeList);
    }

}
