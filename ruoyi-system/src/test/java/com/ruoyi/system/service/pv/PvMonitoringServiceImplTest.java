package com.ruoyi.system.service.pv;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.pv.PvAlert;
import com.ruoyi.system.domain.pv.PvDashboardSummary;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.domain.pv.PvHourlyYieldBucket;
import com.ruoyi.system.domain.pv.PvHourlyYieldRow;
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.domain.pv.PvPowerSeriesPoint;
import com.ruoyi.system.domain.pv.PvTelemetry;
import com.ruoyi.system.mapper.pv.PvAssetMapper;
import com.ruoyi.system.mapper.pv.PvMonitoringMapper;
import com.ruoyi.system.service.pv.impl.PvMonitoringServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PvMonitoringServiceImplTest
{
    private static final String DASHBOARD_SUMMARY_CACHE_KEY = CacheConstants.PV_DASHBOARD_SUMMARY_KEY + "all";
    private static final DateTimeFormatter HOUR_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");

    @Mock
    private PvMonitoringMapper monitoringMapper;

    @Mock
    private PvAssetMapper assetMapper;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private PvMonitoringServiceImpl service;

    @Test
    void listHourlyYieldRowsShouldAggregateAcrossMidnightBuckets()
    {
        when(assetMapper.selectHourlyYieldBuckets(any(Date.class), any(Date.class), eq(2L))).thenReturn(List.of(
                bucket(1L, "苏州工业园区一号站", "工商业屋顶", 23, "1.25"),
                bucket(1L, "苏州工业园区一号站", "工商业屋顶", 0, "0.75"),
                bucket(1L, "苏州工业园区一号站", "工商业屋顶", 23, "0.50")));

        List<PvHourlyYieldRow> rows = service.listHourlyYieldRows("2026-04-12", "2026-04-13", 2L);

        assertEquals(1, rows.size());
        PvHourlyYieldRow row = rows.get(0);
        assertEquals("苏州工业园区一号站", row.getStationName());
        assertEquals("工商业屋顶", row.getTagName());
        assertBigDecimalEquals("0.75", row.getH0());
        assertBigDecimalEquals("1.75", row.getH23());
        assertBigDecimalEquals("2.50", row.getTotal());
    }

    @Test
    void listPowerSeriesShouldBackfillMissingHoursWithZero()
    {
        LocalDateTime startHour = LocalDateTime.now().minusHours(23).withMinute(0).withSecond(0).withNano(0);
        PvPowerSeriesPoint source = new PvPowerSeriesPoint();
        source.setHour(startHour.plusHours(5).format(HOUR_KEY_FORMATTER) + ":00:00");
        source.setAvgPowerKw(new BigDecimal("88.80"));
        when(monitoringMapper.selectPowerSeriesPoints(any(Date.class))).thenReturn(List.of(source));

        List<PvPowerSeriesPoint> points = service.listPowerSeries();

        assertEquals(24, points.size());
        assertEquals(1L, points.stream()
                .filter(point -> point.getAvgPowerKw().compareTo(new BigDecimal("88.80")) == 0)
                .count());
        assertEquals(23L, points.stream()
                .filter(point -> point.getAvgPowerKw().compareTo(BigDecimal.ZERO) == 0)
                .count());
    }

    @Test
    void getDashboardSummaryShouldReturnCachedValueWhenPresent()
    {
        PvDashboardSummary cached = new PvDashboardSummary();
        cached.setTotalInverters(3L);
        when(redisCache.getCacheObject(DASHBOARD_SUMMARY_CACHE_KEY)).thenReturn(cached);

        PvDashboardSummary summary = service.getDashboardSummary();

        assertSame(cached, summary);
        verify(monitoringMapper, never()).selectDashboardSummary();
        verify(redisCache, never()).setCacheObject(anyString(), any(), anyInt(), any(TimeUnit.class));
    }

    @Test
    void getDashboardSummaryShouldLoadNormalizeAndCacheOnMiss()
    {
        PvDashboardSummary dbSummary = new PvDashboardSummary();
        dbSummary.setTotalInverters(5L);

        when(redisCache.getCacheObject(DASHBOARD_SUMMARY_CACHE_KEY)).thenReturn(null);
        when(monitoringMapper.selectDashboardSummary()).thenReturn(dbSummary);

        PvDashboardSummary summary = service.getDashboardSummary();

        assertBigDecimalEquals("0.00", summary.getTotalCapacityMw());
        assertBigDecimalEquals("0.00", summary.getCurrentPowerKw());
        assertBigDecimalEquals("0.00", summary.getDailyYieldKwh());
        assertEquals(5L, summary.getTotalInverters());
        assertEquals(0L, summary.getOnlineInverters());
        assertEquals(0L, summary.getActiveAlerts());
        verify(redisCache).setCacheObject(eq(DASHBOARD_SUMMARY_CACHE_KEY), eq(summary), eq(30), eq(TimeUnit.SECONDS));
    }

    @Test
    void simulateTelemetryShouldProduceDeterministicStatusDistributionAndEvictCache()
    {
        List<PvInverter> inverters = new ArrayList<>();
        for (long index = 1; index <= 20; index++)
        {
            inverters.add(inverter(index, 1000L + index));
        }
        when(assetMapper.selectInverterList(any(PvInverter.class))).thenReturn(inverters);

        int simulated = service.simulateTelemetry("tester");

        assertEquals(20, simulated);

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BigDecimal> powerCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(assetMapper, times(20)).updateInverterStatus(any(Long.class), statusCaptor.capture(), any(Date.class),
                powerCaptor.capture(), any(BigDecimal.class));

        long online = statusCaptor.getAllValues().stream().filter("online"::equals).count();
        long offline = statusCaptor.getAllValues().stream().filter("offline"::equals).count();
        long fault = statusCaptor.getAllValues().stream().filter("fault"::equals).count();
        assertEquals(14L, online);
        assertEquals(3L, offline);
        assertEquals(3L, fault);

        long positivePower = powerCaptor.getAllValues().stream().filter(value -> value.compareTo(BigDecimal.ZERO) > 0).count();
        long zeroPower = powerCaptor.getAllValues().stream().filter(value -> value.compareTo(BigDecimal.ZERO) == 0).count();
        assertEquals(14L, positivePower);
        assertEquals(6L, zeroPower);

        ArgumentCaptor<List<PvTelemetry>> telemetryCaptor = ArgumentCaptor.forClass(List.class);
        verify(assetMapper).insertTelemetryBatch(telemetryCaptor.capture());
        assertEquals(20, telemetryCaptor.getValue().size());
        assertEquals(20L, telemetryCaptor.getValue().stream().filter(item -> item.getCollectTime() != null).count());
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    @Test
    void selectAlertListShouldDelegateToMapper()
    {
        PvAlert query = new PvAlert();
        query.setStatus("active");
        PvAlert alert = new PvAlert();
        alert.setAlertId(7L);
        when(monitoringMapper.selectAlertList(query)).thenReturn(List.of(alert));

        List<PvAlert> alerts = service.selectAlertList(query);

        assertEquals(1, alerts.size());
        assertSame(alert, alerts.get(0));
        verify(monitoringMapper).selectAlertList(query);
    }

    @Test
    void resolveAlertShouldEvictDashboardCacheWhenRowsUpdated()
    {
        when(monitoringMapper.resolveAlert(eq(99L), eq("tester"), any(Date.class))).thenReturn(1);

        int rows = service.resolveAlert(99L, "tester");

        assertEquals(1, rows);
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    @Test
    void resolveAllAlertsShouldNotEvictDashboardCacheWhenNothingChanges()
    {
        when(monitoringMapper.resolveAllActiveAlerts(eq("tester"), any(Date.class))).thenReturn(0);

        int rows = service.resolveAllAlerts("tester");

        assertEquals(0, rows);
        verify(redisCache, never()).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    @Test
    void cleanupTelemetryShouldDeleteInBatchesUntilExhausted()
    {
        when(assetMapper.deleteTelemetryBatchBefore(any(Date.class), eq(5000))).thenReturn(5000, 1200);

        int deleted = service.cleanupTelemetry(90);

        assertEquals(6200, deleted);
        verify(assetMapper, times(2)).deleteTelemetryBatchBefore(any(Date.class), eq(5000));
    }

    @Test
    void pollGatewayTelemetryShouldSkipUnsupportedGateway()
    {
        PvGateway gateway = gateway(1L, "Polling", "IEC104");

        int rows = service.pollGatewayTelemetry(gateway);

        assertEquals(0, rows);
        verify(assetMapper, never()).selectInverterList(any(PvInverter.class));
        verify(assetMapper, never()).insertTelemetryBatch(any());
        verify(redisCache, never()).deleteObject(anyString());
    }

    @Test
    void pollGatewayTelemetryShouldPersistTelemetryForSupportedModbusGateway()
    {
        PvGateway gateway = gateway(6L, "Polling", "ModbusTCP");
        when(assetMapper.selectInverterList(any(PvInverter.class))).thenReturn(List.of(inverter(11L, 6L), inverter(12L, 6L)));

        int rows = service.pollGatewayTelemetry(gateway);

        assertEquals(2, rows);

        ArgumentCaptor<List<PvTelemetry>> telemetryCaptor = ArgumentCaptor.forClass(List.class);
        verify(assetMapper).insertTelemetryBatch(telemetryCaptor.capture());
        assertEquals(2, telemetryCaptor.getValue().size());
        assertEquals(2L, telemetryCaptor.getValue().stream()
                .filter(item -> item.getActivePower() != null && item.getActivePower().compareTo(BigDecimal.ZERO) > 0)
                .count());
        assertEquals(2L, telemetryCaptor.getValue().stream().filter(item -> item.getCollectTime() != null).count());
        verify(assetMapper, times(2)).updateInverterStatus(any(Long.class), eq("online"), any(Date.class),
                any(BigDecimal.class), any(BigDecimal.class));
        verify(assetMapper).updateGatewayStatus(eq(6L), eq("online"), any(Date.class));
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    @Test
    void pollGatewayTelemetryShouldReadLiveModbusTcpRegistersWhenBrokerUrlConfigured() throws Exception
    {
        service.setModbusTcpRegisterReader((host, port, unitId, connectTimeoutMs, readTimeoutMs, startAddress, quantity) -> {
            assertEquals("127.0.0.1", host);
            assertEquals(1502, port);
            assertEquals(1500, connectTimeoutMs);
            assertEquals(1500, readTimeoutMs);
            assertEquals(0, startAddress);
            assertEquals(8, quantity);
            if (unitId == 11)
            {
                return new int[] { 0, 1234, 0, 456, 0, 7890, 2305, 987 };
            }
            if (unitId == 12)
            {
                return new int[] { 0, 1567, 0, 654, 0, 9123, 2310, 1111 };
            }
            throw new AssertionError("Unexpected Modbus unitId: " + unitId);
        });

        PvGateway gateway = gateway(8L, "Polling", "ModbusTCP");
        gateway.setBrokerUrl("tcp://127.0.0.1:1502?unitIds=11,12&connectTimeoutMs=1500&readTimeoutMs=1500");
        gateway.setTopic("power=0:2:0.1;dailyYield=2:2:0.1;totalYield=4:2:0.1;voltage=6:1:0.1;current=7:1:0.01");
        when(assetMapper.selectInverterList(any(PvInverter.class)))
                .thenReturn(List.of(inverter(21L, 8L), inverter(22L, 8L)));

        int rows = service.pollGatewayTelemetry(gateway);

        assertEquals(2, rows);

        ArgumentCaptor<List<PvTelemetry>> telemetryCaptor = ArgumentCaptor.forClass(List.class);
        verify(assetMapper).insertTelemetryBatch(telemetryCaptor.capture());
        List<PvTelemetry> telemetryList = telemetryCaptor.getValue();
        assertEquals(2, telemetryList.size());
        assertBigDecimalEquals("123.40", telemetryList.get(0).getActivePower());
        assertBigDecimalEquals("45.60", telemetryList.get(0).getDailyYield());
        assertBigDecimalEquals("789.00", telemetryList.get(0).getTotalYield());
        assertBigDecimalEquals("230.50", telemetryList.get(0).getVoltage());
        assertBigDecimalEquals("9.87", telemetryList.get(0).getCurrent());
        assertBigDecimalEquals("156.70", telemetryList.get(1).getActivePower());
        assertBigDecimalEquals("65.40", telemetryList.get(1).getDailyYield());
        assertBigDecimalEquals("912.30", telemetryList.get(1).getTotalYield());
        assertBigDecimalEquals("231.00", telemetryList.get(1).getVoltage());
        assertBigDecimalEquals("11.11", telemetryList.get(1).getCurrent());
    }

    private PvHourlyYieldBucket bucket(Long stationId, String stationName, String tagName, int hour, String yield)
    {
        PvHourlyYieldBucket bucket = new PvHourlyYieldBucket();
        bucket.setStationId(stationId);
        bucket.setStationName(stationName);
        bucket.setTagName(tagName);
        bucket.setHourOfDay(hour);
        bucket.setYieldKwh(new BigDecimal(yield));
        return bucket;
    }

    private PvInverter inverter(Long inverterId, Long gatewayId)
    {
        PvInverter inverter = new PvInverter();
        inverter.setInverterId(inverterId);
        inverter.setGatewayId(gatewayId);
        inverter.setInverterNumber("INV-" + inverterId);
        inverter.setSerialNumber("SN-" + inverterId);
        inverter.setGatewayName("GW-" + gatewayId);
        inverter.setDailyYield(new BigDecimal("10.00"));
        inverter.setLastSeen(new Date());
        return inverter;
    }

    private PvGateway gateway(Long gatewayId, String communicationType, String protocol)
    {
        PvGateway gateway = new PvGateway();
        gateway.setGatewayId(gatewayId);
        gateway.setCommunicationType(communicationType);
        gateway.setProtocol(protocol);
        gateway.setPollingIntervalSec(60);
        return gateway;
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual)
    {
        assertNotNull(actual);
        assertTrue(actual.compareTo(new BigDecimal(expected)) == 0,
                () -> "Expected " + expected + " but was " + actual);
    }
}
