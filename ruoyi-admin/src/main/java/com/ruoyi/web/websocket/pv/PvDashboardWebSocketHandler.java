package com.ruoyi.web.websocket.pv;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.system.domain.pv.PvDashboardRealtimePayload;

@Component
public class PvDashboardWebSocketHandler extends TextWebSocketHandler
{
    private static final Logger log = LoggerFactory.getLogger(PvDashboardWebSocketHandler.class);
    private static final int SEND_TIMEOUT_MS = 5000;
    private static final int SEND_BUFFER_SIZE_BYTES = 512 * 1024;

    @Autowired
    private PvDashboardPushService dashboardPushService;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception
    {
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(session, SEND_TIMEOUT_MS,
                SEND_BUFFER_SIZE_BYTES);
        sessions.put(session.getId(), decorated);
        sendPayload(decorated, dashboardPushService.buildPayload("websocket.connect"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
    {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception
    {
        sessions.remove(session.getId());
        if (session.isOpen())
        {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public boolean hasSessions()
    {
        return !sessions.isEmpty();
    }

    public void broadcast(PvDashboardRealtimePayload payload)
    {
        if (payload == null || sessions.isEmpty())
        {
            return;
        }
        for (WebSocketSession session : sessions.values())
        {
            sendPayload(session, payload);
        }
    }

    private void sendPayload(WebSocketSession session, PvDashboardRealtimePayload payload)
    {
        if (session == null || payload == null || !session.isOpen())
        {
            return;
        }
        try
        {
            session.sendMessage(new TextMessage(JSON.toJSONString(payload)));
        }
        catch (IOException ex)
        {
            sessions.remove(session.getId());
            log.warn("PV dashboard websocket push failed. sessionId={}, message={}", session.getId(), ex.getMessage());
            try
            {
                session.close(CloseStatus.SERVER_ERROR);
            }
            catch (IOException closeEx)
            {
                log.debug("PV dashboard websocket close failed. sessionId={}, message={}", session.getId(),
                        closeEx.getMessage());
            }
        }
    }
}
