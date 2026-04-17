package com.ruoyi.system.event.pv;

import com.ruoyi.system.domain.pv.PvAlert;

public class PvAlertCreatedEvent
{
    private final PvAlert alert;

    public PvAlertCreatedEvent(PvAlert alert)
    {
        this.alert = alert;
    }

    public PvAlert getAlert()
    {
        return alert;
    }
}
