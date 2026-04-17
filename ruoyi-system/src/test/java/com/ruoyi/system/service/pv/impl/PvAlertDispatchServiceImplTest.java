package com.ruoyi.system.service.pv.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.pv.PvAlert;
import com.ruoyi.system.domain.pv.PvAlertRule;
import com.ruoyi.system.mapper.pv.PvMonitoringMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PvAlertDispatchServiceImplTest
{
    @Mock
    private PvMonitoringMapper monitoringMapper;

    @Mock
    private RedisCache redisCache;

    @Mock
    private TaskExecutor taskExecutor;

    @InjectMocks
    private PvAlertDispatchServiceImpl service;

    @Test
    void handleAlertCreatedShouldDispatchMatchingRuleAndSetThrottleKey()
    {
        AtomicInteger dispatchCount = new AtomicInteger();
        enableImmediateTaskExecution();
        service.setAlertChannelTransport((rule, alert) -> {
            dispatchCount.incrementAndGet();
            return true;
        });
        when(monitoringMapper.selectAlertRulesByLevel("warning")).thenReturn(List.of(rule(11L, "warning", 21L, 300, 1)));
        when(redisCache.hasKey(CacheConstants.PV_ALERT_THROTTLE_KEY + "11:warning")).thenReturn(false);

        service.handleAlertCreated(alert("warning", "active"));

        assertEquals(1, dispatchCount.get());
        verify(taskExecutor).execute(any(Runnable.class));
        verify(monitoringMapper).selectAlertRulesByLevel("warning");
        verify(redisCache).setCacheObject(eq(CacheConstants.PV_ALERT_THROTTLE_KEY + "11:warning"), eq("1"), eq(300),
                eq(TimeUnit.SECONDS));
    }

    @Test
    void handleAlertCreatedShouldSkipDispatchWhenThrottleKeyExists()
    {
        AtomicInteger dispatchCount = new AtomicInteger();
        enableImmediateTaskExecution();
        service.setAlertChannelTransport((rule, alert) -> {
            dispatchCount.incrementAndGet();
            return true;
        });
        when(monitoringMapper.selectAlertRulesByLevel("critical"))
                .thenReturn(List.of(rule(12L, "critical", 22L, 180, 1)));
        when(redisCache.hasKey(CacheConstants.PV_ALERT_THROTTLE_KEY + "12:critical")).thenReturn(true);

        service.handleAlertCreated(alert("critical", "active"));

        assertEquals(0, dispatchCount.get());
        verify(redisCache, never()).setCacheObject(anyString(), any(), any(Integer.class), any(TimeUnit.class));
    }

    @Test
    void handleAlertCreatedShouldNotSetThrottleWhenTransportFails()
    {
        AtomicInteger dispatchCount = new AtomicInteger();
        enableImmediateTaskExecution();
        service.setAlertChannelTransport((rule, alert) -> {
            dispatchCount.incrementAndGet();
            return false;
        });
        when(monitoringMapper.selectAlertRulesByLevel("warning")).thenReturn(List.of(rule(13L, "warning", 23L, 60, 1)));
        when(redisCache.hasKey(CacheConstants.PV_ALERT_THROTTLE_KEY + "13:warning")).thenReturn(false);

        service.handleAlertCreated(alert("warning", "active"));

        assertEquals(1, dispatchCount.get());
        verify(redisCache, never()).setCacheObject(anyString(), any(), any(Integer.class), any(TimeUnit.class));
    }

    @Test
    void handleAlertCreatedShouldIgnoreNonActiveAlert()
    {
        service.handleAlertCreated(alert("warning", "resolved"));

        verify(taskExecutor, never()).execute(any(Runnable.class));
        verify(monitoringMapper, never()).selectAlertRulesByLevel(anyString());
    }

    private void enableImmediateTaskExecution()
    {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));
    }

    private PvAlert alert(String level, String status)
    {
        PvAlert alert = new PvAlert();
        alert.setAlertId(1L);
        alert.setLevel(level);
        alert.setStatus(status);
        alert.setContent("逆变器通信中断");
        alert.setSource("GW-01 / INV-01");
        alert.setOccurTime(new Date());
        return alert;
    }

    private PvAlertRule rule(Long ruleId, String level, Long channelId, Integer throttleSec, Integer channelEnabled)
    {
        PvAlertRule rule = new PvAlertRule();
        rule.setRuleId(ruleId);
        rule.setLevel(level);
        rule.setChannelId(channelId);
        rule.setThrottleSec(throttleSec);
        rule.setChannelType("email");
        rule.setTarget("ops@myems.local");
        rule.setChannelEnabled(channelEnabled);
        return rule;
    }
}
