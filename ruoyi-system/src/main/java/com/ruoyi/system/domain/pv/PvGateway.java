package com.ruoyi.system.domain.pv;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.ruoyi.common.core.domain.BaseEntity;

public class PvGateway extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long gatewayId;

    private Long stationId;

    private String stationName;

    private String gatewayName;

    private String gatewayType;

    private String serialNumber;

    private String status;

    private String communicationType;

    private String protocol;

    private String brokerUrl;

    private String topic;

    private Integer pollingIntervalSec;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeen;

    private String legacyFirebaseId;

    public Long getGatewayId()
    {
        return gatewayId;
    }

    public void setGatewayId(Long gatewayId)
    {
        this.gatewayId = gatewayId;
    }

    @NotNull(message = "所属电站不能为空")
    public Long getStationId()
    {
        return stationId;
    }

    public void setStationId(Long stationId)
    {
        this.stationId = stationId;
    }

    public String getStationName()
    {
        return stationName;
    }

    public void setStationName(String stationName)
    {
        this.stationName = stationName;
    }

    @NotBlank(message = "网关名称不能为空")
    @Size(max = 128, message = "网关名称长度不能超过128个字符")
    public String getGatewayName()
    {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName)
    {
        this.gatewayName = gatewayName;
    }

    @NotBlank(message = "网关类型不能为空")
    @Size(max = 32, message = "网关类型长度不能超过32个字符")
    public String getGatewayType()
    {
        return gatewayType;
    }

    public void setGatewayType(String gatewayType)
    {
        this.gatewayType = gatewayType;
    }

    @NotBlank(message = "网关序列号不能为空")
    @Size(max = 128, message = "网关序列号长度不能超过128个字符")
    public String getSerialNumber()
    {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber)
    {
        this.serialNumber = serialNumber;
    }

    @Size(max = 16, message = "网关状态长度不能超过16个字符")
    @Pattern(regexp = "online|offline|fault", message = "网关状态只能为online、offline或fault")
    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @NotBlank(message = "通讯方式不能为空")
    @Size(max = 32, message = "通讯方式长度不能超过32个字符")
    public String getCommunicationType()
    {
        return communicationType;
    }

    public void setCommunicationType(String communicationType)
    {
        this.communicationType = communicationType;
    }

    @Size(max = 32, message = "采集协议长度不能超过32个字符")
    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    @Size(max = 255, message = "Broker地址长度不能超过255个字符")
    public String getBrokerUrl()
    {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl)
    {
        this.brokerUrl = brokerUrl;
    }

    @Size(max = 255, message = "订阅主题长度不能超过255个字符")
    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    @Min(value = 10, message = "轮询间隔不能小于10秒")
    @Max(value = 86400, message = "轮询间隔不能超过86400秒")
    public Integer getPollingIntervalSec()
    {
        return pollingIntervalSec;
    }

    public void setPollingIntervalSec(Integer pollingIntervalSec)
    {
        this.pollingIntervalSec = pollingIntervalSec;
    }

    public Date getLastSeen()
    {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen)
    {
        this.lastSeen = lastSeen;
    }

    @Size(max = 128, message = "历史Firebase ID长度不能超过128个字符")
    public String getLegacyFirebaseId()
    {
        return legacyFirebaseId;
    }

    public void setLegacyFirebaseId(String legacyFirebaseId)
    {
        this.legacyFirebaseId = legacyFirebaseId;
    }
}
