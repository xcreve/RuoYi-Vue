package com.ruoyi.quartz.task;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.mapper.pv.PvAssetMapper;
import com.ruoyi.system.metrics.pv.PvMetricsRecorder;
import com.ruoyi.system.service.pv.IPvMonitoringService;

/**
 * 光伏网关轮询与心跳检查任务。
 */
@Component("pvGatewayPollingTask")
public class PvGatewayPollingTask
{
    private static final Logger log = LoggerFactory.getLogger(PvGatewayPollingTask.class);
    private static final String DASHBOARD_SUMMARY_CACHE_KEY = CacheConstants.PV_DASHBOARD_SUMMARY_KEY + "all";

    @Autowired
    private PvAssetMapper assetMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private IPvMonitoringService monitoringService;

    @Autowired(required = false)
    private PvMetricsRecorder metricsRecorder;

    public void heartbeatCheck()
    {
        heartbeatCheck(3);
    }

    public void heartbeatCheck(Integer timeoutMultiplier)
    {
        long startedNanos = System.nanoTime();
        try
        {
            int multiplier = timeoutMultiplier == null || timeoutMultiplier < 1 ? 3 : timeoutMultiplier;
            List<PvGateway> gateways = assetMapper.selectGatewayList(new PvGateway());
            int gatewayUpdates = 0;
            int inverterUpdates = 0;
            int polledTelemetry = 0;

            for (PvGateway gateway : gateways)
            {
                if (isActivePollingGateway(gateway) && shouldPollNow(gateway))
                {
                    try
                    {
                        int polledRows = monitoringService.pollGatewayTelemetry(gateway);
                        polledTelemetry += polledRows;
                        if (polledRows > 0)
                        {
                            continue;
                        }
                    }
                    catch (Exception ex)
                    {
                        log.warn("PV gateway active polling failed. gatewayId={}, protocol={}, message={}",
                                gateway.getGatewayId(), gateway.getProtocol(), ex.getMessage(), ex);
                    }
                }
                if (!isTimeout(gateway, multiplier))
                {
                    continue;
                }
                Date lastSeen = gateway.getLastSeen();
                gatewayUpdates += assetMapper.updateGatewayStatus(gateway.getGatewayId(), "offline", lastSeen);
                inverterUpdates += assetMapper.updateInvertersOfflineByGatewayId(gateway.getGatewayId());
            }

            if (gatewayUpdates > 0 || inverterUpdates > 0)
            {
                redisCache.deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
            }
            log.info(
                    "PV gateway polling task finished. timeoutMultiplier={}, polledTelemetry={}, gatewayUpdates={}, inverterUpdates={}",
                    multiplier, polledTelemetry, gatewayUpdates, inverterUpdates);
        }
        finally
        {
            recordGatewayPollingDuration(System.nanoTime() - startedNanos);
        }
    }

    private void recordGatewayPollingDuration(long elapsedNanos)
    {
        if (metricsRecorder != null)
        {
            metricsRecorder.recordGatewayPollingDuration(elapsedNanos, TimeUnit.NANOSECONDS);
        }
    }

    private boolean isActivePollingGateway(PvGateway gateway)
    {
        return StringUtils.equalsIgnoreCase("Polling", gateway.getCommunicationType())
                && StringUtils.equalsAnyIgnoreCase(gateway.getProtocol(), "ModbusTCP", "ModbusRTU");
    }

    private boolean isTimeout(PvGateway gateway, int timeoutMultiplier)
    {
        if (gateway.getLastSeen() == null)
        {
            return true;
        }
        long timeoutMillis = resolvePollingIntervalSeconds(gateway) * timeoutMultiplier * 1000L;
        return DateUtils.getNowDate().getTime() - gateway.getLastSeen().getTime() > timeoutMillis;
    }

    private boolean shouldPollNow(PvGateway gateway)
    {
        if (gateway.getLastSeen() == null)
        {
            return true;
        }
        long elapsedMillis = DateUtils.getNowDate().getTime() - gateway.getLastSeen().getTime();
        return elapsedMillis >= resolvePollingIntervalSeconds(gateway) * 1000L;
    }

    private long resolvePollingIntervalSeconds(PvGateway gateway)
    {
        return gateway.getPollingIntervalSec() == null || gateway.getPollingIntervalSec() < 10
                ? 60L : gateway.getPollingIntervalSec().longValue();
    }
}
