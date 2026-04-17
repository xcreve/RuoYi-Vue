package com.ruoyi.system.event.pv;

import java.util.Date;

public class PvDashboardRefreshEvent
{
    private final String source;

    private final Date occurredAt;

    public PvDashboardRefreshEvent(String source, Date occurredAt)
    {
        this.source = source;
        this.occurredAt = occurredAt == null ? new Date() : occurredAt;
    }

    public String getSource()
    {
        return source;
    }

    public Date getOccurredAt()
    {
        return occurredAt;
    }
}
