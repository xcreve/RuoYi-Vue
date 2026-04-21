package com.ruoyi.system.domain.pv;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyi.common.core.domain.BaseEntity;

public class PvInverterModel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long modelId;

    private String brand;

    private String modelName;

    private String mqttProtocol;

    private String registerProfile;

    private String legacyFirebaseId;

    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
    }

    @NotBlank(message = "品牌不能为空")
    @Size(max = 64, message = "品牌长度不能超过64个字符")
    public String getBrand()
    {
        return brand;
    }

    public void setBrand(String brand)
    {
        this.brand = brand;
    }

    @NotBlank(message = "型号不能为空")
    @Size(max = 128, message = "型号长度不能超过128个字符")
    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getMqttProtocol()
    {
        return mqttProtocol;
    }

    public void setMqttProtocol(String mqttProtocol)
    {
        this.mqttProtocol = mqttProtocol;
    }

    public String getRegisterProfile()
    {
        return registerProfile;
    }

    public void setRegisterProfile(String registerProfile)
    {
        this.registerProfile = registerProfile;
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
