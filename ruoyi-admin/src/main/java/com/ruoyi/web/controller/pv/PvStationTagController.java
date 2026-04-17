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
import com.ruoyi.system.domain.pv.PvStationTag;
import com.ruoyi.system.service.pv.IPvCatalogService;

@RestController
@RequestMapping("/pv/stationTag")
public class PvStationTagController extends BaseController
{
    @Autowired
    private IPvCatalogService catalogService;

    @PreAuthorize("@ss.hasPermi('pv:stationTag:list')")
    @GetMapping("/list")
    public TableDataInfo list(PvStationTag query)
    {
        startPage();
        List<PvStationTag> list = catalogService.selectStationTagList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('pv:stationTag:list')")
    @GetMapping("/{tagId}")
    public AjaxResult getInfo(@PathVariable Long tagId)
    {
        return success(catalogService.selectStationTagById(tagId));
    }

    @PreAuthorize("@ss.hasPermi('pv:stationTag:add')")
    @Log(title = "PV-电站标签", businessType = BusinessType.INSERT, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PvStationTag tag)
    {
        tag.setCreateBy(getUsername());
        return toAjax(catalogService.insertStationTag(tag));
    }

    @PreAuthorize("@ss.hasPermi('pv:stationTag:edit')")
    @Log(title = "PV-电站标签", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PvStationTag tag)
    {
        tag.setUpdateBy(getUsername());
        return toAjax(catalogService.updateStationTag(tag));
    }

    @PreAuthorize("@ss.hasPermi('pv:stationTag:remove')")
    @Log(title = "PV-电站标签", businessType = BusinessType.DELETE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @DeleteMapping("/{tagIds}")
    public AjaxResult remove(@PathVariable Long[] tagIds)
    {
        return toAjax(catalogService.deleteStationTagByIds(tagIds));
    }
}
