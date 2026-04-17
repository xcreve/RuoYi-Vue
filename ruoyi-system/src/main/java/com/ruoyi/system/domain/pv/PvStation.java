package com.ruoyi.system.domain.pv;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyi.common.core.domain.BaseEntity;

public class PvStation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long stationId;

    private String stationName;

    private String location;

    private BigDecimal capacityMw;

    private Long tagId;

    private String tagName;

    private String legacyFirebaseId;

    public Long getStationId()
    {
        return stationId;
    }

    public void setStationId(Long stationId)
    {
        this.stationId = stationId;
    }

    @NotBlank(message = "电站名称不能为空")
    @Size(max = 128, message = "电站名称长度不能超过128个字符")
    public String getStationName()
    {
        return stationName;
    }

    public void setStationName(String stationName)
    {
        this.stationName = stationName;
    }

    @Size(max = 255, message = "地理位置长度不能超过255个字符")
    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    @DecimalMin(value = "0.00", message = "装机容量不能为负数")
    @Digits(integer = 8, fraction = 2, message = "装机容量格式不正确")
    public BigDecimal getCapacityMw()
    {
        return capacityMw;
    }

    public void setCapacityMw(BigDecimal capacityMw)
    {
        this.capacityMw = capacityMw;
    }

    public Long getTagId()
    {
        return tagId;
    }

    public void setTagId(Long tagId)
    {
        this.tagId = tagId;
    }

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
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
