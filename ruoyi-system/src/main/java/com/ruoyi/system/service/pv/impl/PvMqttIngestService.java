package com.ruoyi.system.service.pv.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.domain.pv.PvTelemetry;
import com.ruoyi.system.event.pv.PvDashboardRefreshPublisher;
import com.ruoyi.system.mapper.pv.PvAssetMapper;
import com.ruoyi.system.metrics.pv.PvMetricsRecorder;
import com.ruoyi.system.service.pv.IPvMqttIngestService;

@Service
public class PvMqttIngestService implements IPvMqttIngestService
{
    private static final Logger log = LoggerFactory.getLogger(PvMqttIngestService.class);
    private static final String DASHBOARD_SUMMARY_CACHE_KEY = CacheConstants.PV_DASHBOARD_SUMMARY_KEY + "all";
    private static final int DEFAULT_MQTT_PORT = 1883;
    private static final int DEFAULT_MQTTS_PORT = 8883;
    private static final int DEFAULT_QOS = 1;
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;
    private static final long DEFAULT_COMPLETION_TIMEOUT_MS = 5000L;

    @Autowired
    private PvAssetMapper assetMapper;

    @Autowired(required = false)
    private IntegrationFlowContext integrationFlowContext;

    @Autowired
    private RedisCache redisCache;

    @Autowired(required = false)
    private PvDashboardRefreshPublisher dashboardRefreshPublisher;

    @Autowired(required = false)
    private PvMetricsRecorder metricsRecorder;

    private final Map<String, BrokerSubscription> activeSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, IntegrationFlowContext.IntegrationFlowRegistration> flowRegistrations = new ConcurrentHashMap<>();

    private MqttPayloadDecoder mqttPayloadDecoder = this::decodePayload;
    private MqttSubscriptionRegistrar mqttSubscriptionRegistrar = this::registerBrokerSubscription;
    private MqttSubscriptionUnregistrar mqttSubscriptionUnregistrar = this::unregisterBrokerSubscription;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady()
    {
        refreshSubscriptions();
    }

    @Override
    public synchronized void refreshSubscriptions()
    {
        Map<String, BrokerSubscription> desired = loadBrokerSubscriptions();

        for (String brokerKey : new ArrayList<>(activeSubscriptions.keySet()))
        {
            if (desired.containsKey(brokerKey))
            {
                continue;
            }
            mqttSubscriptionUnregistrar.unregister(brokerKey);
            activeSubscriptions.remove(brokerKey);
        }

        for (Map.Entry<String, BrokerSubscription> entry : desired.entrySet())
        {
            BrokerSubscription next = entry.getValue();
            BrokerSubscription current = activeSubscriptions.get(entry.getKey());
            if (next.equals(current))
            {
                continue;
            }
            if (current != null)
            {
                mqttSubscriptionUnregistrar.unregister(entry.getKey());
            }
            mqttSubscriptionRegistrar.register(next);
            activeSubscriptions.put(entry.getKey(), next);
        }
    }

    @Override
    @Transactional
    public int ingestMessage(String brokerUrl, String topic, String payload)
    {
        recordMqttMessage(topic);
        if (StringUtils.isBlank(topic) || StringUtils.isBlank(payload))
        {
            return 0;
        }

        BrokerSubscription brokerSubscription = resolveBrokerSubscription(brokerUrl);
        if (brokerSubscription == null)
        {
            log.debug("PV MQTT message ignored because broker is not configured. brokerUrl={}, topic={}", brokerUrl, topic);
            return 0;
        }

        List<PvGateway> matchedGateways = brokerSubscription.matchGateways(topic);
        if (matchedGateways.isEmpty())
        {
            log.debug("PV MQTT message ignored because no gateway topic matched. brokerUrl={}, topic={}", brokerUrl, topic);
            return 0;
        }

        int insertedRows = 0;
        for (PvGateway gateway : matchedGateways)
        {
            insertedRows += ingestGatewayMessage(gateway, topic, payload);
        }
        recordTelemetryIngest(insertedRows);
        return insertedRows;
    }

    public void setMqttPayloadDecoder(MqttPayloadDecoder mqttPayloadDecoder)
    {
        this.mqttPayloadDecoder = mqttPayloadDecoder == null ? this::decodePayload : mqttPayloadDecoder;
    }

