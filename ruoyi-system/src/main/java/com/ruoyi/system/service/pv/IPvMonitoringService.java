package com.ruoyi.system.service.pv;

import java.util.List;
import com.ruoyi.system.domain.pv.PvAlert;
import com.ruoyi.system.domain.pv.PvDashboardSummary;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.domain.pv.PvHourlyYieldRow;
import com.ruoyi.system.domain.pv.PvPowerSeriesPoint;

public interface IPvMonitoringService
{
    PvDashboardSummary getDashboardSummary();

    List<PvPowerSeriesPoint> listPowerSeries();

    int simulateTelemetry(String operator);

    List<PvAlert> selectAlertList(PvAlert query);

    int resolveAlert(Long alertId, String resolvedBy);

    int resolveAllAlerts(String resolvedBy);

    List<PvHourlyYieldRow> listHourlyYieldRows(String startDate, String endDate, Long tagId);

    int cleanupTelemetry(Integer retentionDays);

    int pollGatewayTelemetry(PvGateway gateway);
}
