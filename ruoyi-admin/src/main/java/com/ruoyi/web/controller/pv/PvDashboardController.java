package com.ruoyi.web.controller.pv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.OperatorType;
import com.ruoyi.system.service.pv.IPvMonitoringService;

@RestController
@RequestMapping("/pv/dashboard")
public class PvDashboardController extends BaseController
{
    @Autowired
    private IPvMonitoringService monitoringService;

    @PreAuthorize("@ss.hasPermi('pv:dashboard:view')")
    @GetMapping("/summary")
    public AjaxResult summary()
    {
        return success(monitoringService.getDashboardSummary());
    }

    @PreAuthorize("@ss.hasPermi('pv:dashboard:view')")
    @GetMapping("/power-series")
    public AjaxResult powerSeries()
    {
        return success(monitoringService.listPowerSeries());
    }

    @PreAuthorize("@ss.hasPermi('pv:dashboard:simulate')")
    @Log(title = "PV-监控大屏", businessType = BusinessType.OTHER, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PostMapping("/simulate")
    public AjaxResult simulate()
    {
        int simulated = monitoringService.simulateTelemetry(getUsername());
        return success().put("simulated", simulated);
    }
}
