package com.ruoyi.web.controller.pv;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ruoyi.system.domain.pv.PvStation;
import com.ruoyi.system.service.pv.IPvAssetService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PvWebMvcTest(PvStationController.class)
class PvStationControllerTest extends AbstractPvControllerWebMvcTest
{
    @MockitoBean
    private IPvAssetService assetService;

    @Test
    void listShouldReturnUnauthorizedWithoutToken() throws Exception
    {
        mockMvc.perform(get("/pv/station/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void listShouldReturnForbiddenWithoutPermission() throws Exception
    {
        mockMvc.perform(get("/pv/station/list").with(loginUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void addShouldReturnBadRequestWhenValidationFails() throws Exception
    {
        PvStation station = new PvStation();
        station.setStationName("");
        station.setCapacityMw(new BigDecimal("1.50"));

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/station", station).with(loginUser("pv:station:add"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("电站名称不能为空"));
    }

    @Test
    void listShouldReturnTableData() throws Exception
    {
        when(assetService.selectStationList(any(PvStation.class))).thenReturn(List.of(buildStation()));

        mockMvc.perform(get("/pv/station/list").with(loginUser("pv:station:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("查询成功"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].stationId").value(1))
                .andExpect(jsonPath("$.rows[0].stationName").value("示例电站"));
    }

    @Test
    void getInfoShouldReturnStationData() throws Exception
    {
        when(assetService.selectStationById(1L)).thenReturn(buildStation());

        mockMvc.perform(get("/pv/station/1").with(loginUser("pv:station:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stationId").value(1))
                .andExpect(jsonPath("$.data.stationName").value("示例电站"));
    }

    @Test
    void addShouldSetCreateByAndReturnSuccess() throws Exception
    {
        when(assetService.insertStation(any(PvStation.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/station", buildStation()).with(loginUser("pv:station:add")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        verify(assetService).insertStation(argThat(station -> "pv-admin".equals(station.getCreateBy())
                && "示例电站".equals(station.getStationName())));
    }

    @Test
    void editShouldSetUpdateByAndReturnSuccess() throws Exception
    {
        when(assetService.updateStation(any(PvStation.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.PUT, "/pv/station", buildStation()).with(loginUser("pv:station:edit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assetService).updateStation(argThat(station -> "pv-admin".equals(station.getUpdateBy())
                && Long.valueOf(1L).equals(station.getStationId())));
    }

    @Test
    void removeShouldReturnSuccess() throws Exception
    {
        when(assetService.deleteStationByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/pv/station/1,2").with(loginUser("pv:station:remove")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(assetService).deleteStationByIds(argThat(ids -> ids.length == 2 && ids[0] == 1L && ids[1] == 2L));
    }

    private PvStation buildStation()
    {
        PvStation station = new PvStation();
        station.setStationId(1L);
        station.setStationName("示例电站");
        station.setLocation("杭州");
        station.setCapacityMw(new BigDecimal("3.60"));
        station.setTagId(9L);
        station.setTagName("工商业");
        return station;
    }
}
