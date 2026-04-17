package com.ruoyi.web.controller.pv;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ruoyi.system.domain.pv.PvHourlyYieldRow;
import com.ruoyi.system.service.pv.IPvMonitoringService;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PvWebMvcTest(PvAnalysisController.class)
class PvAnalysisControllerTest extends AbstractPvControllerWebMvcTest
{
    @MockitoBean
    private IPvMonitoringService monitoringService;

    @Test
    void hourlyYieldShouldReturnUnauthorizedWithoutToken() throws Exception
    {
        mockMvc.perform(get("/pv/analysis/hourly-yield"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void hourlyYieldShouldReturnForbiddenWithoutPermission() throws Exception
    {
        mockMvc.perform(get("/pv/analysis/hourly-yield").with(loginUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void hourlyYieldShouldReturnRows() throws Exception
    {
        when(monitoringService.listHourlyYieldRows("2026-04-14", "2026-04-14", 9L))
                .thenReturn(List.of(buildYieldRow()));

        mockMvc.perform(get("/pv/analysis/hourly-yield")
                .param("startDate", "2026-04-14")
                .param("endDate", "2026-04-14")
                .param("tagId", "9")
                .with(loginUser("pv:analysis:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].stationId").value(1))
                .andExpect(jsonPath("$.data[0].stationName").value("示例电站"))
                .andExpect(jsonPath("$.data[0].total").value(18.6));
    }

    @Test
    void exportShouldReturnExcelAttachment() throws Exception
    {
        when(monitoringService.listHourlyYieldRows("2026-04-14", "2026-04-14", 9L))
                .thenReturn(List.of(buildYieldRow()));

        mockMvc.perform(post("/pv/analysis/hourly-yield/export")
                .param("startDate", "2026-04-14")
                .param("endDate", "2026-04-14")
                .param("tagId", "9")
                .with(loginUser("pv:analysis:export")))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-disposition"))
                .andExpect(header().exists("download-filename"));
    }

    private PvHourlyYieldRow buildYieldRow()
    {
        PvHourlyYieldRow row = new PvHourlyYieldRow();
        row.setStationId(1L);
        row.setStationName("示例电站");
        row.setTagName("工商业");
        row.addHourValue(9, new BigDecimal("8.50"));
        row.addHourValue(10, new BigDecimal("10.10"));
        return row;
    }
}
