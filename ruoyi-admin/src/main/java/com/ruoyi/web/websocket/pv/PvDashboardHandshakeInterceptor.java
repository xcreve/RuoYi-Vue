package com.ruoyi.web.websocket.pv;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;

@Component
public class PvDashboardHandshakeInterceptor implements HandshakeInterceptor
{
    public static final String LOGIN_USER_ATTR = PvDashboardHandshakeInterceptor.class.getName() + ".LOGIN_USER";
    private static final String DASHBOARD_VIEW_PERMISSION = "pv:dashboard:view";

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes)
    {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (StringUtils.isBlank(token))
        {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        LoginUser loginUser = tokenService.getLoginUser(token);
        if (loginUser == null)
        {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        if (!SecurityUtils.hasPermi(loginUser.getPermissions(), DASHBOARD_VIEW_PERMISSION))
        {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        tokenService.verifyToken(loginUser);
        attributes.put(LOGIN_USER_ATTR, loginUser);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception exception)
    {
    }
}
