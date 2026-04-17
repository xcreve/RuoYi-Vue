package com.ruoyi.web.controller.pv;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.service.pv.IPvAssetService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PvWebMvcTest(PvGatewayController.class)
class PvGatewayControllerTest extends AbstractPvControllerWebMvcTest
{
    @MockitoBean
    private IPvAssetService assetService;

    @Test
    void listShouldReturnUnauthorizedWithoutToken() throws Exception
    {
        mockMvc.perform(get("/pv/gateway/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void listShouldReturnForbiddenWithoutPermission() throws Exception
    {
        mockMvc.perform(get("/pv/gateway/list").with(loginUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void addShouldReturnBadRequestWhenValidationFails() throws Exception
    {
        PvGateway gateway = new PvGateway();
        gateway.setStationId(1L);
        gateway.setGatewayName("");
        gateway.setGatewayType("EdgeGateway");
        gateway.setSerialNumber("GW-001");
        gateway.setCommunicationType("Polling");

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/gateway", gateway).with(loginUser("pv:gateway:add"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("网关名称不能为空"));
    }

    @Test
    void listShouldReturnTableData() throws Exception
    {
        when(assetService.selectGatewayList(any(PvGateway.class))).thenReturn(List.of(buildGateway()));

        mockMvc.perform(get("/pv/gateway/list").with(loginUser("pv:gateway:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].gatewayId").value(2))
                .andExpect(jsonPath("$.rows[0].gatewayName").value("一号网关"));
    }

    @Test
    void getInfoShouldReturnGatewayData() throws Exception
    {
        when(assetService.selectGatewayById(2L)).thenReturn(buildGateway());

        mockMvc.perform(get("/pv/gateway/2").with(loginUser("pv:gateway:view")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gatewayId").value(2))
                .andExpect(jsonPath("$.data.gatewayName").value("一号网关"));
    }

    @Test
    void addShouldSetCreateByAndReturnSuccess() throws Exception
    {
        when(assetService.insertGateway(any(PvGateway.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/gateway", buildGateway()).with(loginUser("pv:gateway:add")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assetService).insertGateway(argThat(gateway -> "pv-admin".equals(gateway.getCreateBy())
                && "一号网关".equals(gateway.getGatewayName())));
    }

    @Test
    void editShouldSetUpdateByAndReturnSuccess() throws Exception
    {
        when(assetService.updateGateway(any(PvGateway.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.PUT, "/pv/gateway", buildGateway()).with(loginUser("pv:gateway:edit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assetService).updateGateway(argThat(gateway -> "pv-admin".equals(gateway.getUpdateBy())
                && Long.valueOf(2L).equals(gateway.getGatewayId())));
    }

    @Test
    void removeShouldReturnSuccess() throws Exception
    {
        when(assetService.deleteGatewayByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/pv/gateway/2,3").with(loginUser("pv:gateway:remove")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assetService).deleteGatewayByIds(argThat(ids -> ids.length == 2 && ids[0] == 2L && ids[1] == 3L));
    }

    private PvGateway buildGateway()
    {
        PvGateway gateway = new PvGateway();
        gateway.setGatewayId(2L);
        gateway.setStationId(1L);
        gateway.setStationName("示例电站");
        gateway.setGatewayName("一号网关");
        gateway.setGatewayType("EdgeGateway");
        gateway.setSerialNumber("GW-001");
        gateway.setCommunicationType("Polling");
        gateway.setProtocol("ModbusTCP");
        gateway.setBrokerUrl("tcp://127.0.0.1:502");
        gateway.setTopic("power=0:2:0.1");
        gateway.setPollingIntervalSec(60);
        return gateway;
    }
}
