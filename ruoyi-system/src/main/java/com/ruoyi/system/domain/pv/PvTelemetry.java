package com.ruoyi.system.domain.pv;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class PvTelemetry
{
    private Long telemetryId;

    private Long inverterId;

    private BigDecimal activePower;

    private BigDecimal dailyYield;

    private BigDecimal totalYield;

    private BigDecimal voltage;

    private BigDecimal current;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectTime;

    private String legacyFirebaseId;

    public Long getTelemetryId()
    {
        return telemetryId;
    }

    public void setTelemetryId(Long telemetryId)
    {
        this.telemetryId = telemetryId;
    }

    public Long getInverterId()
    {
        return inverterId;
    }

    public void setInverterId(Long inverterId)
    {
        this.inverterId = inverterId;
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
}
