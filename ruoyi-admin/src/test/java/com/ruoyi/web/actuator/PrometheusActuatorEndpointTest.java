package com.ruoyi.web.actuator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import com.ruoyi.framework.config.SecurityConfig;
import com.ruoyi.framework.config.properties.PermitAllUrlProperties;
import com.ruoyi.framework.security.filter.JwtAuthenticationTokenFilter;
import com.ruoyi.framework.security.handle.AuthenticationEntryPointImpl;
import com.ruoyi.framework.security.handle.LogoutSuccessHandlerImpl;
import com.ruoyi.framework.web.service.TokenService;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PrometheusActuatorEndpointTest.TestApplication.class,
        properties = {
                "management.endpoints.web.exposure.include=health,info,prometheus",
                "management.endpoint.prometheus.enabled=true",
                "spring.autoconfigure.exclude="
                        + "com.alibaba.druid.spring.boot4.autoconfigure.DruidDataSourceAutoConfigure,"
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
                        + "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,"
                        + "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration" })
@AutoConfigureMockMvc
@AutoConfigureMetrics
class PrometheusActuatorEndpointTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void prometheusEndpointShouldBeAnonymousAndExposeBusinessMetrics() throws Exception
    {
        Counter.builder("pv.telemetry.ingest").tag("source", "simulate").register(meterRegistry).increment();

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/plain")))
                .andExpect(content().string(containsString("pv_telemetry_ingest_total")));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(SecurityConfig.class)
    static class TestApplication
    {
        @Bean
        AuthenticationEntryPointImpl authenticationEntryPoint()
        {
            return new AuthenticationEntryPointImpl();
        }

        @Bean
        JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter()
        {
            return new NoopJwtAuthenticationTokenFilter();
        }

        @Bean
        CorsFilter corsFilter()
        {
            CorsConfiguration config = new CorsConfiguration();
            config.applyPermitDefaultValues();
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", config);
            return new CorsFilter(source);
        }

        @Bean
        PermitAllUrlProperties permitAllUrlProperties()
        {
            PermitAllUrlProperties properties = new PermitAllUrlProperties()
            {
                @Override
                public void afterPropertiesSet()
                {
                    setUrls(Collections.emptyList());
                }
            };
            properties.setUrls(Collections.emptyList());
            return properties;
        }
    }

    static class NoopJwtAuthenticationTokenFilter extends JwtAuthenticationTokenFilter
    {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException
        {
            chain.doFilter(request, response);
        }
    }
}
