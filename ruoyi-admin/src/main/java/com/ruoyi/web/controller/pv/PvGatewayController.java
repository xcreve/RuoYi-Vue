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
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.service.pv.IPvAssetService;

@RestController
@RequestMapping("/pv/gateway")
public class PvGatewayController extends BaseController
{
    @Autowired
    private IPvAssetService assetService;

    @PreAuthorize("@ss.hasPermi('pv:gateway:list')")
    @GetMapping("/list")
    public TableDataInfo list(PvGateway query)
    {
        startPage();
        List<PvGateway> list = assetService.selectGatewayList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('pv:gateway:view')")
    @GetMapping("/{gatewayId}")
    public AjaxResult getInfo(@PathVariable Long gatewayId)
    {
        return success(assetService.selectGatewayById(gatewayId));
    }

    @PreAuthorize("@ss.hasPermi('pv:gateway:add')")
    @Log(title = "PV-接入网关", businessType = BusinessType.INSERT, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PvGateway gateway)
    {
        gateway.setCreateBy(getUsername());
        return toAjax(assetService.insertGateway(gateway));
    }

    @PreAuthorize("@ss.hasPermi('pv:gateway:edit')")
    @Log(title = "PV-接入网关", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PvGateway gateway)
    {
        gateway.setUpdateBy(getUsername());
        return toAjax(assetService.updateGateway(gateway));
    }

    @PreAuthorize("@ss.hasPermi('pv:gateway:remove')")
    @Log(title = "PV-接入网关", businessType = BusinessType.DELETE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @DeleteMapping("/{gatewayIds}")
    public AjaxResult remove(@PathVariable Long[] gatewayIds)
    {
        return toAjax(assetService.deleteGatewayByIds(gatewayIds));
    }
}
