package com.ruoyi.system.domain.pv;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.ruoyi.common.core.domain.BaseEntity;

public class PvInverter extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long inverterId;

    private Long gatewayId;

    private String gatewayName;

    private Long modelId;

    private String brand;

    private String modelName;

    private String inverterNumber;

    private String serialNumber;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeen;

    private BigDecimal currentPower;

    private BigDecimal dailyYield;

    private String legacyFirebaseId;

    public Long getInverterId()
    {
        return inverterId;
    }

    public void setInverterId(Long inverterId)
    {
        this.inverterId = inverterId;
    }

    @NotNull(message = "所属网关不能为空")
    public Long getGatewayId()
    {
        return gatewayId;
    }

    public void setGatewayId(Long gatewayId)
    {
        this.gatewayId = gatewayId;
    }

    public String getGatewayName()
    {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName)
    {
        this.gatewayName = gatewayName;
    }

    @NotNull(message = "品牌型号不能为空")
    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
    }

    public String getBrand()
    {
        return brand;
    }

    public void setBrand(String brand)
    {
        this.brand = brand;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    @Size(max = 64, message = "设备编号长度不能超过64个字符")
    public String getInverterNumber()
    {
        return inverterNumber;
    }

    public void setInverterNumber(String inverterNumber)
    {
        this.inverterNumber = inverterNumber;
    }

    @NotBlank(message = "设备序列号不能为空")
    @Size(max = 128, message = "设备序列号长度不能超过128个字符")
    public String getSerialNumber()
    {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber)
    {
        this.serialNumber = serialNumber;
    }

    @Size(max = 16, message = "设备状态长度不能超过16个字符")
    @Pattern(regexp = "online|offline|fault", message = "设备状态只能为online、offline或fault")
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

    @DecimalMin(value = "0.00", message = "当前功率不能为负数")
    @Digits(integer = 10, fraction = 2, message = "当前功率格式不正确")
    public BigDecimal getCurrentPower()
    {
        return currentPower;
    }

    public void setCurrentPower(BigDecimal currentPower)
    {
        this.currentPower = currentPower;
    }

    @DecimalMin(value = "0.00", message = "当日发电量不能为负数")
    @Digits(integer = 10, fraction = 2, message = "当日发电量格式不正确")
    public BigDecimal getDailyYield()
    {
        return dailyYield;
    }

    public void setDailyYield(BigDecimal dailyYield)
    {
        this.dailyYield = dailyYield;
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
