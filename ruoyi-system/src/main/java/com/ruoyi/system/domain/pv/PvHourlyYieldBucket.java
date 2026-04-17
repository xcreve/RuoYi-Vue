package com.ruoyi.system.domain.pv;

import java.math.BigDecimal;

public class PvHourlyYieldBucket
{
    private Long stationId;

    private String stationName;

    private String tagName;

    private Integer hourOfDay;

    private BigDecimal yieldKwh;

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

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    public Integer getHourOfDay()
    {
        return hourOfDay;
    }

    public void setHourOfDay(Integer hourOfDay)
    {
        this.hourOfDay = hourOfDay;
    }

    public BigDecimal getYieldKwh()
    {
        return yieldKwh;
    }

    public void setYieldKwh(BigDecimal yieldKwh)
    {
        this.yieldKwh = yieldKwh;
    }
}
