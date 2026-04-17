package com.ruoyi.system.event.pv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.pv.PvAlert;

@Component
public class PvAlertCreatedPublisher
{
    @Autowired(required = false)
    private ApplicationEventPublisher applicationEventPublisher;

    public void publish(PvAlert alert)
    {
        if (applicationEventPublisher == null || alert == null)
        {
            return;
        }
        applicationEventPublisher.publishEvent(new PvAlertCreatedEvent(alert));
    }
}
