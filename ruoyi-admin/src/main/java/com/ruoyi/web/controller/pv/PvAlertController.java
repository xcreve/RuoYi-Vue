package com.ruoyi.web.controller.pv;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.OperatorType;
import com.ruoyi.system.domain.pv.PvAlert;
import com.ruoyi.system.service.pv.IPvMonitoringService;

@RestController
@RequestMapping("/pv/alert")
public class PvAlertController extends BaseController
{
    @Autowired
    private IPvMonitoringService monitoringService;

    @PreAuthorize("@ss.hasPermi('pv:alert:list')")
    @GetMapping("/list")
    public TableDataInfo list(PvAlert query)
    {
        startPage();
        List<PvAlert> list = monitoringService.selectAlertList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('pv:alert:resolve')")
    @Log(title = "PV-系统告警", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PostMapping("/resolve/{id}")
    public AjaxResult resolve(@PathVariable("id") Long alertId)
    {
        return toAjax(monitoringService.resolveAlert(alertId, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('pv:alert:resolveAll')")
    @Log(title = "PV-系统告警", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PostMapping("/resolve-all")
    public AjaxResult resolveAll()
    {
        int resolved = monitoringService.resolveAllAlerts(getUsername());
        return success().put("resolved", resolved);
    }
}
