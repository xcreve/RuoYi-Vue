package com.ruoyi.system.service.pv.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.pv.PvAlert;
import com.ruoyi.system.domain.pv.PvDashboardSummary;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.domain.pv.PvHourlyYieldBucket;
import com.ruoyi.system.domain.pv.PvHourlyYieldRow;
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.domain.pv.PvPowerSeriesPoint;
import com.ruoyi.system.domain.pv.PvTelemetry;
import com.ruoyi.system.event.pv.PvAlertCreatedPublisher;
import com.ruoyi.system.event.pv.PvDashboardRefreshPublisher;
import com.ruoyi.system.mapper.pv.PvAssetMapper;
import com.ruoyi.system.mapper.pv.PvMonitoringMapper;
import com.ruoyi.system.service.pv.IPvMonitoringService;

@Service
public class PvMonitoringServiceImpl implements IPvMonitoringService
{
    private static final DateTimeFormatter HOUR_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
    private static final String DASHBOARD_SUMMARY_CACHE_KEY = CacheConstants.PV_DASHBOARD_SUMMARY_KEY + "all";
    private static final int DASHBOARD_SUMMARY_CACHE_SECONDS = 30;
    private static final int TELEMETRY_CLEANUP_BATCH_SIZE = 5000;
    private static final int MODBUS_TCP_DEFAULT_PORT = 502;
    private static final int MODBUS_TCP_DEFAULT_TIMEOUT_MS = 2000;
    private static final int MODBUS_READ_HOLDING_REGISTERS = 3;
    private static final int MODBUS_MAX_REGISTER_COUNT = 125;

    @Autowired
    private PvMonitoringMapper monitoringMapper;

    @Autowired
    private PvAssetMapper assetMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired(required = false)
    private PvDashboardRefreshPublisher dashboardRefreshPublisher;

    @Autowired(required = false)
    private PvAlertCreatedPublisher alertCreatedPublisher;

    private ModbusTcpRegisterReader modbusTcpRegisterReader = this::executeModbusTcpRead;

    @Override
    public PvDashboardSummary getDashboardSummary()
    {
        PvDashboardSummary summary = redisCache.getCacheObject(DASHBOARD_SUMMARY_CACHE_KEY);
        if (summary != null)
        {
            return summary;
        }
        summary = normalizeDashboardSummary(monitoringMapper.selectDashboardSummary());
        redisCache.setCacheObject(DASHBOARD_SUMMARY_CACHE_KEY, summary, DASHBOARD_SUMMARY_CACHE_SECONDS, TimeUnit.SECONDS);
        return summary;
    }

    @Override
    public List<PvPowerSeriesPoint> listPowerSeries()
    {
        LocalDateTime startHour = LocalDateTime.now().minusHours(23).withMinute(0).withSecond(0).withNano(0);
        List<PvPowerSeriesPoint> source = monitoringMapper.selectPowerSeriesPoints(DateUtils.toDate(startHour));
        Map<String, BigDecimal> pointMap = new HashMap<>();
        for (PvPowerSeriesPoint point : source)
        {
            pointMap.put(point.getHour(), scale(point.getAvgPowerKw()));
        }

        List<PvPowerSeriesPoint> result = new ArrayList<>();
        for (int i = 0; i < 24; i++)
        {
            LocalDateTime cursor = startHour.plusHours(i);
            String hourKey = cursor.format(HOUR_KEY_FORMATTER) + ":00:00";
            PvPowerSeriesPoint point = new PvPowerSeriesPoint();
            point.setHour(hourKey);
            point.setAvgPowerKw(pointMap.getOrDefault(hourKey, zero()));
            result.add(point);
        }
        return result;
    }

