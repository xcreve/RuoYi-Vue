package com.ruoyi.system.mapper.pv;

import java.util.List;
import com.ruoyi.system.domain.pv.PvInverterModel;
import com.ruoyi.system.domain.pv.PvStationTag;

public interface PvCatalogMapper
{
    List<PvStationTag> selectStationTagList(PvStationTag query);

    PvStationTag selectStationTagById(Long tagId);

    int insertStationTag(PvStationTag tag);

    int updateStationTag(PvStationTag tag);

    int deleteStationTagByIds(Long[] tagIds);

    List<PvInverterModel> selectInverterModelList(PvInverterModel query);

    PvInverterModel selectInverterModelById(Long modelId);

    int insertInverterModel(PvInverterModel model);

    int updateInverterModel(PvInverterModel model);

    int deleteInverterModelByIds(Long[] modelIds);
}
