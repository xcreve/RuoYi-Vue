package com.ruoyi.web.controller.pv;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.OperatorType;
import com.ruoyi.system.domain.pv.PvStation;
import com.ruoyi.system.service.pv.IPvAssetService;

@RestController
@RequestMapping("/pv/station")
public class PvStationController extends BaseController
{
    @Autowired
    private IPvAssetService assetService;

    @PreAuthorize("@ss.hasPermi('pv:station:list')")
    @GetMapping("/list")
    public TableDataInfo list(PvStation query)
    {
        startPage();
        List<PvStation> list = assetService.selectStationList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('pv:station:list')")
    @GetMapping("/{stationId}")
    public AjaxResult getInfo(@PathVariable Long stationId)
    {
        return success(assetService.selectStationById(stationId));
    }

    @PreAuthorize("@ss.hasPermi('pv:station:add')")
    @Log(title = "PV-电站管理", businessType = BusinessType.INSERT, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PvStation station)
    {
        station.setCreateBy(getUsername());
        return toAjax(assetService.insertStation(station));
    }

    @PreAuthorize("@ss.hasPermi('pv:station:edit')")
    @Log(title = "PV-电站管理", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PvStation station)
    {
        station.setUpdateBy(getUsername());
        return toAjax(assetService.updateStation(station));
    }

    @PreAuthorize("@ss.hasPermi('pv:station:remove')")
    @Log(title = "PV-电站管理", businessType = BusinessType.DELETE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @DeleteMapping("/{stationIds}")
    public AjaxResult remove(@PathVariable Long[] stationIds)
    {
        return toAjax(assetService.deleteStationByIds(stationIds));
    }
}