    @Override
    @Transactional
    public int simulateTelemetry(String operator)
    {
        List<PvInverter> inverters = assetMapper.selectInverterList(new PvInverter());
        if (inverters.isEmpty())
        {
            return 0;
        }

        Collections.shuffle(inverters);
        Date now = DateUtils.getNowDate();
        List<PvTelemetry> telemetryList = new ArrayList<>();
        Map<Long, GatewaySnapshot> gatewaySnapshots = new HashMap<>();

        for (int index = 0; index < inverters.size(); index++)
        {
            PvInverter inverter = inverters.get(index);
            String status = randomStatus(index, inverters.size());
            BigDecimal currentPower = zero();
            BigDecimal dailyYield = baseDailyYield(inverter, now);

            if ("online".equals(status))
            {
                currentPower = randomDecimal(12.0, 120.0);
                dailyYield = dailyYield.add(randomDecimal(0.3, 5.5));
            }

            PvTelemetry telemetry = new PvTelemetry();
            telemetry.setInverterId(inverter.getInverterId());
            telemetry.setActivePower(currentPower);
            telemetry.setDailyYield(scale(dailyYield));
            telemetry.setTotalYield(scale(dailyYield.multiply(BigDecimal.valueOf(31)).add(randomDecimal(100.0, 500.0))));
            telemetry.setVoltage(randomDecimal(360.0, 620.0));
            telemetry.setCurrent(currentPower.compareTo(BigDecimal.ZERO) > 0 ? randomDecimal(12.0, 98.0) : zero());
            telemetry.setCollectTime(now);
            telemetryList.add(telemetry);

            assetMapper.updateInverterStatus(inverter.getInverterId(), status, now, currentPower, scale(dailyYield));
            mergeGatewayStatus(gatewaySnapshots, inverter.getGatewayId(), status, now);

            if ("fault".equals(status) || "offline".equals(status) && ThreadLocalRandom.current().nextDouble() > 0.6)
            {
                PvAlert alert = buildAlert(inverter, status, operator, now);
                monitoringMapper.insertAlert(alert);
                publishAlertCreated(alert);
            }
        }

        assetMapper.insertTelemetryBatch(telemetryList);
        for (Map.Entry<Long, GatewaySnapshot> entry : gatewaySnapshots.entrySet())
        {
            assetMapper.updateGatewayStatus(entry.getKey(), entry.getValue().getStatus(), entry.getValue().getLastSeen());
        }
        evictDashboardSummaryCache();
        publishDashboardRefresh("monitoring.simulate");
        return telemetryList.size();
    }

    @Override
    public List<PvAlert> selectAlertList(PvAlert query)
    {
        return monitoringMapper.selectAlertList(query);
    }

    @Override
    @Transactional
    public int resolveAlert(Long alertId, String resolvedBy)
    {
        int rows = monitoringMapper.resolveAlert(alertId, resolvedBy, DateUtils.getNowDate());
        if (rows > 0)
        {
            evictDashboardSummaryCache();
            publishDashboardRefresh("alert.resolve");
        }
        return rows;
    }

    @Override
    @Transactional
    public int resolveAllAlerts(String resolvedBy)
    {
        int rows = monitoringMapper.resolveAllActiveAlerts(resolvedBy, DateUtils.getNowDate());
        if (rows > 0)
        {
            evictDashboardSummaryCache();
            publishDashboardRefresh("alert.resolveAll");
        }
        return rows;
    }

    @Override
    public List<PvHourlyYieldRow> listHourlyYieldRows(String startDate, String endDate, Long tagId)
    {
        LocalDate start = StringUtils.isBlank(startDate) ? LocalDate.now() : LocalDate.parse(startDate);
        LocalDate end = StringUtils.isBlank(endDate) ? start : LocalDate.parse(endDate);
        Date startTime = DateUtils.toDate(LocalDateTime.of(start, LocalTime.MIN));
        Date endTime = DateUtils.toDate(LocalDateTime.of(end, LocalTime.of(23, 59, 59)));

        List<PvHourlyYieldBucket> buckets = assetMapper.selectHourlyYieldBuckets(startTime, endTime, tagId);
        Map<Long, PvHourlyYieldRow> rowMap = new LinkedHashMap<>();

        for (PvHourlyYieldBucket bucket : buckets)
        {
            PvHourlyYieldRow row = rowMap.computeIfAbsent(bucket.getStationId(), key -> {
                PvHourlyYieldRow next = new PvHourlyYieldRow();
                next.setStationId(bucket.getStationId());
                next.setStationName(bucket.getStationName());
                next.setTagName(StringUtils.isBlank(bucket.getTagName()) ? "未分组" : bucket.getTagName());
                return next;
            });
            if (bucket.getHourOfDay() != null)
            {
                row.addHourValue(bucket.getHourOfDay(), bucket.getYieldKwh());
            }
        }

        return new ArrayList<>(rowMap.values());
    }

