package com.ruoyi.system.mapper.pv;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.domain.pv.PvHourlyYieldBucket;
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.domain.pv.PvStation;
import com.ruoyi.system.domain.pv.PvTelemetry;

public interface PvAssetMapper
{
    List<PvStation> selectStationList(PvStation query);

    PvStation selectStationById(Long stationId);

    int insertStation(PvStation station);

    int updateStation(PvStation station);

    int deleteStationByIds(Long[] stationIds);

    int countGatewaysByStationIds(Long[] stationIds);

    List<PvGateway> selectGatewayList(PvGateway query);

    PvGateway selectGatewayById(Long gatewayId);

    int insertGateway(PvGateway gateway);

    int updateGateway(PvGateway gateway);

    int deleteGatewayByIds(Long[] gatewayIds);

    int countInvertersByGatewayIds(Long[] gatewayIds);

    int updateGatewayStatus(@Param("gatewayId") Long gatewayId, @Param("status") String status,
            @Param("lastSeen") Date lastSeen);

    List<PvInverter> selectInverterList(PvInverter query);

    PvInverter selectInverterById(Long inverterId);

    int insertInverter(PvInverter inverter);

    int updateInverter(PvInverter inverter);

    int deleteInverterByIds(Long[] inverterIds);

    List<PvTelemetry> selectTelemetryList();

    List<PvTelemetry> selectTelemetryByRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int deleteTelemetryBatchBefore(@Param("cutoffTime") Date cutoffTime, @Param("limit") int limit);

    List<PvHourlyYieldBucket> selectHourlyYieldBuckets(@Param("startTime") Date startTime, @Param("endTime") Date endTime,
            @Param("tagId") Long tagId);

    int insertTelemetryBatch(@Param("list") List<PvTelemetry> list);

    int updateInverterStatus(@Param("inverterId") Long inverterId, @Param("status") String status,
            @Param("lastSeen") Date lastSeen, @Param("currentPower") java.math.BigDecimal currentPower,
            @Param("dailyYield") java.math.BigDecimal dailyYield);

    int updateInvertersOfflineByGatewayId(@Param("gatewayId") Long gatewayId);
}
