package com.ruoyi.system.domain.pv;

import com.ruoyi.common.core.domain.BaseEntity;

public class PvAlertChannel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long channelId;

    private String channelType;

    private String target;

    private Integer enabled;

    public Long getChannelId()
    {
        return channelId;
    }

    public void setChannelId(Long channelId)
    {
        this.channelId = channelId;
    }

    public String getChannelType()
    {
        return channelType;
    }

    public void setChannelType(String channelType)
    {
        this.channelType = channelType;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public Integer getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Integer enabled)
    {
        this.enabled = enabled;
    }
}
