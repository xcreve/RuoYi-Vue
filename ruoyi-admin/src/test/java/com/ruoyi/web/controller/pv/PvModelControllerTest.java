package com.ruoyi.web.controller.pv;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ruoyi.system.domain.pv.PvInverterModel;
import com.ruoyi.system.service.pv.IPvCatalogService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PvWebMvcTest(PvModelController.class)
class PvModelControllerTest extends AbstractPvControllerWebMvcTest
{
    @MockitoBean
    private IPvCatalogService catalogService;

    @Test
    void listShouldReturnUnauthorizedWithoutToken() throws Exception
    {
        mockMvc.perform(get("/pv/model/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void listShouldReturnForbiddenWithoutPermission() throws Exception
    {
        mockMvc.perform(get("/pv/model/list").with(loginUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void addShouldReturnBadRequestWhenValidationFails() throws Exception
    {
        PvInverterModel model = new PvInverterModel();
        model.setBrand("");
        model.setModelName("SUN2000");

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/model", model).with(loginUser("pv:model:add"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("品牌不能为空"));
    }

    @Test
    void listShouldReturnTableData() throws Exception
    {
        when(catalogService.selectInverterModelList(any(PvInverterModel.class))).thenReturn(List.of(buildModel()));

        mockMvc.perform(get("/pv/model/list").with(loginUser("pv:model:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].modelId").value(6))
                .andExpect(jsonPath("$.rows[0].brand").value("Huawei"));
    }

    @Test
    void getInfoShouldReturnModelData() throws Exception
    {
        when(catalogService.selectInverterModelById(6L)).thenReturn(buildModel());

        mockMvc.perform(get("/pv/model/6").with(loginUser("pv:model:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelId").value(6))
                .andExpect(jsonPath("$.data.modelName").value("SUN2000"));
    }

    @Test
    void addShouldSetCreateByAndReturnSuccess() throws Exception
    {
        when(catalogService.insertInverterModel(any(PvInverterModel.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/model", buildModel()).with(loginUser("pv:model:add")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(catalogService).insertInverterModel(argThat(model -> "pv-admin".equals(model.getCreateBy())
                && "Huawei".equals(model.getBrand())));
    }

    @Test
    void editShouldSetUpdateByAndReturnSuccess() throws Exception
    {
        when(catalogService.updateInverterModel(any(PvInverterModel.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.PUT, "/pv/model", buildModel()).with(loginUser("pv:model:edit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(catalogService).updateInverterModel(argThat(model -> "pv-admin".equals(model.getUpdateBy())
                && Long.valueOf(6L).equals(model.getModelId())));
    }

    @Test
    void removeShouldReturnSuccess() throws Exception
    {
        when(catalogService.deleteInverterModelByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/pv/model/6,7").with(loginUser("pv:model:remove")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(catalogService).deleteInverterModelByIds(argThat(ids -> ids.length == 2 && ids[0] == 6L && ids[1] == 7L));
    }

    private PvInverterModel buildModel()
    {
        PvInverterModel model = new PvInverterModel();
        model.setModelId(6L);
        model.setBrand("Huawei");
        model.setModelName("SUN2000");
        model.setMqttProtocol("json");
        return model;
    }
}
