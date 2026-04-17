package com.ruoyi.web.controller.pv;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.framework.web.exception.GlobalExceptionHandler;
import com.ruoyi.framework.web.service.PermissionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@Import({ AbstractPvControllerWebMvcTest.MvcTestConfig.class, GlobalExceptionHandler.class })
abstract class AbstractPvControllerWebMvcTest
{
    @MockitoBean
    protected RedisCache redisCache;

    @Autowired
    protected MockMvc mockMvc;

    protected RequestPostProcessor loginUser(String... permissions)
    {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setDeptId(1L);
        user.setUserName("pv-admin");
        user.setPassword("N/A");
        Set<String> permissionSet = new LinkedHashSet<>(Arrays.asList(permissions));
        LoginUser loginUser = new LoginUser(1L, 1L, user, permissionSet);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null,
                loginUser.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    protected MockHttpServletRequestBuilder jsonRequest(HttpMethod method, String url, Object body, Object... uriVariables)
            throws Exception
    {
        MockHttpServletRequestBuilder builder = request(method, url, uriVariables);
        if (body != null)
        {
            builder.contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(body));
        }
        return builder;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity(prePostEnabled = true)
    static class MvcTestConfig
    {
        @Bean(name = "ss")
        PermissionService permissionService()
        {
            return new PermissionService();
        }

        @Bean
        AuthenticationEntryPoint authenticationEntryPoint()
        {
            return (request, response, authException) -> ServletUtils.renderString(response,
                    JSON.toJSONString(AjaxResult.error(HttpStatus.UNAUTHORIZED, "认证失败")), HttpStatus.UNAUTHORIZED);
        }

        @Bean
        AccessDeniedHandler accessDeniedHandler()
        {
            return (request, response, accessDeniedException) -> ServletUtils.renderString(response,
                    JSON.toJSONString(AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权")),
                    HttpStatus.FORBIDDEN);
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint,
                AccessDeniedHandler accessDeniedHandler) throws Exception
        {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .logout(AbstractHttpConfigurer::disable)
                    .anonymous(Customizer.withDefaults())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }
}
