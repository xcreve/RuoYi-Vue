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
import com.ruoyi.system.domain.pv.PvInverterModel;
import com.ruoyi.system.service.pv.IPvCatalogService;

@RestController
@RequestMapping("/pv/model")
public class PvModelController extends BaseController
{
    @Autowired
    private IPvCatalogService catalogService;

    @PreAuthorize("@ss.hasPermi('pv:model:list')")
    @GetMapping("/list")
    public TableDataInfo list(PvInverterModel query)
    {
        startPage();
        List<PvInverterModel> list = catalogService.selectInverterModelList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('pv:model:list')")
    @GetMapping("/{modelId}")
    public AjaxResult getInfo(@PathVariable Long modelId)
    {
        return success(catalogService.selectInverterModelById(modelId));
    }

    @PreAuthorize("@ss.hasPermi('pv:model:add')")
    @Log(title = "PV-品牌型号", businessType = BusinessType.INSERT, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PvInverterModel model)
    {
        model.setCreateBy(getUsername());
        return toAjax(catalogService.insertInverterModel(model));
    }

    @PreAuthorize("@ss.hasPermi('pv:model:edit')")
    @Log(title = "PV-品牌型号", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PvInverterModel model)
    {
        model.setUpdateBy(getUsername());
        return toAjax(catalogService.updateInverterModel(model));
    }

    @PreAuthorize("@ss.hasPermi('pv:model:remove')")
    @Log(title = "PV-品牌型号", businessType = BusinessType.DELETE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @DeleteMapping("/{modelIds}")
    public AjaxResult remove(@PathVariable Long[] modelIds)
    {
        return toAjax(catalogService.deleteInverterModelByIds(modelIds));
    }
}
