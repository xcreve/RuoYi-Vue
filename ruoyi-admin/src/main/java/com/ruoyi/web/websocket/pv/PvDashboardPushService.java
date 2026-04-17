package com.ruoyi.web.websocket.pv;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.pv.PvAlert;
import com.ruoyi.system.domain.pv.PvDashboardRealtimePayload;
import com.ruoyi.system.service.pv.IPvMonitoringService;

@Service
public class PvDashboardPushService
{
    private static final int DASHBOARD_ALERT_LIMIT = 6;

    @Autowired
    private IPvMonitoringService monitoringService;

    public PvDashboardRealtimePayload buildPayload(String source)
    {
        PvAlert alertQuery = new PvAlert();
        alertQuery.setStatus("active");
        List<PvAlert> alerts = new ArrayList<>(monitoringService.selectAlertList(alertQuery));
        if (alerts.size() > DASHBOARD_ALERT_LIMIT)
        {
            alerts = new ArrayList<>(alerts.subList(0, DASHBOARD_ALERT_LIMIT));
        }

        PvDashboardRealtimePayload payload = new PvDashboardRealtimePayload();
        payload.setSummary(monitoringService.getDashboardSummary());
        payload.setPowerSeries(monitoringService.listPowerSeries());
        payload.setAlerts(alerts);
        payload.setSource(source);
        payload.setPushedAt(DateUtils.getNowDate());
        return payload;
    }
}
