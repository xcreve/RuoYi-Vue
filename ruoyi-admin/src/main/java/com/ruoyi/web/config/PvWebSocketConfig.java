package com.ruoyi.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.ruoyi.web.websocket.pv.PvDashboardHandshakeInterceptor;
import com.ruoyi.web.websocket.pv.PvDashboardWebSocketHandler;

@Configuration
@EnableWebSocket
public class PvWebSocketConfig implements WebSocketConfigurer
{
    @Autowired
    private PvDashboardWebSocketHandler pvDashboardWebSocketHandler;

    @Autowired
    private PvDashboardHandshakeInterceptor pvDashboardHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
    {
        registry.addHandler(pvDashboardWebSocketHandler, "/ws/pv/dashboard")
                .addInterceptors(pvDashboardHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