    public void setMqttSubscriptionRegistrar(MqttSubscriptionRegistrar mqttSubscriptionRegistrar)
    {
        this.mqttSubscriptionRegistrar = mqttSubscriptionRegistrar == null ? this::registerBrokerSubscription
                : mqttSubscriptionRegistrar;
    }

    public void setMqttSubscriptionUnregistrar(MqttSubscriptionUnregistrar mqttSubscriptionUnregistrar)
    {
        this.mqttSubscriptionUnregistrar = mqttSubscriptionUnregistrar == null ? this::unregisterBrokerSubscription
                : mqttSubscriptionUnregistrar;
    }

    private int ingestGatewayMessage(PvGateway gateway, String topic, String payload)
    {
        PvInverter query = new PvInverter();
        query.setGatewayId(gateway.getGatewayId());
        List<PvInverter> inverters = assetMapper.selectInverterList(query);
        if (inverters.isEmpty())
        {
            assetMapper.updateGatewayStatus(gateway.getGatewayId(), "online", DateUtils.getNowDate());
            return 0;
        }

        MqttTelemetryEnvelope envelope;
        try
        {
            envelope = mqttPayloadDecoder.decode(gateway, topic, payload);
        }
        catch (RuntimeException ex)
        {
            log.warn("PV MQTT payload parse failed. gatewayId={}, topic={}, message={}", gateway.getGatewayId(), topic,
                    ex.getMessage());
            return 0;
        }

        if (envelope.getSamples().isEmpty())
        {
            assetMapper.updateGatewayStatus(gateway.getGatewayId(), "online", envelope.getReceivedAt());
            return 0;
        }

        Map<String, PvInverter> serialIndex = new HashMap<>();
        Map<String, PvInverter> numberIndex = new HashMap<>();
        for (PvInverter inverter : inverters)
        {
            if (StringUtils.isNotBlank(inverter.getSerialNumber()))
            {
                serialIndex.put(inverter.getSerialNumber(), inverter);
            }
            if (StringUtils.isNotBlank(inverter.getInverterNumber()))
            {
                numberIndex.put(inverter.getInverterNumber(), inverter);
            }
        }

        List<PvTelemetry> telemetryList = new ArrayList<>();
        Date gatewayLastSeen = envelope.getReceivedAt();
        for (MqttTelemetrySample sample : envelope.getSamples())
        {
            PvInverter inverter = resolveInverter(sample, inverters, serialIndex, numberIndex);
            if (inverter == null)
            {
                log.warn("PV MQTT sample ignored because inverter could not be resolved. gatewayId={}, topic={}, sample={}",
                        gateway.getGatewayId(), topic, sample.describe());
                continue;
            }

            Date collectTime = sample.getCollectTime() == null ? envelope.getReceivedAt() : sample.getCollectTime();
            BigDecimal currentPower = normalizeDecimal(sample.getActivePower());
            BigDecimal dailyYield = sample.getDailyYield() == null ? baseDailyYield(inverter, collectTime)
                    : normalizeDecimal(sample.getDailyYield());
            BigDecimal totalYield = sample.getTotalYield() == null ? normalizeDecimal(dailyYield)
                    : normalizeDecimal(sample.getTotalYield());
            BigDecimal voltage = normalizeDecimal(sample.getVoltage());
            BigDecimal current = normalizeDecimal(sample.getCurrent());

            PvTelemetry telemetry = new PvTelemetry();
            telemetry.setInverterId(inverter.getInverterId());
            telemetry.setActivePower(currentPower);
            telemetry.setDailyYield(dailyYield);
            telemetry.setTotalYield(totalYield);
            telemetry.setVoltage(voltage);
            telemetry.setCurrent(current);
            telemetry.setCollectTime(collectTime);
            telemetry.setLegacyFirebaseId(sample.getLegacyFirebaseId());
            telemetryList.add(telemetry);

            assetMapper.updateInverterStatus(inverter.getInverterId(), resolveStatus(sample, currentPower), collectTime,
                    currentPower, dailyYield);
            if (collectTime.after(gatewayLastSeen))
            {
                gatewayLastSeen = collectTime;
            }
        }

        if (telemetryList.isEmpty())
        {
            assetMapper.updateGatewayStatus(gateway.getGatewayId(), "online", gatewayLastSeen);
            return 0;
        }

        assetMapper.insertTelemetryBatch(telemetryList);
        assetMapper.updateGatewayStatus(gateway.getGatewayId(), "online", gatewayLastSeen);
        redisCache.deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
        publishDashboardRefresh("mqtt.ingest");
        return telemetryList.size();
    }

