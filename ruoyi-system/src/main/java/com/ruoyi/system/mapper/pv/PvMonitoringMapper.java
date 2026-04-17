package com.ruoyi.system.mapper.pv;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.pv.PvAlert;
import com.ruoyi.system.domain.pv.PvAlertRule;
import com.ruoyi.system.domain.pv.PvDashboardSummary;
import com.ruoyi.system.domain.pv.PvPowerSeriesPoint;

public interface PvMonitoringMapper
{
    PvDashboardSummary selectDashboardSummary();

    List<PvAlert> selectAlertList(PvAlert query);

    List<PvPowerSeriesPoint> selectPowerSeriesPoints(@Param("startTime") Date startTime);

    List<PvAlertRule> selectAlertRulesByLevel(@Param("level") String level);

    int insertAlert(PvAlert alert);

    int resolveAlert(@Param("alertId") Long alertId, @Param("resolvedBy") String resolvedBy,
            @Param("resolvedAt") Date resolvedAt);

    int resolveAllActiveAlerts(@Param("resolvedBy") String resolvedBy, @Param("resolvedAt") Date resolvedAt);
}
