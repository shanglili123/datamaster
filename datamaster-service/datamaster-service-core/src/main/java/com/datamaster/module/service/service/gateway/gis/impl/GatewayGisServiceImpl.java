package com.datamaster.module.service.service.gateway.gis.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.httpClient.HeaderEntity;
import com.datamaster.common.httpClient.HttpUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.service.api.service.gateway.gis.IGatewayGisOutService;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisReqVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisRespVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisSaveReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.gis.GatewayGisDO;
import com.datamaster.module.service.dal.mapper.gateway.gis.GatewayGisMapper;
import com.datamaster.module.service.service.gateway.gis.IGatewayGisService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据服务-GIS网关 Service 实现
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GatewayGisServiceImpl extends ServiceImpl<GatewayGisMapper, GatewayGisDO>
        implements IGatewayGisService, IGatewayGisOutService {
    @Resource
    private GatewayGisMapper GatewayGisMapper;

    @Override
    public PageResult<GatewayGisDO> getGatewayGisPage(GatewayGisPageReqVO pageReqVO) {
        return GatewayGisMapper.selectPage(pageReqVO);
    }

    @Override
    public GatewayGisRespVO getGatewayGisByAssetId(Long assetId) {
        LambdaQueryWrapperX<GatewayGisDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(GatewayGisDO::getAssetId, assetId);
        GatewayGisDO gatewayGisDO = GatewayGisMapper.selectOne(queryWrapperX);
        return BeanUtils.toBean(gatewayGisDO, GatewayGisRespVO.class);
    }

    @Override
    public Long createGatewayGis(GatewayGisSaveReqVO createReqVO) {
        GatewayGisDO dictType = BeanUtils.toBean(createReqVO, GatewayGisDO.class);
        GatewayGisMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateGatewayGis(GatewayGisSaveReqVO updateReqVO) {
        GatewayGisDO updateObj = BeanUtils.toBean(updateReqVO, GatewayGisDO.class);
        return GatewayGisMapper.updateById(updateObj);
    }

    @Override
    public int removeGatewayGis(Collection<Long> idList) {
        return GatewayGisMapper.deleteBatchIds(idList);
    }

    @Override
    public GatewayGisDO getGatewayGisById(Long id) {
        return GatewayGisMapper.selectById(id);
    }

    @Override
    public List<GatewayGisDO> getGatewayGisList() {
        return GatewayGisMapper.selectList();
    }

    @Override
    public Map<Long, GatewayGisDO> getGatewayGisMap() {
        List<GatewayGisDO> gatewayGisList = GatewayGisMapper.selectList();
        return gatewayGisList.stream()
                .collect(Collectors.toMap(
                        GatewayGisDO::getId,
                        gatewayGisDO -> gatewayGisDO,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public String importGatewayGis(List<GatewayGisRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (GatewayGisRespVO respVO : importExcelList) {
            try {
                GatewayGisDO gatewayGisDO = BeanUtils.toBean(respVO, GatewayGisDO.class);
                Long gatewayGisId = respVO.getId();
                if (isUpdateSupport) {
                    if (gatewayGisId != null) {
                        GatewayGisDO existingGatewayGis = GatewayGisMapper.selectById(gatewayGisId);
                        if (existingGatewayGis != null) {
                            GatewayGisMapper.updateById(gatewayGisDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + gatewayGisId + " 的GIS网关记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + gatewayGisId + " 的GIS网关记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<GatewayGisDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", gatewayGisId);
                    GatewayGisDO existingGatewayGis = GatewayGisMapper.selectOne(queryWrapper);
                    if (existingGatewayGis == null) {
                        GatewayGisMapper.insert(gatewayGisDO);
                        successNum++;
                        successMessages.add("数据导入成功，ID为 " + gatewayGisId + " 的GIS网关记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据导入失败，ID为 " + gatewayGisId + " 的GIS网关记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("导入失败 ").append(failureNum).append(" 条");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("导入成功 ").append(successNum).append(" 条");
        }
        return resultMsg.toString();
    }

    @Override
    public void queryServiceForwarding(HttpServletResponse response, GatewayGisReqVO gatewayGis) {
        Map<String, Object> queryParams = gatewayGis.getQueryParams() == null ? new HashMap<>() : gatewayGis.getQueryParams();
        queryParams.put("spaceId", gatewayGis.getSpaceId());
        queryParams.put("spaceCode", gatewayGis.getSpaceCode());
        this.executeServiceForwarding(response, gatewayGis.getId(), queryParams);
    }

    @Override
    public void executeServiceForwarding(HttpServletResponse response, Long gisId, Map<String, Object> queryParams) {
        //根据id获取三方GIS配置
        GatewayGisDO gatewayGisDO = this.getGatewayGisById(gisId);

        //判断api信息，例如是否启用等
        chackYapiConfig(gatewayGisDO);

        //取出Url
        String url = gatewayGisDO.getUrl();

        //封装header
        List<HeaderEntity> headerEntities = packHeadersOrYApiField(queryParams);
        //进行三方GIS服务的调取
        try {
            //取出调取方式
            String reqMethod = gatewayGisDO.getHttpMethod();
            //取出入参数
            Map<String, Object> params = (Map<String, Object>) MapUtils.getMap(queryParams, "params", new HashMap<>());
            this.fillDefaultWmtsParams(params, reqMethod);
            //get
            if (StringUtils.equals(HttpUtils.GET, reqMethod)) {//封装get请求
                HttpUtils.sendGet(HttpUtils.packGetRequestURL(url, params), response, headerEntities);
            } else if (StringUtils.equals(HttpUtils.POST, reqMethod)) {//post
                HttpUtils.sendPost(url, params, response, headerEntities);
            } else {//未知
                throw new DataQueryException("不支持的请求方式");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new DataQueryException("调用三方GIS服务出错");
        }
    }

    private void chackYapiConfig(GatewayGisDO gatewayGisDO) {
        //判断是否为null
        if (gatewayGisDO == null) {
            throw new DataQueryException("未查询到GIS网关配置");
        }
    }

    /**
     * 填充WMTS默认参数
     *
     * @param params    请求参数Map
     * @param reqMethod 请求方式
     */
    private void fillDefaultWmtsParams(Map<String, Object> params, String reqMethod) {
        String service = MapUtils.getString(params, "service");
        if (StringUtils.isBlank(service)) {
            params.put("service", "WMTS");
        }

        String request = MapUtils.getString(params, "request");
        if (StringUtils.isBlank(request)) {
            params.put("request", "GetCapabilities");
        }

        String version = MapUtils.getString(params, "version");
        if (StringUtils.isBlank(version)) {
            params.put("version", "1.0.0");
        }
    }

    /**
     * 封装请求Header
     *
     * @param queryParams
     * @return
     */
    public static List<HeaderEntity> packHeadersOrYApiField(Map<String, Object> queryParams) {
        List<Map<String, Object>> fieldHerderList = (List<Map<String, Object>>) MapUtils.getObject(queryParams, "fieldHerderList", new ArrayList<>());

        //封装 headers
        List<HeaderEntity> headerEntityList = new ArrayList<>();
        if (CollectionUtils.isEmpty(fieldHerderList)) {
            return headerEntityList;
        }

        for (Map<String, Object> stringObjectMap : fieldHerderList) {
            if (MapUtils.isNotEmpty(stringObjectMap)) {
                HeaderEntity headerEntity = new HeaderEntity();
                headerEntity.setKey(MapUtils.getString(stringObjectMap, "name"));
                String defaultValue = MapUtils.getString(stringObjectMap, "defaultValue");
                if (defaultValue == null) {
                    throw new DataQueryException("请求Header默认值不能为空");
                }
                headerEntity.setValue(defaultValue);
                headerEntityList.add(headerEntity);
            }
        }
        return headerEntityList;
    }
}