    private BrokerSubscription resolveBrokerSubscription(String brokerUrl)
    {
        if (StringUtils.isBlank(brokerUrl))
        {
            return null;
        }
        String brokerKey = parseBrokerOptions(brokerUrl).getBrokerKey();
        BrokerSubscription brokerSubscription = activeSubscriptions.get(brokerKey);
        if (brokerSubscription != null)
        {
            return brokerSubscription;
        }
        refreshSubscriptions();
        return activeSubscriptions.get(brokerKey);
    }

    private Map<String, BrokerSubscription> loadBrokerSubscriptions()
    {
        PvGateway query = new PvGateway();
        query.setCommunicationType("MQTT");
        List<PvGateway> gateways = assetMapper.selectGatewayList(query);
        Map<String, BrokerSubscriptionBuilder> builders = new LinkedHashMap<>();
        for (PvGateway gateway : gateways)
        {
            if (StringUtils.isBlank(gateway.getBrokerUrl()) || StringUtils.isBlank(gateway.getTopic()))
            {
                continue;
            }
            try
            {
                MqttBrokerOptions brokerOptions = parseBrokerOptions(gateway.getBrokerUrl());
                builders.computeIfAbsent(brokerOptions.getBrokerKey(), key -> new BrokerSubscriptionBuilder(brokerOptions))
                        .addGateway(gateway);
            }
            catch (IllegalArgumentException ex)
            {
                log.warn("PV MQTT gateway config ignored. gatewayId={}, brokerUrl={}, message={}", gateway.getGatewayId(),
                        gateway.getBrokerUrl(), ex.getMessage());
            }
        }

        Map<String, BrokerSubscription> result = new LinkedHashMap<>();
        for (Map.Entry<String, BrokerSubscriptionBuilder> entry : builders.entrySet())
        {
            result.put(entry.getKey(), entry.getValue().build());
        }
        return result;
    }

    private void registerBrokerSubscription(BrokerSubscription brokerSubscription)
    {
        if (integrationFlowContext == null || brokerSubscription.getTopics().isEmpty())
        {
            return;
        }

        DefaultMqttPahoClientFactory clientFactory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions connectionOptions = new MqttConnectOptions();
        connectionOptions.setAutomaticReconnect(brokerSubscription.isAutomaticReconnect());
        connectionOptions.setCleanSession(brokerSubscription.isCleanSession());
        connectionOptions.setConnectionTimeout(brokerSubscription.getConnectionTimeoutSeconds());
        connectionOptions.setKeepAliveInterval(brokerSubscription.getKeepAliveSeconds());
        connectionOptions.setServerURIs(new String[] { brokerSubscription.getServerUri() });
        if (StringUtils.isNotBlank(brokerSubscription.getUsername()))
        {
            connectionOptions.setUserName(brokerSubscription.getUsername());
        }
        if (StringUtils.isNotBlank(brokerSubscription.getPassword()))
        {
            connectionOptions.setPassword(brokerSubscription.getPassword().toCharArray());
        }
        clientFactory.setConnectionOptions(connectionOptions);

        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                brokerSubscription.getClientId(), clientFactory, brokerSubscription.getTopics().toArray(new String[0]));
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(false);
        adapter.setConverter(converter);
        adapter.setQos(brokerSubscription.getQos());
        adapter.setCompletionTimeout(brokerSubscription.getCompletionTimeoutMs());

