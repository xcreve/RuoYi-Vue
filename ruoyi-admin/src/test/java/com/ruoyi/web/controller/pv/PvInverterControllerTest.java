package com.ruoyi.web.controller.pv;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.service.pv.IPvAssetService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PvWebMvcTest(PvInverterController.class)
class PvInverterControllerTest extends AbstractPvControllerWebMvcTest
{
    @MockitoBean
    private IPvAssetService assetService;

    @Test
    void listShouldReturnUnauthorizedWithoutToken() throws Exception
    {
        mockMvc.perform(get("/pv/inverter/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void listShouldReturnForbiddenWithoutPermission() throws Exception
    {
        mockMvc.perform(get("/pv/inverter/list").with(loginUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void addShouldReturnBadRequestWhenValidationFails() throws Exception
    {
        PvInverter inverter = new PvInverter();
        inverter.setGatewayId(2L);
        inverter.setModelId(3L);
        inverter.setSerialNumber("");

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/inverter", inverter).with(loginUser("pv:inverter:add"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("设备序列号不能为空"));
    }

    @Test
    void listShouldReturnTableData() throws Exception
    {
        when(assetService.selectInverterList(any(PvInverter.class))).thenReturn(List.of(buildInverter()));

        mockMvc.perform(get("/pv/inverter/list").with(loginUser("pv:inverter:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].inverterId").value(3))
                .andExpect(jsonPath("$.rows[0].serialNumber").value("INV-001"));
    }

    @Test
    void getInfoShouldReturnInverterData() throws Exception
    {
        when(assetService.selectInverterById(3L)).thenReturn(buildInverter());

        mockMvc.perform(get("/pv/inverter/3").with(loginUser("pv:inverter:view")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inverterId").value(3))
                .andExpect(jsonPath("$.data.serialNumber").value("INV-001"));
    }

    @Test
    void addShouldSetCreateByAndReturnSuccess() throws Exception
    {
        when(assetService.insertInverter(any(PvInverter.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/inverter", buildInverter()).with(loginUser("pv:inverter:add")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assetService).insertInverter(argThat(inverter -> "pv-admin".equals(inverter.getCreateBy())
                && "INV-001".equals(inverter.getSerialNumber())));
    }

    @Test
    void editShouldSetUpdateByAndReturnSuccess() throws Exception
    {
        when(assetService.updateInverter(any(PvInverter.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.PUT, "/pv/inverter", buildInverter()).with(loginUser("pv:inverter:edit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assetService).updateInverter(argThat(inverter -> "pv-admin".equals(inverter.getUpdateBy())
                && Long.valueOf(3L).equals(inverter.getInverterId())));
    }

    @Test
    void removeShouldReturnSuccess() throws Exception
    {
        when(assetService.deleteInverterByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/pv/inverter/3,4").with(loginUser("pv:inverter:remove")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assetService).deleteInverterByIds(argThat(ids -> ids.length == 2 && ids[0] == 3L && ids[1] == 4L));
    }

    private PvInverter buildInverter()
    {
        PvInverter inverter = new PvInverter();
        inverter.setInverterId(3L);
        inverter.setGatewayId(2L);
        inverter.setGatewayName("一号网关");
        inverter.setModelId(4L);
        inverter.setBrand("Huawei");
        inverter.setModelName("SUN2000");
        inverter.setSerialNumber("INV-001");
        inverter.setInverterNumber("INV-NO-001");
        inverter.setStatus("online");
        inverter.setCurrentPower(new BigDecimal("56.70"));
        inverter.setDailyYield(new BigDecimal("123.45"));
        return inverter;
    }
}
