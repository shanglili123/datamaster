package com.datamaster.module.service.service.gateway.api.impl;

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
import com.datamaster.module.service.api.service.gateway.api.IGatewayApiOutService;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiRespVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiSaveReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiDO;
import com.datamaster.module.service.dal.mapper.gateway.api.GatewayApiMapper;
import com.datamaster.module.service.service.gateway.api.IGatewayApiService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据服务-API网关 Service 实现
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GatewayApiServiceImpl extends ServiceImpl<GatewayApiMapper, GatewayApiDO>
        implements IGatewayApiService, IGatewayApiOutService {
    @Resource
    private GatewayApiMapper GatewayApiMapper;

    @Override
    public PageResult<GatewayApiDO> getGatewayApiPage(GatewayApiPageReqVO pageReqVO) {
        return GatewayApiMapper.selectPage(pageReqVO);
    }

    @Override
    public GatewayApiRespVO getGatewayApiByAssetId(Long assetId) {
        LambdaQueryWrapperX<GatewayApiDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(GatewayApiDO::getAssetId, assetId);
        GatewayApiDO gatewayApiDO = GatewayApiMapper.selectOne(queryWrapperX);
        return BeanUtils.toBean(gatewayApiDO, GatewayApiRespVO.class);
    }

    @Override
    public Long createGatewayApi(GatewayApiSaveReqVO createReqVO) {
        GatewayApiDO dictType = BeanUtils.toBean(createReqVO, GatewayApiDO.class);
        GatewayApiMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateGatewayApi(GatewayApiSaveReqVO updateReqVO) {
        GatewayApiDO updateObj = BeanUtils.toBean(updateReqVO, GatewayApiDO.class);
        return GatewayApiMapper.updateById(updateObj);
    }

    @Override
    public int removeGatewayApi(Collection<Long> idList) {
        return GatewayApiMapper.deleteBatchIds(idList);
    }

    @Override
    public GatewayApiDO getGatewayApiById(Long id) {
        return GatewayApiMapper.selectById(id);
    }

    @Override
    public List<GatewayApiDO> getGatewayApiList() {
        return GatewayApiMapper.selectList();
    }

    @Override
    public Map<Long, GatewayApiDO> getGatewayApiMap() {
        List<GatewayApiDO> gatewayApiList = GatewayApiMapper.selectList();
        return gatewayApiList.stream()
                .collect(Collectors.toMap(
                        GatewayApiDO::getId,
                        gatewayApiDO -> gatewayApiDO,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public String importGatewayApi(List<GatewayApiRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (GatewayApiRespVO respVO : importExcelList) {
            try {
                GatewayApiDO gatewayApiDO = BeanUtils.toBean(respVO, GatewayApiDO.class);
                Long gatewayApiId = respVO.getId();
                if (isUpdateSupport) {
                    if (gatewayApiId != null) {
                        GatewayApiDO existingGatewayApi = GatewayApiMapper.selectById(gatewayApiId);
                        if (existingGatewayApi != null) {
                            GatewayApiMapper.updateById(gatewayApiDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + gatewayApiId + " 的API网关记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + gatewayApiId + " 的API网关记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<GatewayApiDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", gatewayApiId);
                    GatewayApiDO existingGatewayApi = GatewayApiMapper.selectOne(queryWrapper);
                    if (existingGatewayApi == null) {
                        GatewayApiMapper.insert(gatewayApiDO);
                        successNum++;
                        successMessages.add("数据导入成功，ID为 " + gatewayApiId + " 的API网关记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据导入失败，ID为 " + gatewayApiId + " 的API网关记录已存在。");
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
    public void queryServiceForwarding(HttpServletResponse response, GatewayApiReqVO gatewayApi) {
        Map<String, Object> queryParams = gatewayApi.getQueryParams() == null ? new HashMap<>() : gatewayApi.getQueryParams();
        queryParams.put("spaceId", gatewayApi.getSpaceId());
        queryParams.put("spaceCode", gatewayApi.getSpaceCode());
        this.executeServiceForwarding(response, gatewayApi.getId(), queryParams);
    }

    @Override
    public void executeServiceForwarding(HttpServletResponse response, Long apiId, Map<String, Object> queryParams) {
        //根据id获取三方api配置
        GatewayApiDO gatewayApiById = this.getGatewayApiById(apiId);

        //判断api信息，例如是否启用等
        chackYapiConfig(gatewayApiById);

        //取出Url
        String url = gatewayApiById.getUrl();

        //封装header
        List<HeaderEntity> headerEntities = packHeadersOrYApiField(queryParams);
        //进行三方api的调取
        try {
            //取出调取方式
            String reqMethod = gatewayApiById.getHttpMethod();
            //取出入参数
            Map<String, Object> params = (Map<String, Object>) MapUtils.getMap(queryParams, "params", new HashMap<>());
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
            throw new DataQueryException("调用三方HTTP服务出错");
        }
    }

    private void chackYapiConfig(GatewayApiDO gatewayApiById) {
        //判断是否为null
        if (gatewayApiById == null) {
            throw new DataQueryException("未查询到API网关配置");
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
