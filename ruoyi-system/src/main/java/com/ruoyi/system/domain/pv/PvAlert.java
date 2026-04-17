package com.ruoyi.system.domain.pv;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class PvAlert extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long alertId;

    private String level;

    private String content;

    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date occurTime;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date resolvedAt;

    private String resolvedBy;

    private String legacyFirebaseId;

    public Long getAlertId()
    {
        return alertId;
    }

    public void setAlertId(Long alertId)
    {
        this.alertId = alertId;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public Date getOccurTime()
    {
        return occurTime;
    }

    public void setOccurTime(Date occurTime)
    {
        this.occurTime = occurTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getResolvedAt()
    {
        return resolvedAt;
    }

    public void setResolvedAt(Date resolvedAt)
    {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy()
    {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy)
    {
        this.resolvedBy = resolvedBy;
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
