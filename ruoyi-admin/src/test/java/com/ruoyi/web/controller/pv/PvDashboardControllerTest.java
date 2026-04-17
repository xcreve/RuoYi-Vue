package com.ruoyi.web.controller.pv;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ruoyi.system.domain.pv.PvDashboardSummary;
import com.ruoyi.system.domain.pv.PvPowerSeriesPoint;
import com.ruoyi.system.service.pv.IPvMonitoringService;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PvWebMvcTest(PvDashboardController.class)
class PvDashboardControllerTest extends AbstractPvControllerWebMvcTest
{
    @MockitoBean
    private IPvMonitoringService monitoringService;

    @Test
    void summaryShouldReturnUnauthorizedWithoutToken() throws Exception
    {
        mockMvc.perform(get("/pv/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void summaryShouldReturnForbiddenWithoutPermission() throws Exception
    {
        mockMvc.perform(get("/pv/dashboard/summary").with(loginUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void summaryShouldReturnDashboardData() throws Exception
    {
        when(monitoringService.getDashboardSummary()).thenReturn(buildSummary());

        mockMvc.perform(get("/pv/dashboard/summary").with(loginUser("pv:dashboard:view")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCapacityMw").value(12.6))
                .andExpect(jsonPath("$.data.onlineInverters").value(8))
                .andExpect(jsonPath("$.data.activeAlerts").value(2));
    }

    @Test
    void powerSeriesShouldReturnPointList() throws Exception
    {
        when(monitoringService.listPowerSeries()).thenReturn(List.of(buildPoint()));

        mockMvc.perform(get("/pv/dashboard/power-series").with(loginUser("pv:dashboard:view")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].hour").value("2026-04-14 10:00:00"))
                .andExpect(jsonPath("$.data[0].avgPowerKw").value(56.7));
    }

    @Test
    void simulateShouldPassUsernameAndReturnResult() throws Exception
    {
        when(monitoringService.simulateTelemetry("pv-admin")).thenReturn(18);

        mockMvc.perform(post("/pv/dashboard/simulate").with(loginUser("pv:dashboard:simulate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.simulated").value(18));

        verify(monitoringService).simulateTelemetry("pv-admin");
    }

    private PvDashboardSummary buildSummary()
    {
        PvDashboardSummary summary = new PvDashboardSummary();
        summary.setTotalCapacityMw(new BigDecimal("12.60"));
        summary.setOnlineInverters(8L);
        summary.setTotalInverters(10L);
        summary.setCurrentPowerKw(new BigDecimal("356.80"));
        summary.setDailyYieldKwh(new BigDecimal("1248.60"));
        summary.setActiveAlerts(2L);
        return summary;
    }

    private PvPowerSeriesPoint buildPoint()
    {
        PvPowerSeriesPoint point = new PvPowerSeriesPoint();
        point.setHour("2026-04-14 10:00:00");
        point.setAvgPowerKw(new BigDecimal("56.70"));
        return point;
    }
}
