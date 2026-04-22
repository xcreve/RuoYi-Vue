package com.ruoyi.system.metrics.pv;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvMetricsRecorderTest
{
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final PvMetricsRecorder recorder = new PvMetricsRecorder(meterRegistry);

    @Test
    void recordTelemetryIngestShouldCountRowsBySource()
    {
        recorder.recordTelemetryIngest("mqtt", 3);

        assertEquals(3.0, meterRegistry.get("pv.telemetry.ingest").tag("source", "mqtt").counter().count());
    }

    @Test
    void recordMqttMessageShouldCountByTopic()
    {
        recorder.recordMqttMessage("ems/gateway/GW-1/telemetry");

        assertEquals(1.0, meterRegistry.get("pv.mqtt.message")
                .tag("topic", "ems/gateway/GW-1/telemetry").counter().count());
    }

    @Test
    void recordGatewayPollingDurationShouldUseTimer()
    {
        recorder.recordGatewayPollingDuration(25, TimeUnit.MILLISECONDS);

        assertEquals(1L, meterRegistry.get("pv.gateway.polling.duration").timer().count());
        assertTrue(meterRegistry.get("pv.gateway.polling.duration").timer().totalTime(TimeUnit.MILLISECONDS) >= 25.0);
    }

    @Test
    void partitionGaugesShouldExposeCountAndLastSuccessTimestamp()
    {
        recorder.updateTelemetryPartitionCount(13);
        recorder.recordPartitionMaintainSuccess(Instant.parse("2026-04-22T12:00:00Z"));

        assertEquals(13.0, meterRegistry.get("pv.telemetry.partition.count").gauge().value());
        assertEquals(1776859200.0, meterRegistry.get("pv.partition.maintain.last.success.timestamp").gauge().value());
    }
}
