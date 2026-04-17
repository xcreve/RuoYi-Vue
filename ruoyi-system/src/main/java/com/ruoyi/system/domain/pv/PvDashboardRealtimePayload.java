package com.ruoyi.system.domain.pv;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

public class PvDashboardRealtimePayload
{
    private PvDashboardSummary summary;

    private List<PvPowerSeriesPoint> powerSeries;

    private List<PvAlert> alerts;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date pushedAt;

    private String source;

    public PvDashboardSummary getSummary()
    {
        return summary;
    }

    public void setSummary(PvDashboardSummary summary)
    {
        this.summary = summary;
    }

    public List<PvPowerSeriesPoint> getPowerSeries()
    {
        return powerSeries == null ? Collections.emptyList() : powerSeries;
    }

    public void setPowerSeries(List<PvPowerSeriesPoint> powerSeries)
    {
        this.powerSeries = powerSeries;
    }

    public List<PvAlert> getAlerts()
    {
        return alerts == null ? Collections.emptyList() : alerts;
    }

    public void setAlerts(List<PvAlert> alerts)
    {
        this.alerts = alerts;
    }

    public Date getPushedAt()
    {
        return pushedAt;
    }

    public void setPushedAt(Date pushedAt)
    {
        this.pushedAt = pushedAt;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }
}
