package com.ruoyi.web.controller.pv;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ruoyi.system.domain.pv.PvStationTag;
import com.ruoyi.system.service.pv.IPvCatalogService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PvWebMvcTest(PvStationTagController.class)
class PvStationTagControllerTest extends AbstractPvControllerWebMvcTest
{
    @MockitoBean
    private IPvCatalogService catalogService;

    @Test
    void listShouldReturnUnauthorizedWithoutToken() throws Exception
    {
        mockMvc.perform(get("/pv/stationTag/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void listShouldReturnForbiddenWithoutPermission() throws Exception
    {
        mockMvc.perform(get("/pv/stationTag/list").with(loginUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void addShouldReturnBadRequestWhenValidationFails() throws Exception
    {
        PvStationTag tag = new PvStationTag();
        tag.setTagName("");

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/stationTag", tag).with(loginUser("pv:stationTag:add"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("标签名称不能为空"));
    }

    @Test
    void listShouldReturnTableData() throws Exception
    {
        when(catalogService.selectStationTagList(any(PvStationTag.class))).thenReturn(List.of(buildTag()));

        mockMvc.perform(get("/pv/stationTag/list").with(loginUser("pv:stationTag:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].tagId").value(5))
                .andExpect(jsonPath("$.rows[0].tagName").value("工商业"));
    }

    @Test
    void getInfoShouldReturnTagData() throws Exception
    {
        when(catalogService.selectStationTagById(5L)).thenReturn(buildTag());

        mockMvc.perform(get("/pv/stationTag/5").with(loginUser("pv:stationTag:list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tagId").value(5))
                .andExpect(jsonPath("$.data.tagName").value("工商业"));
    }

    @Test
    void addShouldSetCreateByAndReturnSuccess() throws Exception
    {
        when(catalogService.insertStationTag(any(PvStationTag.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.POST, "/pv/stationTag", buildTag()).with(loginUser("pv:stationTag:add")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(catalogService).insertStationTag(argThat(tag -> "pv-admin".equals(tag.getCreateBy())
                && "工商业".equals(tag.getTagName())));
    }

    @Test
    void editShouldSetUpdateByAndReturnSuccess() throws Exception
    {
        when(catalogService.updateStationTag(any(PvStationTag.class))).thenReturn(1);

        mockMvc.perform(jsonRequest(HttpMethod.PUT, "/pv/stationTag", buildTag()).with(loginUser("pv:stationTag:edit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(catalogService).updateStationTag(argThat(tag -> "pv-admin".equals(tag.getUpdateBy())
                && Long.valueOf(5L).equals(tag.getTagId())));
    }

    @Test
    void removeShouldReturnSuccess() throws Exception
    {
        when(catalogService.deleteStationTagByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/pv/stationTag/5,6").with(loginUser("pv:stationTag:remove")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(catalogService).deleteStationTagByIds(argThat(ids -> ids.length == 2 && ids[0] == 5L && ids[1] == 6L));
    }

    private PvStationTag buildTag()
    {
        PvStationTag tag = new PvStationTag();
        tag.setTagId(5L);
        tag.setTagName("工商业");
        tag.setDescription("工商业分布式项目");
        return tag;
    }
}
