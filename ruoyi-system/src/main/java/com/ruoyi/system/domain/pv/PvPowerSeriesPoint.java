package com.ruoyi.system.domain.pv;

import java.math.BigDecimal;

public class PvPowerSeriesPoint
{
    private String hour;

    private BigDecimal avgPowerKw;

    public String getHour()
    {
        return hour;
    }

    public void setHour(String hour)
    {
        this.hour = hour;
    }

    public BigDecimal getAvgPowerKw()
    {
        return avgPowerKw;
    }

    public void setAvgPowerKw(BigDecimal avgPowerKw)
    {
        this.avgPowerKw = avgPowerKw;
    }
}
