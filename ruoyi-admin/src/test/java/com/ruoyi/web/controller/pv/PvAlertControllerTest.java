package com.ruoyi.web.controller.pv;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ruoyi.system.domain.pv.PvAlert;
import com.ruoyi.system.service.pv.IPvMonitoringService;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PvWebMvcTest(PvAlertController.class)
class PvAlertControllerTest extends AbstractPvControllerWebMvcTest
{
    @MockitoBean
    private IPvMonitoringService monitoringService;

    @Test
    void listShouldReturnUnauthorizedWithoutToken() throws Exception
    {
        mockMvc.perform(get("/pv/alert/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void listShouldReturnForbiddenWithoutPermission() throws Exception
    {
        mockMvc.perform(get("/pv/alert/list").with(loginUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void listShouldReturnAlertRows() throws Exception
    {
        when(monitoringService.selectAlertList(org.mockito.ArgumentMatchers.any(PvAlert.class)))
                .thenReturn(List.of(buildAlert()));

        mockMvc.perform(get("/pv/alert/list").with(loginUser("pv:alert:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].alertId").value(11))
                .andExpect(jsonPath("$.rows[0].content").value("逆变器离线"));
    }

    @Test
    void resolveShouldPassUsernameAndReturnSuccess() throws Exception
    {
        when(monitoringService.resolveAlert(11L, "pv-admin")).thenReturn(1);

        mockMvc.perform(post("/pv/alert/resolve/11").with(loginUser("pv:alert:resolve")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(monitoringService).resolveAlert(11L, "pv-admin");
    }

    @Test
    void resolveAllShouldPassUsernameAndReturnResolvedCount() throws Exception
    {
        when(monitoringService.resolveAllAlerts("pv-admin")).thenReturn(3);

        mockMvc.perform(post("/pv/alert/resolve-all").with(loginUser("pv:alert:resolveAll")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.resolved").value(3));

        verify(monitoringService).resolveAllAlerts("pv-admin");
    }

    private PvAlert buildAlert()
    {
        PvAlert alert = new PvAlert();
        alert.setAlertId(11L);
        alert.setLevel("critical");
        alert.setContent("逆变器离线");
        alert.setSource("INV-001");
        alert.setStatus("active");
        alert.setOccurTime(new Date());
        return alert;
    }
}
