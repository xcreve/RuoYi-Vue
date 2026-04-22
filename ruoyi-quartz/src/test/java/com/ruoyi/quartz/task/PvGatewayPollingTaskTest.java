package com.ruoyi.quartz.task;

import java.util.Date;
import java.util.List;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.mapper.pv.PvAssetMapper;
import com.ruoyi.system.metrics.pv.PvMetricsRecorder;
import com.ruoyi.system.service.pv.IPvMonitoringService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PvGatewayPollingTaskTest
{
    private static final String DASHBOARD_SUMMARY_CACHE_KEY = CacheConstants.PV_DASHBOARD_SUMMARY_KEY + "all";

    @Mock
    private PvAssetMapper assetMapper;

    @Mock
    private RedisCache redisCache;

    @Mock
    private IPvMonitoringService monitoringService;

    @InjectMocks
    private PvGatewayPollingTask task;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUpMetrics()
    {
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(task, "metricsRecorder", new PvMetricsRecorder(meterRegistry));
    }

    @Test
    void heartbeatCheckShouldSkipPollingWhenGatewayIntervalNotElapsed()
    {
        PvGateway gateway = pollingGateway(1L, 120, 30_000L);
        when(assetMapper.selectGatewayList(any(PvGateway.class))).thenReturn(List.of(gateway));

        task.heartbeatCheck(3);

        verify(monitoringService, never()).pollGatewayTelemetry(any(PvGateway.class));
        verify(assetMapper, never()).updateGatewayStatus(any(Long.class), any(String.class), any(Date.class));
        verify(assetMapper, never()).updateInvertersOfflineByGatewayId(any(Long.class));
    }

    @Test
    void heartbeatCheckShouldPollWhenGatewayIntervalElapsed()
    {
        PvGateway gateway = pollingGateway(2L, 60, 90_000L);
        when(assetMapper.selectGatewayList(any(PvGateway.class))).thenReturn(List.of(gateway));
        when(monitoringService.pollGatewayTelemetry(gateway)).thenReturn(2);

        task.heartbeatCheck(3);

        verify(monitoringService).pollGatewayTelemetry(gateway);
        verify(assetMapper, never()).updateGatewayStatus(any(Long.class), any(String.class), any(Date.class));
        verify(redisCache, never()).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
        assertEquals(1L, meterRegistry.get("pv.gateway.polling.duration").timer().count());
    }

    @Test
    void heartbeatCheckShouldMarkGatewayOfflineWhenTimedOutAndNoTelemetryPolled()
    {
        PvGateway gateway = pollingGateway(3L, 60, 240_000L);
        when(assetMapper.selectGatewayList(any(PvGateway.class))).thenReturn(List.of(gateway));
        when(monitoringService.pollGatewayTelemetry(gateway)).thenReturn(0);
        when(assetMapper.updateGatewayStatus(3L, "offline", gateway.getLastSeen())).thenReturn(1);
        when(assetMapper.updateInvertersOfflineByGatewayId(3L)).thenReturn(2);

        task.heartbeatCheck(3);

        verify(monitoringService).pollGatewayTelemetry(gateway);
        verify(assetMapper).updateGatewayStatus(3L, "offline", gateway.getLastSeen());
        verify(assetMapper).updateInvertersOfflineByGatewayId(3L);
        verify(redisCache).deleteObject(eq(DASHBOARD_SUMMARY_CACHE_KEY));
    }

    private PvGateway pollingGateway(Long gatewayId, int pollingIntervalSec, long elapsedMillis)
    {
        PvGateway gateway = new PvGateway();
        gateway.setGatewayId(gatewayId);
        gateway.setCommunicationType("Polling");
        gateway.setProtocol("ModbusTCP");
        gateway.setPollingIntervalSec(pollingIntervalSec);
        gateway.setLastSeen(new Date(System.currentTimeMillis() - elapsedMillis));
        return gateway;
    }
}
