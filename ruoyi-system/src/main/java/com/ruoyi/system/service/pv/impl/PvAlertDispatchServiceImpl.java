package com.ruoyi.system.service.pv.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.pv.PvAlert;
import com.ruoyi.system.domain.pv.PvAlertRule;
import com.ruoyi.system.event.pv.PvAlertCreatedEvent;
import com.ruoyi.system.mapper.pv.PvMonitoringMapper;
import com.ruoyi.system.service.pv.IPvAlertDispatchService;

@Service
public class PvAlertDispatchServiceImpl implements IPvAlertDispatchService
{
    private static final Logger log = LoggerFactory.getLogger(PvAlertDispatchServiceImpl.class);
    private static final int DEFAULT_WEBHOOK_TIMEOUT_SECONDS = 5;

    @Autowired
    private PvMonitoringMapper monitoringMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired(required = false)
    @Qualifier("threadPoolTaskExecutor")
    private TaskExecutor taskExecutor;

    private AlertChannelTransport alertChannelTransport = this::sendByDefaultTransport;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAlertCreated(PvAlertCreatedEvent event)
    {
        if (event == null || event.getAlert() == null)
        {
            return;
        }
        handleAlertCreated(event.getAlert());
    }

    @Override
    public void handleAlertCreated(PvAlert alert)
    {
        if (alert == null || !"active".equals(alert.getStatus()) || StringUtils.isBlank(alert.getLevel()))
        {
            return;
        }
        Runnable task = () -> dispatchAlert(alert);
        if (taskExecutor != null)
        {
            taskExecutor.execute(task);
        }
        else
        {
            task.run();
        }
    }

    public void setAlertChannelTransport(AlertChannelTransport alertChannelTransport)
    {
        this.alertChannelTransport = alertChannelTransport == null ? this::sendByDefaultTransport : alertChannelTransport;
    }

    void dispatchAlert(PvAlert alert)
    {
        List<PvAlertRule> rules = monitoringMapper.selectAlertRulesByLevel(alert.getLevel());
        if (rules == null || rules.isEmpty())
        {
            return;
        }
        for (PvAlertRule rule : rules)
        {
            if (rule == null || !rule.isChannelEnabled() || StringUtils.isBlank(rule.getChannelType())
                    || StringUtils.isBlank(rule.getTarget()))
            {
                continue;
            }
            String throttleKey = buildThrottleKey(rule, alert);
            Integer throttleSec = normalizeThrottle(rule.getThrottleSec());
            if (throttleSec > 0 && Boolean.TRUE.equals(redisCache.hasKey(throttleKey)))
            {
                log.debug("PV alert dispatch throttled. ruleId={}, level={}", rule.getRuleId(), alert.getLevel());
                continue;
            }
            if (alertChannelTransport.send(rule, alert))
            {
                if (throttleSec > 0)
                {
                    redisCache.setCacheObject(throttleKey, "1", throttleSec, TimeUnit.SECONDS);
                }
                log.info("PV alert dispatched. ruleId={}, channelType={}, target={}, level={}", rule.getRuleId(),
                        rule.getChannelType(), rule.getTarget(), alert.getLevel());
            }
        }
    }

    private Integer normalizeThrottle(Integer throttleSec)
    {
        return throttleSec == null || throttleSec < 0 ? 0 : throttleSec;
    }

    private String buildThrottleKey(PvAlertRule rule, PvAlert alert)
    {
        return CacheConstants.PV_ALERT_THROTTLE_KEY + rule.getRuleId() + ":" + alert.getLevel();
    }

    private boolean sendByDefaultTransport(PvAlertRule rule, PvAlert alert)
    {
        if ("webhook".equalsIgnoreCase(rule.getChannelType()))
        {
            return sendWebhook(rule.getTarget(), buildWebhookPayload(rule, alert));
        }
        if ("dingtalk".equalsIgnoreCase(rule.getChannelType()))
        {
            return sendWebhook(rule.getTarget(), buildDingTalkPayload(alert));
        }
        if ("email".equalsIgnoreCase(rule.getChannelType()) || "sms".equalsIgnoreCase(rule.getChannelType()))
        {
            log.info("PV alert channel accepted by stub transport. channelType={}, target={}, level={}, source={}",
                    rule.getChannelType(), rule.getTarget(), alert.getLevel(), alert.getSource());
            return true;
        }
        log.warn("PV alert channel type unsupported. ruleId={}, channelType={}", rule.getRuleId(),
                rule.getChannelType());
        return false;
    }

    private boolean sendWebhook(String target, String body)
    {
        try
        {
            HttpRequest request = HttpRequest.newBuilder(URI.create(target))
                    .timeout(Duration.ofSeconds(DEFAULT_WEBHOOK_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        }
        catch (IOException | InterruptedException | IllegalArgumentException ex)
        {
            if (ex instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            log.warn("PV alert webhook send failed. target={}, message={}", target, ex.getMessage());
            return false;
        }
    }

    private String buildWebhookPayload(PvAlertRule rule, PvAlert alert)
    {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruleId", rule.getRuleId());
        payload.put("channelType", rule.getChannelType());
        payload.put("level", alert.getLevel());
        payload.put("content", alert.getContent());
        payload.put("source", alert.getSource());
        payload.put("occurTime", alert.getOccurTime());
        payload.put("status", alert.getStatus());
        return JSON.toJSONString(payload);
    }

    private String buildDingTalkPayload(PvAlert alert)
    {
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("content", String.format("MyEMS-PV 告警\n级别: %s\n来源: %s\n内容: %s", alert.getLevel(),
                StringUtils.defaultIfBlank(alert.getSource(), "-"), StringUtils.defaultIfBlank(alert.getContent(), "-")));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgtype", "text");
        payload.put("text", text);
        return JSON.toJSONString(payload);
    }

    @FunctionalInterface
    public interface AlertChannelTransport
    {
        boolean send(PvAlertRule rule, PvAlert alert);
    }
}
