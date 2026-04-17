package com.ruoyi.quartz.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.service.pv.IPvMonitoringService;

/**
 * 光伏遥测数据维护任务。
 */
@Component("pvTelemetryTask")
public class PvTelemetryTask
{
    private static final Logger log = LoggerFactory.getLogger(PvTelemetryTask.class);

    @Autowired
    private IPvMonitoringService monitoringService;

    public void cleanupTelemetry()
    {
        cleanupTelemetry(90);
    }

    public void cleanupTelemetry(Integer retentionDays)
    {
        int days = retentionDays == null || retentionDays < 1 ? 90 : retentionDays;
        int deletedRows = monitoringService.cleanupTelemetry(days);
        log.info("PV telemetry cleanup finished. retentionDays={}, deletedRows={}", days, deletedRows);
    }
}
