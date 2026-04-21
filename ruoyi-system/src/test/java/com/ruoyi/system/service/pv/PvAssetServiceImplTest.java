package com.ruoyi.system.service.pv;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.domain.pv.PvGateway;
import com.ruoyi.system.domain.pv.PvInverter;
import com.ruoyi.system.domain.pv.PvStation;
import com.ruoyi.system.event.pv.PvDashboardRefreshPublisher;
import com.ruoyi.system.mapper.pv.PvAssetMapper;
import com.ruoyi.system.service.pv.IPvMqttIngestService;
import com.ruoyi.system.service.pv.impl.PvAssetServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PvAssetServiceImplTest
{
    private static final String DASHBOARD_SUMMARY_CACHE_KEY = CacheConstants.PV_DASHBOARD_SUMMARY_KEY + "all";

    @Mock
    private PvAssetMapper assetMapper;

    @Mock
    private RedisCache redisCache;

    @Mock
    private IPvMqttIngestService mqttIngestService;

    @Mock
    private PvDashboardRefreshPublisher dashboardRefreshPublisher;

    @Mock
    private ConfigurableListableBeanFactory beanFactory;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private PvAssetServiceImpl service;

    @BeforeEach
    void setUpMessageSource() throws Exception
    {
        lenient().when(beanFactory.getBean(MessageSource.class)).thenReturn(messageSource);
        lenient().when(messageSource.getMessage(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(), eq(LocaleContextHolder.getLocale())))
            .thenAnswer(invocation -> {
                String code = invocation.getArgument(0);
                if ("pv.station.delete.has.gateway".equals(code))
                {
                    return "存在接入网关的电站不可删除";
                }
                if ("pv.gateway.delete.has.inverter".equals(code))
                {
                    return "存在逆变器的网关不可删除";
                }
                return code;
            });
        setBeanFactory(beanFactory);
    }

    @AfterEach
    void tearDownMessageSource() throws Exception
    {
        setBeanFactory(null);
    }

    @Test
    void selectStationListShouldDelegateToMapper()
    {
        PvStation station = new PvStation();
        station.setStationId(1L);
        when(assetMapper.selectStationList(station)).thenReturn(List.of(station));

        List<PvStation> result = service.selectStationList(station);

        assertEquals(1, result.size());
        assertSame(station, result.get(0));
        verify(assetMapper).selectStationList(station);
    }

    @Test
    void selectMethodsShouldDelegateToMapper()
    {
        PvStation station = new PvStation();
        station.setStationId(1L);
        PvGateway gateway = new PvGateway();
        gateway.setGatewayId(2L);
        PvInverter inverter = new PvInverter();
        inverter.setInverterId(3L);

        when(assetMapper.selectStationList(station)).thenReturn(List.of(station));
        when(assetMapper.selectStationById(1L)).thenReturn(station);
        when(assetMapper.selectGatewayList(gateway)).thenReturn(List.of(gateway));
        when(assetMapper.selectGatewayById(2L)).thenReturn(gateway);
        when(assetMapper.selectInverterList(inverter)).thenReturn(List.of(inverter));
        when(assetMapper.selectInverterById(3L)).thenReturn(inverter);

        assertEquals(1, service.selectStationList(station).size());
        assertSame(station, service.selectStationById(1L));
        assertEquals(1, service.selectGatewayList(gateway).size());
        assertSame(gateway, service.selectGatewayById(2L));
        assertEquals(1, service.selectInverterList(inverter).size());
        assertSame(inverter, service.selectInverterById(3L));
    }

    @Test
    void listMethodsShouldDeclarePvDataScopeAnnotations() throws Exception
    {
        assertDataScope(PvAssetServiceImpl.class.getMethod("selectStationList", PvStation.class), "pv:station:list");
        assertDataScope(PvAssetServiceImpl.class.getMethod("selectGatewayList", PvGateway.class), "pv:gateway:list");
        assertDataScope(PvAssetServiceImpl.class.getMethod("selectInverterList", PvInverter.class), "pv:inverter:list");
    }

    @Test
    void stationShouldExposeDeptIdForDataScope()
    {
        PvStation station = new PvStation();

        station.setDeptId(42L);

        assertEquals(42L, station.getDeptId());
    }

    @Test
    void insertStationShouldPersistAndEvictCache()
    {
        PvStation station = new PvStation();
        station.setStationName("杭州湾储能站");
        when(assetMapper.insertStation(station)).thenReturn(1);

        int rows = service.insertStation(station);

        assertEquals(1, rows);
        verify(assetMapper).insertStation(station);
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
        verify(dashboardRefreshPublisher).publish("station.insert");
    }

    @Test
    void insertStationShouldStampCreateTimeAndEvictCache()
    {
        PvStation station = new PvStation();
        station.setStationName("绍兴屋顶站");
        when(assetMapper.insertStation(station)).thenReturn(1);

        int rows = service.insertStation(station);

        assertEquals(1, rows);
        assertNotNull(station.getCreateTime());
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    @Test
    void deleteStationByIdsShouldThrowWhenGatewayExists()
    {
        Long[] stationIds = { 1L, 2L };
        when(assetMapper.countGatewaysByStationIds(stationIds)).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.deleteStationByIds(stationIds));

        assertEquals("存在接入网关的电站不可删除", exception.getMessage());
        verify(assetMapper, never()).deleteStationByIds(stationIds);
        verify(redisCache, never()).deleteObject(anyString());
    }

    @Test
    void deleteStationByIdsShouldDeleteAndEvictCacheWhenNoGatewayExists()
    {
        Long[] stationIds = { 9L };
        when(assetMapper.countGatewaysByStationIds(stationIds)).thenReturn(0);
        when(assetMapper.deleteStationByIds(stationIds)).thenReturn(1);

        int rows = service.deleteStationByIds(stationIds);

        assertEquals(1, rows);
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    @Test
    void updateGatewayShouldApplyDefaultPollingIntervalAndStampUpdateTime()
    {
        PvGateway gateway = new PvGateway();
        gateway.setGatewayId(6L);
        when(assetMapper.updateGateway(gateway)).thenReturn(1);

        int rows = service.updateGateway(gateway);

        assertEquals(1, rows);
        assertEquals(60, gateway.getPollingIntervalSec());
        assertNotNull(gateway.getUpdateTime());
        verify(mqttIngestService).refreshSubscriptions();
    }

    @Test
    void deleteGatewayByIdsShouldThrowWhenInverterExists()
    {
        Long[] gatewayIds = { 3L };
        when(assetMapper.countInvertersByGatewayIds(gatewayIds)).thenReturn(2);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.deleteGatewayByIds(gatewayIds));

        assertEquals("存在逆变器的网关不可删除", exception.getMessage());
        verify(assetMapper, never()).deleteGatewayByIds(gatewayIds);
        verify(mqttIngestService, never()).refreshSubscriptions();
    }

    @Test
    void deleteGatewayByIdsShouldDeleteWhenNoInverterExists()
    {
        Long[] gatewayIds = { 3L };
        when(assetMapper.countInvertersByGatewayIds(gatewayIds)).thenReturn(0);
        when(assetMapper.deleteGatewayByIds(gatewayIds)).thenReturn(1);

        int rows = service.deleteGatewayByIds(gatewayIds);

        assertEquals(1, rows);
        verify(mqttIngestService).refreshSubscriptions();
    }

    @Test
    void insertGatewayShouldApplyDefaultStatusAndPollingInterval()
    {
        PvGateway gateway = new PvGateway();
        gateway.setStationId(1L);
        gateway.setGatewayName("宁波边缘网关");
        gateway.setGatewayType("EdgeGateway");
        gateway.setSerialNumber("GW-001");
        gateway.setCommunicationType("Polling");

        when(assetMapper.insertGateway(gateway)).thenReturn(1);

        int rows = service.insertGateway(gateway);

        assertEquals(1, rows);
        assertEquals("offline", gateway.getStatus());
        assertEquals(60, gateway.getPollingIntervalSec());
        assertNotNull(gateway.getCreateTime());
        verify(mqttIngestService).refreshSubscriptions();
    }

    @Test
    void insertInverterShouldApplyDefaultsAndEvictCache()
    {
        PvInverter inverter = new PvInverter();
        inverter.setGatewayId(1L);
        inverter.setModelId(2L);
        inverter.setSerialNumber("INV-001");

        when(assetMapper.insertInverter(inverter)).thenReturn(1);

        int rows = service.insertInverter(inverter);

        assertEquals(1, rows);
        assertEquals("offline", inverter.getStatus());
        assertNotNull(inverter.getCreateTime());
        assertEquals(0, inverter.getCurrentPower().compareTo(new BigDecimal("0.00")));
        assertEquals(0, inverter.getDailyYield().compareTo(new BigDecimal("0.00")));
        verify(redisCache).deleteObject(eq(DASHBOARD_SUMMARY_CACHE_KEY));
    }

    @Test
    void updateInverterShouldStampUpdateTimeAndEvictCache()
    {
        PvInverter inverter = new PvInverter();
        inverter.setInverterId(10L);
        when(assetMapper.updateInverter(inverter)).thenReturn(1);

        int rows = service.updateInverter(inverter);

        assertEquals(1, rows);
        assertNotNull(inverter.getUpdateTime());
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    @Test
    void deleteInverterByIdsShouldEvictCacheWhenRowsUpdated()
    {
        Long[] inverterIds = { 10L, 11L };
        when(assetMapper.deleteInverterByIds(inverterIds)).thenReturn(2);

        int rows = service.deleteInverterByIds(inverterIds);

        assertEquals(2, rows);
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    @Test
    void updateStationShouldStampUpdateTimeAndEvictCache()
    {
        PvStation station = new PvStation();
        station.setStationId(8L);
        station.setStationName("苏州工业园区一号站");

        when(assetMapper.updateStation(station)).thenReturn(1);

        int rows = service.updateStation(station);

        assertEquals(1, rows);
        assertNotNull(station.getUpdateTime());
        verify(redisCache).deleteObject(DASHBOARD_SUMMARY_CACHE_KEY);
    }

    private void setBeanFactory(ConfigurableListableBeanFactory value) throws Exception
    {
        Field field = SpringUtils.class.getDeclaredField("beanFactory");
        field.setAccessible(true);
        field.set(null, value);
    }

    private void assertDataScope(Method method, String permission)
    {
        DataScope dataScope = method.getAnnotation(DataScope.class);
        assertNotNull(dataScope);
        assertEquals("s", dataScope.deptAlias());
        assertEquals(permission, dataScope.permission());
    }
}
