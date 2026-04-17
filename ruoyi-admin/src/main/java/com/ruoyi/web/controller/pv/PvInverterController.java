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
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.service.pv.IPvAssetService;

@RestController
@RequestMapping("/pv/inverter")
public class PvInverterController extends BaseController
{
    @Autowired
    private IPvAssetService assetService;

    @PreAuthorize("@ss.hasPermi('pv:inverter:list')")
    @GetMapping("/list")
    public TableDataInfo list(PvInverter query)
    {
        startPage();
        List<PvInverter> list = assetService.selectInverterList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('pv:inverter:view')")
    @GetMapping("/{inverterId}")
    public AjaxResult getInfo(@PathVariable Long inverterId)
    {
        return success(assetService.selectInverterById(inverterId));
    }

    @PreAuthorize("@ss.hasPermi('pv:inverter:add')")
    @Log(title = "PV-接入设备", businessType = BusinessType.INSERT, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PvInverter inverter)
    {
        inverter.setCreateBy(getUsername());
        return toAjax(assetService.insertInverter(inverter));
    }

    @PreAuthorize("@ss.hasPermi('pv:inverter:edit')")
    @Log(title = "PV-接入设备", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PvInverter inverter)
    {
        inverter.setUpdateBy(getUsername());
        return toAjax(assetService.updateInverter(inverter));
    }

    @PreAuthorize("@ss.hasPermi('pv:inverter:remove')")
    @Log(title = "PV-接入设备", businessType = BusinessType.DELETE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @DeleteMapping("/{inverterIds}")
    public AjaxResult remove(@PathVariable Long[] inverterIds)
    {
        return toAjax(assetService.deleteInverterByIds(inverterIds));
    }
}