        IntegrationFlow flow = IntegrationFlow.from(adapter)
                .handle(message -> ingestMessage(brokerSubscription.getRawBrokerUrl(),
                        Objects.toString(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC), ""),
                        Objects.toString(message.getPayload(), "")))
                .get();
        IntegrationFlowContext.IntegrationFlowRegistration registration = integrationFlowContext.registration(flow)
                .id(brokerSubscription.getRegistrationId()).register();
        flowRegistrations.put(brokerSubscription.getBrokerKey(), registration);
        log.info("PV MQTT subscription registered. brokerUrl={}, topics={}", brokerSubscription.getRawBrokerUrl(),
                brokerSubscription.getTopics());
    }

    private void unregisterBrokerSubscription(String brokerKey)
    {
        IntegrationFlowContext.IntegrationFlowRegistration registration = flowRegistrations.remove(brokerKey);
        if (registration != null)
        {
            registration.destroy();
        }
    }

    private MqttBrokerOptions parseBrokerOptions(String brokerUrl)
    {
        String raw = brokerUrl == null ? "" : brokerUrl.trim();
        if (StringUtils.isBlank(raw))
        {
            throw new IllegalArgumentException("brokerUrl 不能为空");
        }

        String candidate = raw.contains("://") ? raw : "mqtt://" + raw;
        try
        {
            URI uri = new URI(candidate);
            String scheme = normalizeBrokerScheme(uri.getScheme());
            String host = uri.getHost();
            if (StringUtils.isBlank(host))
            {
                throw new IllegalArgumentException("brokerUrl 缺少主机名");
            }
            int port = uri.getPort() > 0 ? uri.getPort()
                    : ("ssl".equals(scheme) || "wss".equals(scheme) ? DEFAULT_MQTTS_PORT : DEFAULT_MQTT_PORT);
            Map<String, String> queryParams = parseQueryParams(uri.getRawQuery());
            String serverUri = scheme + "://" + host + ":" + port;
            String brokerKey = serverUri + (StringUtils.isBlank(uri.getRawQuery()) ? "" : "?" + uri.getRawQuery());
            String clientId = queryParams.getOrDefault("clientId",
                    "myems-pv-mqtt-" + Integer.toHexString(Math.abs(brokerKey.hashCode())));
            int qos = parseInteger(queryParams.get("qos"), DEFAULT_QOS, 0, 2, "qos");
            int connectionTimeoutSeconds = parseInteger(queryParams.get("connectionTimeoutSec"),
                    DEFAULT_CONNECTION_TIMEOUT_SECONDS, 1, 60, "connectionTimeoutSec");
            int keepAliveSeconds = parseInteger(queryParams.get("keepAliveSec"), DEFAULT_KEEP_ALIVE_SECONDS, 5, 3600,
                    "keepAliveSec");
            long completionTimeoutMs = parseLong(queryParams.get("completionTimeoutMs"), DEFAULT_COMPLETION_TIMEOUT_MS, 1000L,
                    60000L, "completionTimeoutMs");
            boolean cleanSession = parseBoolean(queryParams.get("cleanSession"), true);
            boolean automaticReconnect = parseBoolean(queryParams.get("automaticReconnect"), true);
            return new MqttBrokerOptions(brokerUrl, brokerKey, serverUri, clientId, queryParams.get("username"),
                    queryParams.get("password"), qos, connectionTimeoutSeconds, keepAliveSeconds, completionTimeoutMs,
                    cleanSession, automaticReconnect);
        }
        catch (URISyntaxException ex)
        {
            throw new IllegalArgumentException("brokerUrl 格式无效: " + brokerUrl, ex);
        }
    }

    private String normalizeBrokerScheme(String scheme)
    {
        if (StringUtils.isBlank(scheme) || "mqtt".equalsIgnoreCase(scheme) || "tcp".equalsIgnoreCase(scheme))
        {
            return "tcp";
        }
        if ("mqtts".equalsIgnoreCase(scheme) || "ssl".equalsIgnoreCase(scheme))
        {
            return "ssl";
        }
        if ("ws".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme))
        {
            return scheme.toLowerCase();
        }
        throw new IllegalArgumentException("不支持的 MQTT 协议: " + scheme);
    }

    private Map<String, String> parseQueryParams(String rawQuery)
    {
        Map<String, String> params = new LinkedHashMap<>();
        if (StringUtils.isBlank(rawQuery))
        {
            return params;
        }
        for (String part : rawQuery.split("&"))
        {
            if (StringUtils.isBlank(part))
            {
                continue;
            }
            String[] pair = part.split("=", 2);
            params.put(decodeUrlComponent(pair[0]), pair.length > 1 ? decodeUrlComponent(pair[1]) : "");
        }
        return params;
    }

    private String decodeUrlComponent(String value)
    {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private int parseInteger(String rawValue, int defaultValue, int minValue, int maxValue, String fieldName)
    {
        if (StringUtils.isBlank(rawValue))
        {
            return defaultValue;
        }
        try
        {
            int value = Integer.parseInt(rawValue.trim());
            if (value < minValue || value > maxValue)
            {
                throw new IllegalArgumentException(fieldName + " 超出范围");
            }
            return value;
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException(fieldName + " 必须为整数", ex);
        }
    }

    private long parseLong(String rawValue, long defaultValue, long minValue, long maxValue, String fieldName)
    {
        if (StringUtils.isBlank(rawValue))
        {
            return defaultValue;
        }
        try
        {
            long value = Long.parseLong(rawValue.trim());
            if (value < minValue || value > maxValue)
            {
                throw new IllegalArgumentException(fieldName + " 超出范围");
            }
            return value;
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException(fieldName + " 必须为整数", ex);
        }
    }

    private boolean parseBoolean(String rawValue, boolean defaultValue)
    {
        if (StringUtils.isBlank(rawValue))
        {
            return defaultValue;
        }
        return Boolean.parseBoolean(rawValue.trim());
    }

    private MqttTelemetryEnvelope decodePayload(PvGateway gateway, String topic, String payload)
    {
        String trimmed = payload == null ? "" : payload.trim();
        if (StringUtils.isBlank(trimmed))
        {
            return new MqttTelemetryEnvelope(DateUtils.getNowDate(), Collections.emptyList());
        }

        Date receivedAt = DateUtils.getNowDate();
        if (trimmed.startsWith("["))
        {
            return new MqttTelemetryEnvelope(receivedAt, decodeSampleArray(JSON.parseArray(trimmed), null));
        }

        JSONObject root = JSON.parseObject(trimmed);
        Date rootCollectTime = parseDateValue(firstNonBlank(root, "collectTime", "timestamp", "ts", "reportedAt"));
        JSONArray array = firstArray(root, "samples", "telemetry", "inverters", "data");
        if (array != null)
        {
            return new MqttTelemetryEnvelope(rootCollectTime == null ? receivedAt : rootCollectTime,
                    decodeSampleArray(array, rootCollectTime));
        }
        return new MqttTelemetryEnvelope(rootCollectTime == null ? receivedAt : rootCollectTime,
                List.of(decodeSample(root, rootCollectTime)));
    }

    private List<MqttTelemetrySample> decodeSampleArray(JSONArray array, Date defaultCollectTime)
    {
        List<MqttTelemetrySample> samples = new ArrayList<>();
        for (int index = 0; index < array.size(); index++)
        {
            JSONObject item = array.getJSONObject(index);
            if (item == null)
            {
                continue;
            }
            samples.add(decodeSample(item, defaultCollectTime));
        }
        return samples;
    }

    private MqttTelemetrySample decodeSample(JSONObject object, Date defaultCollectTime)
    {
        MqttTelemetrySample sample = new MqttTelemetrySample();
        sample.setSerialNumber(firstNonBlank(object, "serialNumber", "inverterSerialNumber", "sn"));
        sample.setInverterNumber(firstNonBlank(object, "inverterNumber", "deviceNumber"));
        sample.setStatus(firstNonBlank(object, "status", "state"));
        sample.setActivePower(decimalValue(object, "activePower", "power", "currentPower"));
        sample.setDailyYield(decimalValue(object, "dailyYield", "todayYield"));
        sample.setTotalYield(decimalValue(object, "totalYield", "energyTotal"));
        sample.setVoltage(decimalValue(object, "voltage"));
        sample.setCurrent(decimalValue(object, "current"));
        sample.setCollectTime(parseDateValue(firstNonBlank(object, "collectTime", "timestamp", "ts", "reportedAt")));
        if (sample.getCollectTime() == null)
        {
            sample.setCollectTime(defaultCollectTime);
        }
        sample.setLegacyFirebaseId(firstNonBlank(object, "legacyFirebaseId", "messageId", "id"));
        return sample;
    }

    private JSONArray firstArray(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            Object value = object.get(key);
            if (value instanceof JSONArray)
            {
                return (JSONArray) value;
            }
        }
        return null;
    }

    private String firstNonBlank(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            Object value = object.get(key);
            if (value == null)
            {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (StringUtils.isNotBlank(text))
            {
                return text;
            }
        }
        return null;
    }

    private BigDecimal decimalValue(JSONObject object, String... keys)
    {
        for (String key : keys)
        {
            Object value = object.get(key);
            if (value == null || StringUtils.isBlank(String.valueOf(value)))
            {
                continue;
            }
            try
            {
                return new BigDecimal(String.valueOf(value).trim());
            }
            catch (NumberFormatException ex)
            {
                throw new IllegalArgumentException("数值字段格式无效: " + key, ex);
            }
        }
        return null;
    }

    private Date parseDateValue(String rawValue)
    {
        if (StringUtils.isBlank(rawValue))
        {
            return null;
        }
        if (rawValue.matches("^\\d{10,13}$"))
        {
            long epoch = Long.parseLong(rawValue);
            if (rawValue.length() == 10)
            {
                epoch *= 1000L;
            }
            return Date.from(Instant.ofEpochMilli(epoch));
        }
        Date parsed = DateUtils.parseDate(rawValue);
        if (parsed != null)
        {
            return parsed;
        }
        try
        {
            return DateUtils.toDate(LocalDateTime.ofInstant(Instant.parse(rawValue), ZoneId.systemDefault()));
        }
        catch (RuntimeException ex)
        {
            throw new IllegalArgumentException("时间字段格式无效", ex);
        }
    }

    private PvInverter resolveInverter(MqttTelemetrySample sample, List<PvInverter> inverters, Map<String, PvInverter> serialIndex,
            Map<String, PvInverter> numberIndex)
    {
        if (StringUtils.isNotBlank(sample.getSerialNumber()))
        {
            PvInverter inverter = serialIndex.get(sample.getSerialNumber());
            if (inverter != null)
            {
                return inverter;
            }
        }
        if (StringUtils.isNotBlank(sample.getInverterNumber()))
        {
            PvInverter inverter = numberIndex.get(sample.getInverterNumber());
            if (inverter != null)
            {
                return inverter;
            }
        }
        return inverters.size() == 1 ? inverters.get(0) : null;
    }

    private String resolveStatus(MqttTelemetrySample sample, BigDecimal currentPower)
    {
        if (StringUtils.equalsAnyIgnoreCase(sample.getStatus(), "online", "offline", "fault"))
        {
            return sample.getStatus().toLowerCase();
        }
        return "online";
    }

    private static boolean mqttTopicMatches(String topicFilter, String actualTopic)
    {
        if (StringUtils.equals(topicFilter, actualTopic))
        {
            return true;
        }
        String[] filters = StringUtils.splitPreserveAllTokens(topicFilter, '/');
        String[] topics = StringUtils.splitPreserveAllTokens(actualTopic, '/');
        int topicIndex = 0;
        for (int filterIndex = 0; filterIndex < filters.length; filterIndex++)
        {
            String segment = filters[filterIndex];
            if ("#".equals(segment))
            {
                return true;
            }
            if (topicIndex >= topics.length)
            {
                return false;
            }
            if (!"+".equals(segment) && !Objects.equals(segment, topics[topicIndex]))
            {
                return false;
            }
            topicIndex++;
        }
        return topicIndex == topics.length;
    }

    private BigDecimal normalizeDecimal(BigDecimal value)
    {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? zero() : scale(value);
    }

    private BigDecimal baseDailyYield(PvInverter inverter, Date now)
    {
        if (inverter.getLastSeen() == null)
        {
            return zero();
        }
        String today = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, now);
        String lastSeenDay = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, inverter.getLastSeen());
        return today.equals(lastSeenDay) ? normalizeDecimal(inverter.getDailyYield()) : zero();
    }

    private BigDecimal scale(BigDecimal value)
    {
        return value == null ? zero() : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero()
    {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private void publishDashboardRefresh(String source)
    {
        if (dashboardRefreshPublisher != null)
        {
            dashboardRefreshPublisher.publish(source);
        }
    }

    private void recordMqttMessage(String topic)
    {
        if (metricsRecorder != null)
        {
            metricsRecorder.recordMqttMessage(topic);
        }
    }

    private void recordTelemetryIngest(int rows)
    {
        if (metricsRecorder != null)
        {
            metricsRecorder.recordTelemetryIngest("mqtt", rows);
        }
    }

    @FunctionalInterface
    public interface MqttPayloadDecoder
    {
        MqttTelemetryEnvelope decode(PvGateway gateway, String topic, String payload);
    }

    @FunctionalInterface
    public interface MqttSubscriptionRegistrar
    {
        void register(BrokerSubscription brokerSubscription);
    }

    @FunctionalInterface
    public interface MqttSubscriptionUnregistrar
    {
        void unregister(String brokerKey);
    }

    public static class MqttTelemetryEnvelope
    {
        private final Date receivedAt;

        private final List<MqttTelemetrySample> samples;

        public MqttTelemetryEnvelope(Date receivedAt, List<MqttTelemetrySample> samples)
        {
            this.receivedAt = receivedAt == null ? DateUtils.getNowDate() : receivedAt;
            this.samples = samples == null ? Collections.emptyList() : samples;
        }

        public Date getReceivedAt()
        {
            return receivedAt;
        }

        public List<MqttTelemetrySample> getSamples()
        {
            return samples;
        }
    }

    public static class MqttTelemetrySample
    {
        private String serialNumber;

        private String inverterNumber;

        private String status;

        private BigDecimal activePower;

        private BigDecimal dailyYield;

        private BigDecimal totalYield;

        private BigDecimal voltage;

        private BigDecimal current;

        private Date collectTime;

        private String legacyFirebaseId;

        public String getSerialNumber()
        {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber)
        {
            this.serialNumber = serialNumber;
        }

        public String getInverterNumber()
        {
            return inverterNumber;
        }

        public void setInverterNumber(String inverterNumber)
        {
            this.inverterNumber = inverterNumber;
        }

        public String getStatus()
        {
            return status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }

        public BigDecimal getActivePower()
        {
            return activePower;
        }

        public void setActivePower(BigDecimal activePower)
        {
            this.activePower = activePower;
        }

        public BigDecimal getDailyYield()
        {
            return dailyYield;
        }

        public void setDailyYield(BigDecimal dailyYield)
        {
            this.dailyYield = dailyYield;
        }

        public BigDecimal getTotalYield()
        {
            return totalYield;
        }

        public void setTotalYield(BigDecimal totalYield)
        {
            this.totalYield = totalYield;
        }

        public BigDecimal getVoltage()
        {
            return voltage;
        }

        public void setVoltage(BigDecimal voltage)
        {
            this.voltage = voltage;
        }

        public BigDecimal getCurrent()
        {
            return current;
        }

        public void setCurrent(BigDecimal current)
        {
            this.current = current;
        }

        public Date getCollectTime()
        {
            return collectTime;
        }

        public void setCollectTime(Date collectTime)
        {
            this.collectTime = collectTime;
        }

        public String getLegacyFirebaseId()
        {
            return legacyFirebaseId;
        }

        public void setLegacyFirebaseId(String legacyFirebaseId)
        {
            this.legacyFirebaseId = legacyFirebaseId;
        }

        public String describe()
        {
            return "serialNumber=" + serialNumber + ", inverterNumber=" + inverterNumber + ", status=" + status;
        }
    }

    static class MqttBrokerOptions
    {
        private final String rawBrokerUrl;

        private final String brokerKey;

        private final String serverUri;

        private final String clientId;

        private final String username;

        private final String password;

        private final int qos;

        private final int connectionTimeoutSeconds;

        private final int keepAliveSeconds;

        private final long completionTimeoutMs;

        private final boolean cleanSession;

        private final boolean automaticReconnect;

        MqttBrokerOptions(String rawBrokerUrl, String brokerKey, String serverUri, String clientId, String username,
                String password, int qos, int connectionTimeoutSeconds, int keepAliveSeconds, long completionTimeoutMs,
                boolean cleanSession, boolean automaticReconnect)
        {
            this.rawBrokerUrl = rawBrokerUrl;
            this.brokerKey = brokerKey;
            this.serverUri = serverUri;
            this.clientId = clientId;
            this.username = username;
            this.password = password;
            this.qos = qos;
            this.connectionTimeoutSeconds = connectionTimeoutSeconds;
            this.keepAliveSeconds = keepAliveSeconds;
            this.completionTimeoutMs = completionTimeoutMs;
            this.cleanSession = cleanSession;
            this.automaticReconnect = automaticReconnect;
        }

        public String getRawBrokerUrl()
        {
            return rawBrokerUrl;
        }

        public String getBrokerKey()
        {
            return brokerKey;
        }

        public String getServerUri()
        {
            return serverUri;
        }

        public String getClientId()
        {
            return clientId;
        }

        public String getUsername()
        {
            return username;
        }

        public String getPassword()
        {
            return password;
        }

        public int getQos()
        {
            return qos;
        }

        public int getConnectionTimeoutSeconds()
        {
            return connectionTimeoutSeconds;
        }

        public int getKeepAliveSeconds()
        {
            return keepAliveSeconds;
        }

        public long getCompletionTimeoutMs()
        {
            return completionTimeoutMs;
        }

        public boolean isCleanSession()
        {
            return cleanSession;
        }

        public boolean isAutomaticReconnect()
        {
            return automaticReconnect;
        }
    }

    public static class BrokerSubscription
    {
        private final MqttBrokerOptions brokerOptions;

        private final List<String> topics;

        private final List<PvGateway> gateways;

        BrokerSubscription(MqttBrokerOptions brokerOptions, List<String> topics, List<PvGateway> gateways)
        {
            this.brokerOptions = brokerOptions;
            this.topics = topics;
            this.gateways = gateways;
        }

        public String getBrokerKey()
        {
            return brokerOptions.getBrokerKey();
        }

        public String getRegistrationId()
        {
            return "pvMqtt:" + Integer.toHexString(Math.abs(getBrokerKey().hashCode()));
        }

        public String getRawBrokerUrl()
        {
            return brokerOptions.getRawBrokerUrl();
        }

        public String getServerUri()
        {
            return brokerOptions.getServerUri();
        }

        public String getClientId()
        {
            return brokerOptions.getClientId();
        }

        public String getUsername()
        {
            return brokerOptions.getUsername();
        }

        public String getPassword()
        {
            return brokerOptions.getPassword();
        }

        public int getQos()
        {
            return brokerOptions.getQos();
        }

        public int getConnectionTimeoutSeconds()
        {
            return brokerOptions.getConnectionTimeoutSeconds();
        }

        public int getKeepAliveSeconds()
        {
            return brokerOptions.getKeepAliveSeconds();
        }

        public long getCompletionTimeoutMs()
        {
            return brokerOptions.getCompletionTimeoutMs();
        }

        public boolean isCleanSession()
        {
            return brokerOptions.isCleanSession();
        }

        public boolean isAutomaticReconnect()
        {
            return brokerOptions.isAutomaticReconnect();
        }

        public List<String> getTopics()
        {
            return topics;
        }

        public List<PvGateway> matchGateways(String actualTopic)
        {
            List<PvGateway> matched = new ArrayList<>();
            for (PvGateway gateway : gateways)
            {
                if (gateway != null && StringUtils.isNotBlank(gateway.getTopic())
                        && mqttTopicMatches(gateway.getTopic(), actualTopic))
                {
                    matched.add(gateway);
                }
            }
            return matched;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof BrokerSubscription))
            {
                return false;
            }
            BrokerSubscription other = (BrokerSubscription) obj;
            return Objects.equals(getBrokerKey(), other.getBrokerKey()) && Objects.equals(topics, other.topics)
                    && Objects.equals(gatewaySignature(), other.gatewaySignature());
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(getBrokerKey(), topics, gatewaySignature());
        }

        private List<String> gatewaySignature()
        {
            List<String> signature = new ArrayList<>();
            for (PvGateway gateway : gateways)
            {
                signature.add(gateway.getGatewayId() + ":" + gateway.getTopic());
            }
            return signature;
        }
    }

    static class BrokerSubscriptionBuilder
    {
        private final MqttBrokerOptions brokerOptions;

        private final List<String> topics = new ArrayList<>();

        private final List<PvGateway> gateways = new ArrayList<>();

        BrokerSubscriptionBuilder(MqttBrokerOptions brokerOptions)
        {
            this.brokerOptions = brokerOptions;
        }

        void addGateway(PvGateway gateway)
        {
            gateways.add(gateway);
            if (!topics.contains(gateway.getTopic()))
            {
                topics.add(gateway.getTopic());
            }
        }

        BrokerSubscription build()
        {
            gateways.sort(Comparator.comparing(PvGateway::getGatewayId));
            return new BrokerSubscription(brokerOptions, new ArrayList<>(topics), new ArrayList<>(gateways));
        }
    }
}
