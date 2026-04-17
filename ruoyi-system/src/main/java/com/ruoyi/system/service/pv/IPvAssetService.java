package com.ruoyi.system.service.pv;

import java.util.List;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.domain.pv.PvStation;

public interface IPvAssetService
{
    List<PvStation> selectStationList(PvStation query);

    PvStation selectStationById(Long stationId);

    int insertStation(PvStation station);

    int updateStation(PvStation station);

    int deleteStationByIds(Long[] stationIds);

    List<PvGateway> selectGatewayList(PvGateway query);

    PvGateway selectGatewayById(Long gatewayId);

    int insertGateway(PvGateway gateway);

    int updateGateway(PvGateway gateway);

    int deleteGatewayByIds(Long[] gatewayIds);

    List<PvInverter> selectInverterList(PvInverter query);

    PvInverter selectInverterById(Long inverterId);

    int insertInverter(PvInverter inverter);

    int updateInverter(PvInverter inverter);

    int deleteInverterByIds(Long[] inverterIds);
}
