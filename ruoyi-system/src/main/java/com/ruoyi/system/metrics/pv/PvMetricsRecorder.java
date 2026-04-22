package com.ruoyi.system.metrics.pv;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Centralizes PV business metrics so call sites stay focused on domain logic.
 */
@Component
public class PvMetricsRecorder
{
    private final MeterRegistry meterRegistry;
    private final AtomicInteger telemetryPartitionCount = new AtomicInteger();
    private final AtomicLong partitionMaintainLastSuccessTimestamp = new AtomicLong();

    public PvMetricsRecorder(MeterRegistry meterRegistry)
    {
        this.meterRegistry = meterRegistry;
        Gauge.builder("pv.telemetry.partition.count", telemetryPartitionCount, AtomicInteger::get)
                .description("Current pv_telemetry partition count, including pmax")
                .register(meterRegistry);
        Gauge.builder("pv.partition.maintain.last.success.timestamp", partitionMaintainLastSuccessTimestamp,
                AtomicLong::get)
                .description("Unix timestamp of the last successful pv_telemetry partition maintenance")
                .baseUnit("seconds")
                .register(meterRegistry);
    }

    public void recordTelemetryIngest(String source, int rows)
    {
        if (rows <= 0)
        {
            return;
        }
        Counter.builder("pv.telemetry.ingest")
                .description("PV telemetry rows ingested")
                .tag("source", normalizeTagValue(source))
                .register(meterRegistry)
                .increment(rows);
    }

    public void recordMqttMessage(String topic)
    {
        if (topic == null || topic.isBlank())
        {
            return;
        }
        Counter.builder("pv.mqtt.message")
                .description("PV MQTT messages received")
                .tag("topic", topic)
                .register(meterRegistry)
                .increment();
    }

    public void recordGatewayPollingDuration(long amount, TimeUnit unit)
    {
        if (amount < 0 || unit == null)
        {
            return;
        }
        Timer.builder("pv.gateway.polling.duration")
                .description("PV gateway polling task duration")
                .register(meterRegistry)
                .record(amount, unit);
    }

    public void updateTelemetryPartitionCount(int partitionCount)
    {
        telemetryPartitionCount.set(Math.max(partitionCount, 0));
    }

    public void recordPartitionMaintainSuccess(Instant instant)
    {
        if (instant != null)
        {
            partitionMaintainLastSuccessTimestamp.set(instant.getEpochSecond());
        }
    }

    private String normalizeTagValue(String value)
    {
        if (value == null || value.isBlank())
        {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
