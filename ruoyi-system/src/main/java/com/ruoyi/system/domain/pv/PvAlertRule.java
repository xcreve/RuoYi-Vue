package com.ruoyi.system.domain.pv;

import com.ruoyi.common.core.domain.BaseEntity;

public class PvAlertRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long ruleId;

    private String level;

    private Long channelId;

    private Integer throttleSec;

    private String channelType;

    private String target;

    private Integer channelEnabled;

    public Long getRuleId()
    {
        return ruleId;
    }

    public void setRuleId(Long ruleId)
    {
        this.ruleId = ruleId;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public Long getChannelId()
    {
        return channelId;
    }

    public void setChannelId(Long channelId)
    {
        this.channelId = channelId;
    }

    public Integer getThrottleSec()
    {
        return throttleSec;
    }

    public void setThrottleSec(Integer throttleSec)
    {
        this.throttleSec = throttleSec;
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

    public Integer getChannelEnabled()
    {
        return channelEnabled;
    }

    public void setChannelEnabled(Integer channelEnabled)
    {
        this.channelEnabled = channelEnabled;
    }

    public boolean isChannelEnabled()
    {
        return channelEnabled != null && channelEnabled == 1;
    }
}
