package com.ruoyi.system.service.pv.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.domain.pv.PvStation;
import com.ruoyi.system.event.pv.PvDashboardRefreshPublisher;
import com.ruoyi.system.mapper.pv.PvAssetMapper;
import com.ruoyi.system.service.pv.IPvAssetService;
import com.ruoyi.system.service.pv.IPvMqttIngestService;

@Service
public class PvAssetServiceImpl implements IPvAssetService
{
    private static final String DASHBOARD_SUMMARY_CACHE_KEY = CacheConstants.PV_DASHBOARD_SUMMARY_KEY + "all";

    @Autowired
    private PvAssetMapper assetMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired(required = false)
    private IPvMqttIngestService mqttIngestService;

    @Autowired(required = false)
    private PvDashboardRefreshPublisher dashboardRefreshPublisher;

    @Override
    @DataScope(deptAlias = "s", permission = "pv:station:list")
    public List<PvStation> selectStationList(PvStation query)
    {
        return assetMapper.selectStationList(query);
    }

    @Override
    public PvStation selectStationById(Long stationId)
    {
        return assetMapper.selectStationById(stationId);
    }

    @Override
    @Transactional
    public int insertStation(PvStation station)
    {
        station.setCreateTime(DateUtils.getNowDate());
        bindCurrentUserDept(station);
        int rows = assetMapper.insertStation(station);
        if (rows > 0)
        {
            evictDashboardSummaryCache();
            publishDashboardRefresh("station.insert");
        }
        return rows;
    }

    @Override
    @Transactional
    public int updateStation(PvStation station)
    {
        station.setUpdateTime(DateUtils.getNowDate());
        int rows = assetMapper.updateStation(station);
        if (rows > 0)
        {
            evictDashboardSummaryCache();
            publishDashboardRefresh("station.update");
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteStationByIds(Long[] stationIds)
    {
        if (assetMapper.countGatewaysByStationIds(stationIds) > 0)
        {
            throw new ServiceException(MessageUtils.message("pv.station.delete.has.gateway"));
        }
        int rows = assetMapper.deleteStationByIds(stationIds);
        if (rows > 0)
        {
            evictDashboardSummaryCache();
            publishDashboardRefresh("station.delete");
        }
        return rows;
    }

    @Override
    @DataScope(deptAlias = "s", permission = "pv:gateway:list")
    public List<PvGateway> selectGatewayList(PvGateway query)
    {
        return assetMapper.selectGatewayList(query);
    }

    @Override
    public PvGateway selectGatewayById(Long gatewayId)
    {
        return assetMapper.selectGatewayById(gatewayId);
    }

    @Override
    @Transactional
    public int insertGateway(PvGateway gateway)
    {
        gateway.setStatus(gateway.getStatus() == null ? "offline" : gateway.getStatus());
        gateway.setPollingIntervalSec(gateway.getPollingIntervalSec() == null ? 60 : gateway.getPollingIntervalSec());
        gateway.setCreateTime(DateUtils.getNowDate());
        int rows = assetMapper.insertGateway(gateway);
        if (rows > 0)
        {
            refreshMqttSubscriptions();
            publishDashboardRefresh("gateway.insert");
        }
        return rows;
    }

    @Override
    @Transactional
    public int updateGateway(PvGateway gateway)
    {
        gateway.setPollingIntervalSec(gateway.getPollingIntervalSec() == null ? 60 : gateway.getPollingIntervalSec());
        gateway.setUpdateTime(DateUtils.getNowDate());
        int rows = assetMapper.updateGateway(gateway);
        if (rows > 0)
        {
            refreshMqttSubscriptions();
            publishDashboardRefresh("gateway.update");
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteGatewayByIds(Long[] gatewayIds)
    {
        if (assetMapper.countInvertersByGatewayIds(gatewayIds) > 0)
        {
            throw new ServiceException(MessageUtils.message("pv.gateway.delete.has.inverter"));
        }
        int rows = assetMapper.deleteGatewayByIds(gatewayIds);
        if (rows > 0)
        {
            refreshMqttSubscriptions();
            publishDashboardRefresh("gateway.delete");
        }
        return rows;
    }

    @Override
    @DataScope(deptAlias = "s", permission = "pv:inverter:list")
    public List<PvInverter> selectInverterList(PvInverter query)
    {
        return assetMapper.selectInverterList(query);
    }

    @Override
    public PvInverter selectInverterById(Long inverterId)
    {
        return assetMapper.selectInverterById(inverterId);
    }

    @Override
    @Transactional
    public int insertInverter(PvInverter inverter)
    {
        inverter.setStatus(inverter.getStatus() == null ? "offline" : inverter.getStatus());
        inverter.setCurrentPower(defaultDecimal(inverter.getCurrentPower()));
        inverter.setDailyYield(defaultDecimal(inverter.getDailyYield()));
        inverter.setCreateTime(DateUtils.getNowDate());
        int rows = assetMapper.insertInverter(inverter);
        if (rows > 0)
        {
            evictDashboardSummaryCache();
            publishDashboardRefresh("inverter.insert");
        }
        return rows;
    }

    @Override
    @Transactional
    public int updateInverter(PvInverter inverter)
    {
        inverter.setUpdateTime(DateUtils.getNowDate());
        int rows = assetMapper.updateInverter(inverter);
        if (rows > 0)
        {
            evictDashboardSummaryCache();
            publishDashboardRefresh("inverter.update");
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteInverterByIds(Long[] inverterIds)
    {
        int rows = assetMapper.deleteInverterByIds(inverterIds);
        if (rows > 0)
        {
            evictDashboardSummaryCache();
            publishDashboardRefresh("inverter.delete");
        }
        return rows;
    }

    private BigDecimal defaultDecimal(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
    }

    private void bindCurrentUserDept(PvStation station)
    {
        // 新建电站时绑定当前用户所在部门，后台任务无登录态时保持调用方传入值。
        try
        {
            Long deptId = SecurityUtils.getLoginUser().getUser().getDeptId();
            if (station.getDeptId() == null)
            {
                station.setDeptId(deptId);
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private void evictDashboardSummaryCache()
    {
        redisCache.deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    private void refreshMqttSubscriptions()
    {
        if (mqttIngestService != null)
        {
            mqttIngestService.refreshSubscriptions();
        }
    }

    private void publishDashboardRefresh(String source)
    {
        if (dashboardRefreshPublisher != null)
        {
            dashboardRefreshPublisher.publish(source);
        }
    }
}
