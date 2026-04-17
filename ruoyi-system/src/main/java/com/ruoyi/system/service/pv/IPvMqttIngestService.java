package com.ruoyi.system.service.pv;

public interface IPvMqttIngestService
{
    void refreshSubscriptions();

    int ingestMessage(String brokerUrl, String topic, String payload);
}
