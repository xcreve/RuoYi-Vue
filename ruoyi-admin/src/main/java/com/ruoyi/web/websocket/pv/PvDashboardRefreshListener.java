package com.ruoyi.web.websocket.pv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.ruoyi.system.event.pv.PvDashboardRefreshEvent;

@Component
public class PvDashboardRefreshListener
{
    @Autowired
    private PvDashboardWebSocketHandler dashboardWebSocketHandler;

    @Autowired
    private PvDashboardPushService dashboardPushService;

    @EventListener
    public void onDashboardRefresh(PvDashboardRefreshEvent event)
    {
        if (!dashboardWebSocketHandler.hasSessions())
        {
            return;
        }
        dashboardWebSocketHandler.broadcast(dashboardPushService.buildPayload(event.getSource()));
    }
}
