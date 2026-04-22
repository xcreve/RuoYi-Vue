package com.ruoyi.system.service.pv.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.metrics.pv.PvMetricsRecorder;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.domain.pv.PvTelemetry;
import com.ruoyi.system.mapper.pv.PvAssetMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PvMqttIngestServiceTest
{
    private static final String DASHBOARD_SUMMARY_CACHE_KEY = CacheConstants.PV_DASHBOARD_SUMMARY_KEY + "all";

    @Mock
    private PvAssetMapper assetMapper;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private PvMqttIngestService service;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUpMetrics()
    {
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(service, "metricsRecorder", new PvMetricsRecorder(meterRegistry));
    }

    @Test
    void refreshSubscriptionsShouldGroupBrokerTopicsAndRemoveStaleSubscription()
    {
        List<PvMqttIngestService.BrokerSubscription> registrations = new ArrayList<>();
        List<String> unregistrations = new ArrayList<>();
        service.setMqttSubscriptionRegistrar(registrations::add);
        service.setMqttSubscriptionUnregistrar(unregistrations::add);

        when(assetMapper.selectGatewayList(any(PvGateway.class))).thenReturn(
                List.of(
                        gateway(1L, "mqtt://broker-a:1883?clientId=gw-a", "ems/gateway/GW-1/telemetry"),
                        gateway(2L, "mqtt://broker-a:1883?clientId=gw-a", "ems/gateway/GW-2/telemetry"),
                        gateway(3L, "mqtt://broker-b:1883?clientId=gw-b", "ems/gateway/GW-3/telemetry"),
                        gateway(4L, "", "ems/gateway/GW-4/telemetry")),
                List.of(
                        gateway(1L, "mqtt://broker-a:1883?clientId=gw-a", "ems/gateway/GW-1/telemetry"),
                        gateway(2L, "mqtt://broker-a:1883?clientId=gw-a", "ems/gateway/GW-2/telemetry")));

        service.refreshSubscriptions();
        service.refreshSubscriptions();

        assertEquals(2, registrations.size());
        assertEquals(2, registrations.get(0).getTopics().size());
        assertEquals(1, registrations.get(1).getTopics().size());
        assertEquals(1, unregistrations.size());
        assertEquals(registrations.get(1).getBrokerKey(), unregistrations.get(0));

        ArgumentCaptor<PvGateway> queryCaptor = ArgumentCaptor.forClass(PvGateway.class);
        verify(assetMapper, times(2)).selectGatewayList(queryCaptor.capture());
        assertEquals("MQTT", queryCaptor.getAllValues().get(0).getCommunicationType());
    }

    @Test
    void ingestMessageShouldPersistTelemetryAndUpdateMatchedInverter()
    {
        String brokerUrl = "mqtt://broker-a:1883?clientId=gw-a";
        PvGateway gateway = gateway(10L, brokerUrl, "ems/gateway/+/telemetry");
        PvInverter primary = inverter(101L, 10L, "SN-101", "INV-101");
        PvInverter secondary = inverter(102L, 10L, "SN-102", "INV-102");

        when(assetMapper.selectGatewayList(any(PvGateway.class))).thenReturn(List.of(gateway));
        when(assetMapper.selectInverterList(any(PvInverter.class))).thenReturn(List.of(primary, secondary));

        int rows = service.ingestMessage(brokerUrl, "ems/gateway/GW-10/telemetry",
                "{\"samples\":[{\"serialNumber\":\"SN-101\",\"activePower\":\"123.40\",\"dailyYield\":\"45.60\","
                        + "\"totalYield\":\"789.00\",\"voltage\":\"230.50\",\"current\":\"9.87\","
                        + "\"timestamp\":1713072000000,\"legacyFirebaseId\":\"mqtt-1\"}]}");

        assertEquals(1, rows);

        ArgumentCaptor<PvInverter> inverterQueryCaptor = ArgumentCaptor.forClass(PvInverter.class);
        verify(assetMapper).selectInverterList(inverterQueryCaptor.capture());
        assertEquals(10L, inverterQueryCaptor.getValue().getGatewayId());

        ArgumentCaptor<List<PvTelemetry>> telemetryCaptor = ArgumentCaptor.forClass(List.class);
        verify(assetMapper).insertTelemetryBatch(telemetryCaptor.capture());
        List<PvTelemetry> telemetryList = telemetryCaptor.getValue();
        assertEquals(1, telemetryList.size());
        PvTelemetry telemetry = telemetryList.get(0);
        assertEquals(101L, telemetry.getInverterId());
        assertBigDecimalEquals("123.40", telemetry.getActivePower());
        assertBigDecimalEquals("45.60", telemetry.getDailyYield());
        assertBigDecimalEquals("789.00", telemetry.getTotalYield());
        assertBigDecimalEquals("230.50", telemetry.getVoltage());
        assertBigDecimalEquals("9.87", telemetry.getCurrent());
        assertEquals("mqtt-1", telemetry.getLegacyFirebaseId());
        assertNotNull(telemetry.getCollectTime());

        verify(assetMapper).updateInverterStatus(eq(101L), eq("online"), any(Date.class), any(BigDecimal.class),
                any(BigDecimal.class));
        verify(assetMapper, never()).updateInverterStatus(eq(102L), eq("online"), any(Date.class), any(BigDecimal.class),
                any(BigDecimal.class));
        verify(assetMapper).updateGatewayStatus(eq(10L), eq("online"), any(Date.class));
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
        assertEquals(1.0, meterRegistry.get("pv.mqtt.message")
                .tag("topic", "ems/gateway/GW-10/telemetry").counter().count());
        assertEquals(1.0, meterRegistry.get("pv.telemetry.ingest").tag("source", "mqtt").counter().count());
    }

    @Test
    void ingestMessageShouldFallbackToSingleGatewayInverterWhenIdentityMissing()
    {
        String brokerUrl = "mqtt://broker-c:1883?clientId=gw-c";
        PvGateway gateway = gateway(20L, brokerUrl, "ems/gateway/GW-20/telemetry");
        PvInverter inverter = inverter(201L, 20L, "SN-201", "INV-201");

        when(assetMapper.selectGatewayList(any(PvGateway.class))).thenReturn(List.of(gateway));
        when(assetMapper.selectInverterList(any(PvInverter.class))).thenReturn(List.of(inverter));

        int rows = service.ingestMessage(brokerUrl, "ems/gateway/GW-20/telemetry",
                "{\"activePower\":\"10.00\",\"dailyYield\":\"2.50\",\"totalYield\":\"50.00\","
                        + "\"voltage\":\"380.00\",\"current\":\"5.10\"}");

        assertEquals(1, rows);

        ArgumentCaptor<List<PvTelemetry>> telemetryCaptor = ArgumentCaptor.forClass(List.class);
        verify(assetMapper).insertTelemetryBatch(telemetryCaptor.capture());
        assertEquals(201L, telemetryCaptor.getValue().get(0).getInverterId());
        verify(assetMapper).updateInverterStatus(eq(201L), eq("online"), any(Date.class), any(BigDecimal.class),
                any(BigDecimal.class));
    }

    private PvGateway gateway(Long gatewayId, String brokerUrl, String topic)
    {
        PvGateway gateway = new PvGateway();
        gateway.setGatewayId(gatewayId);
        gateway.setCommunicationType("MQTT");
        gateway.setBrokerUrl(brokerUrl);
        gateway.setTopic(topic);
        gateway.setSerialNumber("GW-SN-" + gatewayId);
        return gateway;
    }

    private PvInverter inverter(Long inverterId, Long gatewayId, String serialNumber, String inverterNumber)
    {
        PvInverter inverter = new PvInverter();
        inverter.setInverterId(inverterId);
        inverter.setGatewayId(gatewayId);
        inverter.setSerialNumber(serialNumber);
        inverter.setInverterNumber(inverterNumber);
        inverter.setDailyYield(new BigDecimal("1.20"));
        inverter.setLastSeen(new Date());
        return inverter;
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual)
    {
        assertNotNull(actual);
        assertTrue(actual.compareTo(new BigDecimal(expected)) == 0,
                () -> "Expected " + expected + " but was " + actual);
    }
}
