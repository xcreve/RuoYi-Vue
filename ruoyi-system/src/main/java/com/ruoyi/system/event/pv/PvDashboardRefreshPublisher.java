package com.ruoyi.system.event.pv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;

@Component
public class PvDashboardRefreshPublisher
{
    @Autowired(required = false)
    private ApplicationEventPublisher applicationEventPublisher;

    public void publish(String source)
    {
        if (applicationEventPublisher == null)
        {
            return;
        }
        applicationEventPublisher.publishEvent(
                new PvDashboardRefreshEvent(StringUtils.defaultIfBlank(source, "unknown"), DateUtils.getNowDate()));
    }
}
