package com.ruoyi.web.controller.pv;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.pv.PvHourlyYieldRow;
import com.ruoyi.system.service.pv.IPvMonitoringService;

@RestController
@RequestMapping("/pv/analysis")
public class PvAnalysisController extends BaseController
{
    private static final int EXPORT_ROW_LIMIT = 5000;

    @Autowired
    private IPvMonitoringService monitoringService;

    @PreAuthorize("@ss.hasPermi('pv:analysis:list')")
    @GetMapping("/hourly-yield")
    public AjaxResult hourlyYield(String startDate, String endDate, Long tagId)
    {
        return success(monitoringService.listHourlyYieldRows(startDate, endDate, tagId));
    }

    @PreAuthorize("@ss.hasPermi('pv:analysis:export')")
    @Log(title = "PV-数据分析", businessType = BusinessType.EXPORT, operatorType = OperatorType.MANAGE,
            isSaveResponseData = false)
    @PostMapping("/hourly-yield/export")
    public void export(HttpServletResponse response, String startDate, String endDate, Long tagId)
    {
        List<PvHourlyYieldRow> list = monitoringService.listHourlyYieldRows(startDate, endDate, tagId);
        if (list.size() > EXPORT_ROW_LIMIT)
        {
            throw new ServiceException("导出行数超过5000，请缩小查询区间后重试");
        }
        ExcelUtil<PvHourlyYieldRow> util = new ExcelUtil<PvHourlyYieldRow>(PvHourlyYieldRow.class);
        util.exportExcel(response, list, "发电量统计");
    }
}
