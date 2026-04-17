package com.ruoyi.system.service.pv;

import com.ruoyi.system.domain.pv.PvAlert;

public interface IPvAlertDispatchService
{
    void handleAlertCreated(PvAlert alert);
}