    @Override
    @Transactional
    public int cleanupTelemetry(Integer retentionDays)
    {
        int days = retentionDays == null || retentionDays < 1 ? 90 : retentionDays;
        Date cutoffTime = DateUtils.toDate(LocalDateTime.now().minusDays(days));
        int total = 0;
        int rows;
        do
        {
            rows = assetMapper.deleteTelemetryBatchBefore(cutoffTime, TELEMETRY_CLEANUP_BATCH_SIZE);
            total += rows;
        }
        while (rows == TELEMETRY_CLEANUP_BATCH_SIZE);
        return total;
    }

    @Override
    @Transactional
    public int pollGatewayTelemetry(PvGateway gateway)
    {
        if (gateway == null || gateway.getGatewayId() == null || !supportsModbusPolling(gateway))
        {
            return 0;
        }

        PvInverter query = new PvInverter();
        query.setGatewayId(gateway.getGatewayId());
        List<PvInverter> inverters = assetMapper.selectInverterList(query);
        if (inverters.isEmpty())
        {
            return 0;
        }

        LocalDateTime nowTime = LocalDateTime.now().withSecond(0).withNano(0);
        Date now = DateUtils.toDate(nowTime);
        List<PvTelemetry> telemetryList = new ArrayList<>();
        ModbusTcpProfile modbusTcpProfile = resolveModbusTcpProfile(gateway);

        for (int index = 0; index < inverters.size(); index++)
        {
            PvInverter inverter = inverters.get(index);
            PolledTelemetrySnapshot snapshot = modbusTcpProfile == null
                    ? buildSyntheticPolledSnapshot(gateway, inverter, index, nowTime, now)
                    : readModbusTcpSnapshot(modbusTcpProfile, inverter, index);
            PvTelemetry telemetry = new PvTelemetry();
            telemetry.setInverterId(inverter.getInverterId());
            telemetry.setActivePower(snapshot.getCurrentPower());
            telemetry.setDailyYield(snapshot.getDailyYield());
            telemetry.setTotalYield(snapshot.getTotalYield());
            telemetry.setVoltage(snapshot.getVoltage());
            telemetry.setCurrent(snapshot.getCurrent());
            telemetry.setCollectTime(now);
            telemetryList.add(telemetry);

            assetMapper.updateInverterStatus(inverter.getInverterId(), "online", now, snapshot.getCurrentPower(),
                    snapshot.getDailyYield());
        }

        assetMapper.insertTelemetryBatch(telemetryList);
        assetMapper.updateGatewayStatus(gateway.getGatewayId(), "online", now);
        evictDashboardSummaryCache();
        publishDashboardRefresh("gateway.poll");
        return telemetryList.size();
    }

    private PvDashboardSummary normalizeDashboardSummary(PvDashboardSummary summary)
    {
        PvDashboardSummary result = summary == null ? new PvDashboardSummary() : summary;
        if (result.getTotalStations() == null)
        {
            result.setTotalStations(0L);
        }
        if (result.getTotalCapacityMw() == null)
        {
            result.setTotalCapacityMw(zero());
        }
        if (result.getCurrentPowerKw() == null)
        {
            result.setCurrentPowerKw(zero());
        }
        if (result.getDailyYieldKwh() == null)
        {
            result.setDailyYieldKwh(zero());
        }
        if (result.getTotalInverters() == null)
        {
            result.setTotalInverters(0L);
        }
        if (result.getOnlineInverters() == null)
        {
            result.setOnlineInverters(0L);
        }
        if (result.getActiveAlerts() == null)
        {
            result.setActiveAlerts(0L);
        }
        return result;
    }

    private String randomStatus(int index, int total)
    {
        if (total <= 1)
        {
            return "online";
        }
        if (total == 2)
        {
            return index == 0 ? "online" : "offline";
        }

        int offlineCount = Math.max(1, (int) Math.round(total * 0.15D));
        int faultCount = Math.max(1, (int) Math.round(total * 0.15D));
        int onlineCount = total - offlineCount - faultCount;

        if (onlineCount < 1)
        {
            onlineCount = 1;
            if (offlineCount >= faultCount && offlineCount > 1)
            {
                offlineCount--;
            }
            else if (faultCount > 1)
            {
                faultCount--;
            }
        }

        if (index < onlineCount)
        {
            return "online";
        }
        if (index < onlineCount + offlineCount)
        {
            return "offline";
        }
        return "fault";
    }

