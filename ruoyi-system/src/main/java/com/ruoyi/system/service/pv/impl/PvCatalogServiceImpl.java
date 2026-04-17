package com.ruoyi.system.service.pv.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.pv.PvInverterModel;
import com.ruoyi.system.domain.pv.PvStationTag;
import com.ruoyi.system.mapper.pv.PvCatalogMapper;
import com.ruoyi.system.service.pv.IPvCatalogService;

@Service
public class PvCatalogServiceImpl implements IPvCatalogService
{
    @Autowired
    private PvCatalogMapper catalogMapper;

    @Override
    public List<PvStationTag> selectStationTagList(PvStationTag query)
    {
        return catalogMapper.selectStationTagList(query);
    }

    @Override
    public PvStationTag selectStationTagById(Long tagId)
    {
        return catalogMapper.selectStationTagById(tagId);
    }

    @Override
    @Transactional
    public int insertStationTag(PvStationTag tag)
    {
        tag.setCreateTime(DateUtils.getNowDate());
        return catalogMapper.insertStationTag(tag);
    }

    @Override
    @Transactional
    public int updateStationTag(PvStationTag tag)
    {
        tag.setUpdateTime(DateUtils.getNowDate());
        return catalogMapper.updateStationTag(tag);
    }

    @Override
    @Transactional
    public int deleteStationTagByIds(Long[] tagIds)
    {
        return catalogMapper.deleteStationTagByIds(tagIds);
    }

    @Override
    public List<PvInverterModel> selectInverterModelList(PvInverterModel query)
    {
        return catalogMapper.selectInverterModelList(query);
    }

    @Override
    public PvInverterModel selectInverterModelById(Long modelId)
    {
        return catalogMapper.selectInverterModelById(modelId);
    }

    @Override
    @Transactional
    public int insertInverterModel(PvInverterModel model)
    {
        model.setCreateTime(DateUtils.getNowDate());
        return catalogMapper.insertInverterModel(model);
    }

    @Override
    @Transactional
    public int updateInverterModel(PvInverterModel model)
    {
        model.setUpdateTime(DateUtils.getNowDate());
        return catalogMapper.updateInverterModel(model);
    }

    @Override
    @Transactional
    public int deleteInverterModelByIds(Long[] modelIds)
    {
        return catalogMapper.deleteInverterModelByIds(modelIds);
    }
}
