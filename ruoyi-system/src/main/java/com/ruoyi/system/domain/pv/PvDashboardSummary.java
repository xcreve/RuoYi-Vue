package com.ruoyi.system.domain.pv;

import java.math.BigDecimal;

public class PvDashboardSummary
{
    private Long totalStations;

    private BigDecimal totalCapacityMw;

    private Long onlineInverters;

    private Long totalInverters;

    private BigDecimal currentPowerKw;

    private BigDecimal dailyYieldKwh;

    private Long activeAlerts;

    public Long getTotalStations()
    {
        return totalStations;
    }

    public void setTotalStations(Long totalStations)
    {
        this.totalStations = totalStations;
    }

    public BigDecimal getTotalCapacityMw()
    {
        return totalCapacityMw;
    }

    public void setTotalCapacityMw(BigDecimal totalCapacityMw)
    {
        this.totalCapacityMw = totalCapacityMw;
    }

    public Long getOnlineInverters()
    {
        return onlineInverters;
    }

    public void setOnlineInverters(Long onlineInverters)
    {
        this.onlineInverters = onlineInverters;
    }

    public Long getTotalInverters()
    {
        return totalInverters;
    }

    public void setTotalInverters(Long totalInverters)
    {
        this.totalInverters = totalInverters;
    }

    public BigDecimal getCurrentPowerKw()
    {
        return currentPowerKw;
    }

    public void setCurrentPowerKw(BigDecimal currentPowerKw)
    {
        this.currentPowerKw = currentPowerKw;
    }

    public BigDecimal getDailyYieldKwh()
    {
        return dailyYieldKwh;
    }

    public void setDailyYieldKwh(BigDecimal dailyYieldKwh)
    {
        this.dailyYieldKwh = dailyYieldKwh;
    }

    public Long getActiveAlerts()
    {
        return activeAlerts;
    }

    public void setActiveAlerts(Long activeAlerts)
    {
        this.activeAlerts = activeAlerts;
    }
}