    private BigDecimal baseDailyYield(PvInverter inverter, Date now)
    {
        if (inverter.getLastSeen() == null)
        {
            return zero();
        }
        String today = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, now);
        String lastSeenDay = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, inverter.getLastSeen());
        return today.equals(lastSeenDay) ? scale(inverter.getDailyYield()) : zero();
    }

    private boolean supportsModbusPolling(PvGateway gateway)
    {
        return "Polling".equalsIgnoreCase(gateway.getCommunicationType())
                && ("ModbusTCP".equalsIgnoreCase(gateway.getProtocol()) || "ModbusRTU".equalsIgnoreCase(gateway.getProtocol()));
    }

    private PolledTelemetrySnapshot buildSyntheticPolledSnapshot(PvGateway gateway, PvInverter inverter, int index,
            LocalDateTime nowTime, Date now)
    {
        // 未配置真实采集地址时继续保留演示样本，避免现有大屏/告警演示链路回退。
        BigDecimal currentPower = buildPolledPower(gateway, inverter, index, nowTime);
        BigDecimal dailyYield = scale(baseDailyYield(inverter, now).add(buildPolledYieldIncrement(currentPower)));
        return new PolledTelemetrySnapshot(currentPower, dailyYield,
                buildPolledTotalYield(gateway, inverter, index, dailyYield), buildPolledVoltage(gateway, index),
                buildPolledCurrent(currentPower));
    }

    private PolledTelemetrySnapshot readModbusTcpSnapshot(ModbusTcpProfile profile, PvInverter inverter, int index)
    {
        int unitId = resolveUnitId(profile, index);
        RegisterWindow window = profile.getWindow();
        int[] registers = modbusTcpRegisterReader.read(profile.getHost(), profile.getPort(), unitId,
                profile.getConnectTimeoutMs(), profile.getReadTimeoutMs(), window.getStartAddress(),
                window.getQuantity());
        BigDecimal currentPower = normalizeTelemetryValue(
                decodeRegisterValue(registers, window, profile.getRegisterSpecs().get("power")));
        BigDecimal dailyYield = normalizeTelemetryValue(
                decodeRegisterValue(registers, window, profile.getRegisterSpecs().get("dailyYield")));
        BigDecimal totalYield = normalizeTelemetryValue(
                decodeRegisterValue(registers, window, profile.getRegisterSpecs().get("totalYield")));
        BigDecimal voltage = normalizeTelemetryValue(
                decodeRegisterValue(registers, window, profile.getRegisterSpecs().get("voltage")));
        BigDecimal current = normalizeTelemetryValue(
                decodeRegisterValue(registers, window, profile.getRegisterSpecs().get("current")));
        return new PolledTelemetrySnapshot(currentPower, dailyYield, totalYield, voltage, current);
    }

    public void setModbusTcpRegisterReader(ModbusTcpRegisterReader modbusTcpRegisterReader)
    {
        this.modbusTcpRegisterReader = modbusTcpRegisterReader == null ? this::executeModbusTcpRead : modbusTcpRegisterReader;
    }

    private ModbusTcpProfile resolveModbusTcpProfile(PvGateway gateway)
    {
        if (!"ModbusTCP".equalsIgnoreCase(gateway.getProtocol()) || StringUtils.isBlank(gateway.getBrokerUrl()))
        {
            return null;
        }

        String rawEndpoint = gateway.getBrokerUrl().trim();
        if (startsWithAnyIgnoreCase(rawEndpoint, "mqtt://", "ws://", "wss://", "http://", "https://"))
        {
            return null;
        }

        String normalizedEndpoint = rawEndpoint.contains("://") ? rawEndpoint : "tcp://" + rawEndpoint;
        try
        {
            URI uri = new URI(normalizedEndpoint);
            if (!StringUtils.equalsAnyIgnoreCase(uri.getScheme(), "tcp", "modbus", "modbus-tcp"))
            {
                return null;
            }
            if (StringUtils.isBlank(uri.getHost()))
            {
                throw new IllegalArgumentException("ModbusTCP 采集地址缺少主机名");
            }

            Map<String, String> params = parseQueryParams(uri.getRawQuery());
            int unitId = parsePositiveInteger(params.get("unitId"), 1, "unitId");
            int unitStep = parsePositiveInteger(params.get("unitStep"), 1, "unitStep");
            int connectTimeoutMs = parsePositiveInteger(params.get("connectTimeoutMs"), MODBUS_TCP_DEFAULT_TIMEOUT_MS,
                    "connectTimeoutMs");
            int readTimeoutMs = parsePositiveInteger(params.get("readTimeoutMs"), MODBUS_TCP_DEFAULT_TIMEOUT_MS,
                    "readTimeoutMs");
            List<Integer> unitIds = parseIntegerList(params.get("unitIds"), "unitIds");
            Map<String, RegisterSpec> registerSpecs = parseRegisterSpecs(gateway.getTopic());
            return new ModbusTcpProfile(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : MODBUS_TCP_DEFAULT_PORT,
                    unitId, unitStep, unitIds, connectTimeoutMs, readTimeoutMs, registerSpecs,
                    buildRegisterWindow(registerSpecs));
        }
        catch (URISyntaxException ex)
        {
            throw new IllegalArgumentException("ModbusTCP 采集地址格式无效: " + rawEndpoint, ex);
        }
    }

    private Map<String, String> parseQueryParams(String rawQuery)
    {
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isBlank(rawQuery))
        {
            return params;
        }
        for (String pair : rawQuery.split("&"))
        {
            if (StringUtils.isBlank(pair))
            {
                continue;
            }
            String[] parts = pair.split("=", 2);
            String key = decodeUrlComponent(parts[0]);
            String value = parts.length > 1 ? decodeUrlComponent(parts[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private boolean startsWithAnyIgnoreCase(String value, String... prefixes)
    {
        if (StringUtils.isBlank(value) || prefixes == null)
        {
            return false;
        }
        for (String prefix : prefixes)
        {
            if (StringUtils.startsWithIgnoreCase(value, prefix))
            {
                return true;
            }
        }
        return false;
    }

    private String decodeUrlComponent(String value)
    {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private int parsePositiveInteger(String rawValue, int defaultValue, String fieldName)
    {
        if (StringUtils.isBlank(rawValue))
        {
            return defaultValue;
        }
        try
        {
            int value = Integer.parseInt(rawValue.trim());
            if (value < 1)
            {
                throw new IllegalArgumentException(fieldName + " 必须大于 0");
            }
            return value;
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException(fieldName + " 必须为整数", ex);
        }
    }

    private List<Integer> parseIntegerList(String rawValue, String fieldName)
    {
        List<Integer> values = new ArrayList<>();
        if (StringUtils.isBlank(rawValue))
        {
            return values;
        }
        for (String token : rawValue.split(","))
        {
            if (StringUtils.isBlank(token))
            {
                continue;
            }
            values.add(parsePositiveInteger(token, 1, fieldName));
        }
        return values;
    }

    private Map<String, RegisterSpec> parseRegisterSpecs(String rawProfile)
    {
        Map<String, RegisterSpec> registerSpecs = defaultRegisterSpecs();
        if (StringUtils.isBlank(rawProfile) || !rawProfile.contains("="))
        {
            return registerSpecs;
        }
        for (String segment : rawProfile.split(";"))
        {
            if (StringUtils.isBlank(segment))
            {
                continue;
            }
            String[] parts = segment.split("=", 2);
            if (parts.length != 2)
            {
                throw new IllegalArgumentException("寄存器映射格式无效: " + segment);
            }
            String metric = parts[0].trim();
            if (!registerSpecs.containsKey(metric))
            {
                throw new IllegalArgumentException("不支持的寄存器指标: " + metric);
            }
            registerSpecs.put(metric, parseRegisterSpec(metric, parts[1].trim()));
        }
        return registerSpecs;
    }

    private Map<String, RegisterSpec> defaultRegisterSpecs()
    {
        Map<String, RegisterSpec> registerSpecs = new LinkedHashMap<>();
        registerSpecs.put("power", new RegisterSpec(0, 2, new BigDecimal("0.1"), false));
        registerSpecs.put("dailyYield", new RegisterSpec(2, 2, new BigDecimal("0.1"), false));
        registerSpecs.put("totalYield", new RegisterSpec(4, 2, new BigDecimal("0.1"), false));
        registerSpecs.put("voltage", new RegisterSpec(6, 1, new BigDecimal("0.1"), false));
        registerSpecs.put("current", new RegisterSpec(7, 1, new BigDecimal("0.01"), false));
        return registerSpecs;
    }

    private RegisterSpec parseRegisterSpec(String metric, String expression)
    {
        String[] parts = expression.split(":");
        if (parts.length < 3 || parts.length > 4)
        {
            throw new IllegalArgumentException("寄存器映射格式无效: " + metric + "=" + expression);
        }
        try
        {
            int address = Integer.parseInt(parts[0].trim());
            int quantity = Integer.parseInt(parts[1].trim());
            if (address < 0)
            {
                throw new IllegalArgumentException(metric + " 寄存器地址不能为负数");
            }
            if (quantity < 1 || quantity > 2)
            {
                throw new IllegalArgumentException(metric + " 仅支持读取 1 或 2 个寄存器");
            }
            BigDecimal scale = new BigDecimal(parts[2].trim());
            boolean signed = parts.length == 4
                    && ("signed".equalsIgnoreCase(parts[3].trim()) || Boolean.parseBoolean(parts[3].trim()));
            return new RegisterSpec(address, quantity, scale, signed);
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException("寄存器映射数值无效: " + metric + "=" + expression, ex);
        }
    }

    private RegisterWindow buildRegisterWindow(Map<String, RegisterSpec> registerSpecs)
    {
        int startAddress = Integer.MAX_VALUE;
        int endAddress = 0;
        for (RegisterSpec registerSpec : registerSpecs.values())
        {
            startAddress = Math.min(startAddress, registerSpec.getAddress());
            endAddress = Math.max(endAddress, registerSpec.getAddress() + registerSpec.getQuantity());
        }
        int quantity = endAddress - startAddress;
        if (quantity < 1 || quantity > MODBUS_MAX_REGISTER_COUNT)
        {
            throw new IllegalArgumentException("寄存器映射窗口超出 Modbus 读取上限");
        }
        return new RegisterWindow(startAddress, quantity);
    }

    private int resolveUnitId(ModbusTcpProfile profile, int index)
    {
        if (index < profile.getUnitIds().size())
        {
            return profile.getUnitIds().get(index);
        }
        return profile.getUnitId() + index * profile.getUnitStep();
    }

    private int[] executeModbusTcpRead(String host, int port, int unitId, int connectTimeoutMs, int readTimeoutMs,
            int startAddress, int quantity)
    {
        int transactionId = ThreadLocalRandom.current().nextInt(1, 65535);
        try (Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());

            output.writeShort(transactionId);
            output.writeShort(0);
            output.writeShort(6);
            output.writeByte(unitId);
            output.writeByte(MODBUS_READ_HOLDING_REGISTERS);
            output.writeShort(startAddress);
            output.writeShort(quantity);
            output.flush();

            int responseTransactionId = input.readUnsignedShort();
            int protocolId = input.readUnsignedShort();
            input.readUnsignedShort();
            int responseUnitId = input.readUnsignedByte();
            int functionCode = input.readUnsignedByte();
            if (functionCode == (MODBUS_READ_HOLDING_REGISTERS | 0x80))
            {
                throw new IllegalStateException("Modbus 设备返回异常码: " + input.readUnsignedByte());
            }
            int byteCount = input.readUnsignedByte();
            if (responseTransactionId != transactionId || protocolId != 0 || responseUnitId != unitId
                    || functionCode != MODBUS_READ_HOLDING_REGISTERS)
            {
                throw new IllegalStateException("Modbus 响应头不匹配");
            }
            if (byteCount != quantity * 2)
            {
                throw new IllegalStateException("Modbus 响应字节数异常: " + byteCount);
            }

            int[] registers = new int[quantity];
            for (int index = 0; index < registers.length; index++)
            {
                registers[index] = input.readUnsignedShort();
            }
            return registers;
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("ModbusTCP 采集失败: " + host + ":" + port + " unitId=" + unitId, ex);
        }
    }

    private BigDecimal decodeRegisterValue(int[] registers, RegisterWindow window, RegisterSpec registerSpec)
    {
        int offset = registerSpec.getAddress() - window.getStartAddress();
        if (offset < 0 || offset + registerSpec.getQuantity() > registers.length)
        {
            throw new IllegalStateException("寄存器响应窗口与配置不匹配");
        }

        long rawValue;
        if (registerSpec.getQuantity() == 1)
        {
            rawValue = registers[offset];
            if (registerSpec.isSigned() && rawValue > 0x7FFF)
            {
                rawValue -= 0x10000L;
            }
        }
        else
        {
            rawValue = ((long) registers[offset] << 16) | registers[offset + 1];
            if (registerSpec.isSigned() && rawValue > 0x7FFFFFFFL)
            {
                rawValue -= 0x1_0000_0000L;
            }
        }
        return BigDecimal.valueOf(rawValue).multiply(registerSpec.getScale());
    }

    private BigDecimal normalizeTelemetryValue(BigDecimal value)
    {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0)
        {
            return zero();
        }
        return scale(value);
    }

    private BigDecimal buildPolledPower(PvGateway gateway, PvInverter inverter, int index, LocalDateTime nowTime)
    {
        long gatewayId = gateway.getGatewayId() == null ? 0L : gateway.getGatewayId();
        long inverterId = inverter.getInverterId() == null ? index + 1L : inverter.getInverterId();
        int minute = nowTime.getMinute();
        double base = "ModbusRTU".equalsIgnoreCase(gateway.getProtocol()) ? 18.0 : 26.0;
        double swing = ((gatewayId * 11 + inverterId * 7 + minute * 5 + index * 3) % 70) * 0.9;
        return scale(BigDecimal.valueOf(base + swing));
    }

    private BigDecimal buildPolledYieldIncrement(BigDecimal currentPower)
    {
        return scale(currentPower.multiply(BigDecimal.valueOf(0.035)));
    }

    private BigDecimal buildPolledTotalYield(PvGateway gateway, PvInverter inverter, int index, BigDecimal dailyYield)
    {
        long gatewayId = gateway.getGatewayId() == null ? 0L : gateway.getGatewayId();
        long inverterId = inverter.getInverterId() == null ? index + 1L : inverter.getInverterId();
        BigDecimal historicalOffset = BigDecimal.valueOf(120 + (gatewayId + inverterId + index) % 360);
        return scale(dailyYield.multiply(BigDecimal.valueOf(31)).add(historicalOffset));
    }

    private BigDecimal buildPolledVoltage(PvGateway gateway, int index)
    {
        double base = "ModbusRTU".equalsIgnoreCase(gateway.getProtocol()) ? 390.0 : 520.0;
        return scale(BigDecimal.valueOf(base + (index % 5) * 4.5));
    }

    private BigDecimal buildPolledCurrent(BigDecimal currentPower)
    {
        return scale(currentPower.divide(BigDecimal.valueOf(9.5), 2, RoundingMode.HALF_UP));
    }

    private void mergeGatewayStatus(Map<Long, GatewaySnapshot> gatewaySnapshots, Long gatewayId, String status, Date lastSeen)
    {
        if (gatewayId == null)
        {
            return;
        }
        GatewaySnapshot snapshot = gatewaySnapshots.computeIfAbsent(gatewayId, key -> new GatewaySnapshot());
        if (rankStatus(status) >= rankStatus(snapshot.getStatus()))
        {
            snapshot.setStatus(status);
        }
        snapshot.setLastSeen(lastSeen);
    }

    private int rankStatus(String status)
    {
        if ("online".equals(status))
        {
            return 3;
        }
        if ("fault".equals(status))
        {
            return 2;
        }
        return 1;
    }

    private PvAlert buildAlert(PvInverter inverter, String status, String operator, Date now)
    {
        PvAlert alert = new PvAlert();
        alert.setLevel("fault".equals(status) ? "critical" : "warning");
        alert.setContent("fault".equals(status) ? "逆变器发生故障，请尽快排查" : "逆变器通信中断");
        alert.setSource(buildAlertSource(inverter));
        alert.setOccurTime(now);
        alert.setStatus("active");
        alert.setCreateBy(operator);
        alert.setCreateTime(now);
        return alert;
    }

    private String buildAlertSource(PvInverter inverter)
    {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(inverter.getGatewayName()))
        {
            parts.add(inverter.getGatewayName());
        }
        if (StringUtils.isNotBlank(inverter.getInverterNumber()))
        {
            parts.add(inverter.getInverterNumber());
        }
        else if (StringUtils.isNotBlank(inverter.getSerialNumber()))
        {
            parts.add(inverter.getSerialNumber());
        }
        return parts.isEmpty() ? "逆变器" : String.join(" / ", parts);
    }

    private BigDecimal randomDecimal(double min, double max)
    {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(min, max)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value)
    {
        return value == null ? zero() : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero()
    {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private void evictDashboardSummaryCache()
    {
        redisCache.deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    private void publishDashboardRefresh(String source)
    {
        if (dashboardRefreshPublisher != null)
        {
            dashboardRefreshPublisher.publish(source);
        }
    }

    private void publishAlertCreated(PvAlert alert)
    {
        if (alertCreatedPublisher != null)
        {
            alertCreatedPublisher.publish(alert);
        }
    }

    private static class GatewaySnapshot
    {
        private String status = "offline";

        private Date lastSeen;

        public String getStatus()
        {
            return status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }

        public Date getLastSeen()
        {
            return lastSeen;
        }

        public void setLastSeen(Date lastSeen)
        {
            this.lastSeen = lastSeen;
        }
    }

    private static class PolledTelemetrySnapshot
    {
        private final BigDecimal currentPower;

        private final BigDecimal dailyYield;

        private final BigDecimal totalYield;

        private final BigDecimal voltage;

        private final BigDecimal current;

        PolledTelemetrySnapshot(BigDecimal currentPower, BigDecimal dailyYield, BigDecimal totalYield, BigDecimal voltage,
                BigDecimal current)
        {
            this.currentPower = currentPower;
            this.dailyYield = dailyYield;
            this.totalYield = totalYield;
            this.voltage = voltage;
            this.current = current;
        }

        public BigDecimal getCurrentPower()
        {
            return currentPower;
        }

        public BigDecimal getDailyYield()
        {
            return dailyYield;
        }

        public BigDecimal getTotalYield()
        {
            return totalYield;
        }

        public BigDecimal getVoltage()
        {
            return voltage;
        }

        public BigDecimal getCurrent()
        {
            return current;
        }
    }

    private static class RegisterSpec
    {
        private final int address;

        private final int quantity;

        private final BigDecimal scale;

        private final boolean signed;

        RegisterSpec(int address, int quantity, BigDecimal scale, boolean signed)
        {
            this.address = address;
            this.quantity = quantity;
            this.scale = scale;
            this.signed = signed;
        }

        public int getAddress()
        {
            return address;
        }

        public int getQuantity()
        {
            return quantity;
        }

        public BigDecimal getScale()
        {
            return scale;
        }

        public boolean isSigned()
        {
            return signed;
        }
    }

    private static class RegisterWindow
    {
        private final int startAddress;

        private final int quantity;

        RegisterWindow(int startAddress, int quantity)
        {
            this.startAddress = startAddress;
            this.quantity = quantity;
        }

        public int getStartAddress()
        {
            return startAddress;
        }

        public int getQuantity()
        {
            return quantity;
        }
    }

    private static class ModbusTcpProfile
    {
        private final String host;

        private final int port;

        private final int unitId;

        private final int unitStep;

        private final List<Integer> unitIds;

        private final int connectTimeoutMs;

        private final int readTimeoutMs;

        private final Map<String, RegisterSpec> registerSpecs;

        private final RegisterWindow window;

        ModbusTcpProfile(String host, int port, int unitId, int unitStep, List<Integer> unitIds, int connectTimeoutMs,
                int readTimeoutMs, Map<String, RegisterSpec> registerSpecs, RegisterWindow window)
        {
            this.host = host;
            this.port = port;
            this.unitId = unitId;
            this.unitStep = unitStep;
            this.unitIds = unitIds;
            this.connectTimeoutMs = connectTimeoutMs;
            this.readTimeoutMs = readTimeoutMs;
            this.registerSpecs = registerSpecs;
            this.window = window;
        }

        public String getHost()
        {
            return host;
        }

        public int getPort()
        {
            return port;
        }

        public int getUnitId()
        {
            return unitId;
        }

        public int getUnitStep()
        {
            return unitStep;
        }

        public List<Integer> getUnitIds()
        {
            return unitIds;
        }

        public int getConnectTimeoutMs()
        {
            return connectTimeoutMs;
        }

        public int getReadTimeoutMs()
        {
            return readTimeoutMs;
        }

        public Map<String, RegisterSpec> getRegisterSpecs()
        {
            return registerSpecs;
        }

        public RegisterWindow getWindow()
        {
            return window;
        }
    }

    @FunctionalInterface
    public interface ModbusTcpRegisterReader
    {
        int[] read(String host, int port, int unitId, int connectTimeoutMs, int readTimeoutMs, int startAddress,
                int quantity);
    }
}
