package com.datamaster.module.assets.service.assetchild.gis.impl;

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
import com.datamaster.module.assets.api.service.assetchild.gis.IAssetsAssetGisOutService;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.gis.AssetsAssetGisDO;
import com.datamaster.module.assets.dal.mapper.assetchild.gis.AssetsAssetGisMapper;
import com.datamaster.module.assets.service.assetchild.gis.IAssetsAssetGisService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetGisServiceImpl  extends ServiceImpl<AssetsAssetGisMapper,AssetsAssetGisDO> implements IAssetsAssetGisService, IAssetsAssetGisOutService {
    @Resource
    private AssetsAssetGisMapper AssetsAssetGisMapper;

    @Override
    public PageResult<AssetsAssetGisDO> getAssetGisPage(AssetsAssetGisPageReqVO pageReqVO) {
        return AssetsAssetGisMapper.selectPage(pageReqVO);
    }

    @Override
    public AssetsAssetGisRespVO getAssetGisByAssetId(Long assetId) {
        LambdaQueryWrapperX<AssetsAssetGisDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(AssetsAssetGisDO::getAssetId,assetId);
        AssetsAssetGisDO AssetsAssetApiDO = AssetsAssetGisMapper.selectOne(queryWrapperX);
        return BeanUtils.toBean(AssetsAssetApiDO, AssetsAssetGisRespVO.class);
    }

    @Override
    public Long createAssetGis(AssetsAssetGisSaveReqVO createReqVO) {
        AssetsAssetGisDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetGisDO.class);
        AssetsAssetGisMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAssetGis(AssetsAssetGisSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产-地理空间服务
        AssetsAssetGisDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetGisDO.class);
        return AssetsAssetGisMapper.updateById(updateObj);
    }
    @Override
    public int removeAssetGis(Collection<Long> idList) {
        // 批量删除数据资产-地理空间服务
        return AssetsAssetGisMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetGisDO getAssetGisById(Long id) {
        return AssetsAssetGisMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetGisDO> getAssetGisList() {
        return AssetsAssetGisMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetGisDO> getAssetGisMap() {
        List<AssetsAssetGisDO> AssetsAssetGisList = AssetsAssetGisMapper.selectList();
        return AssetsAssetGisList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetGisDO::getId,
                        AssetsAssetGisDO -> AssetsAssetGisDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

        /**
         * -
         *
         * @param importExcelList -
         * @param isUpdateSupport
         * @param operName
         * @return
         */
        @Override
        public String importAssetGis(List<AssetsAssetGisRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (AssetsAssetGisRespVO respVO : importExcelList) {
                try {
                    AssetsAssetGisDO AssetsAssetGisDO = BeanUtils.toBean(respVO, AssetsAssetGisDO.class);
                    Long AssetsAssetGisId = respVO.getId();
                    if (isUpdateSupport) {
                        if (AssetsAssetGisId != null) {
                            AssetsAssetGisDO existingAssetGis = AssetsAssetGisMapper.selectById(AssetsAssetGisId);
                            if (existingAssetGis != null) {
                                AssetsAssetGisMapper.updateById(AssetsAssetGisDO);
                                successNum++;
                                successMessages.add("ID " + AssetsAssetGisId + " -");
                            } else {
                                failureNum++;
                                failureMessages.add("ID " + AssetsAssetGisId + " -");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("ID");
                        }
                    } else {
                        QueryWrapper<AssetsAssetGisDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", AssetsAssetGisId);
                        AssetsAssetGisDO existingAssetGis = AssetsAssetGisMapper.selectOne(queryWrapper);
                        if (existingAssetGis == null) {
                            AssetsAssetGisMapper.insert(AssetsAssetGisDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetGisId + " -");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetGisId + " -");
                        }
                    }
                } catch (Exception e) {
                    failureNum++;
                    String errorMsg = "" + e.getMessage();
                    failureMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }
            StringBuilder resultMsg = new StringBuilder();
            if (failureNum > 0) {
                resultMsg.append(" ").append(failureNum).append(" ");
                resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
                throw new ServiceException(resultMsg.toString());
            } else {
                resultMsg.append(" ").append(successNum).append(" ");
            }
            return resultMsg.toString();
        }

    @Override
    public void queryServiceForwarding(HttpServletResponse response, AssetsAssetGisReqVO AssetsAssetGisReqVO) {
        this.executeServiceForwarding(response,AssetsAssetGisReqVO.getId(),AssetsAssetGisReqVO.getQueryParams());
    }

    @Override
    public void executeServiceForwarding(HttpServletResponse response, Long gisId, Map<String, Object> queryParams) {
        //很具id 获取三方api配置
        AssetsAssetGisDO AssetsAssetGisDO = this.getAssetGisById(gisId);

        //判断api信息，例如是否启用等
        chackYapiConfig(AssetsAssetGisDO);

        //取出Url
        String url = AssetsAssetGisDO.getUrl();

        //封装header
        List<HeaderEntity> headerEntities = packHeadersOrYApiField(queryParams);
        //进行三方api的调取
        try {
            //取出调取方式
            String reqMethod = AssetsAssetGisDO.getHttpMethod();
            //取出入参数
            Map<String, Object> params = ( Map<String, Object>) MapUtils.getMap(queryParams, "params", new HashMap<>());
            this.fillDefaultWmtsParams(params,reqMethod);
            //get
            if (StringUtils.equals(HttpUtils.GET, reqMethod)) {//封装get请求
                HttpUtils.sendGet(HttpUtils.packGetRequestURL(url, params), response, headerEntities);
            } else if (StringUtils.equals(HttpUtils.POST, reqMethod)) {//post
                HttpUtils.sendPost(url, params, response, headerEntities);
            } else {//未知
                throw new DataQueryException("API");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new DataQueryException("Http");
        }
    }

    private void chackYapiConfig(AssetsAssetGisDO AssetsAssetGisDO) {
        //判断是否为null
        if (AssetsAssetGisDO == null) {
            throw new DataQueryException("APIapi");
        }
    }

    /**
     *  WMTS
     * @param params  Map
     */
    private void fillDefaultWmtsParams(Map<String, Object> params,String reqMethod ) {
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
     * Header
     *
     * @param queryParams
     * @return
     */
    public static List<HeaderEntity> packHeadersOrYApiField(Map<String, Object> queryParams) {
        List<Map<String,Object>> fieldHerderList = (List<Map<String,Object>>)MapUtils.getObject(queryParams, "fieldHerderList", new ArrayList<>());

        //封装 headers
        List<HeaderEntity> headerEntityList = new ArrayList<>();
        if(CollectionUtils.isEmpty(fieldHerderList)){
            return headerEntityList;
        }

        for (Map<String, Object> stringObjectMap : fieldHerderList) {
            if(MapUtils.isNotEmpty(stringObjectMap)){
                HeaderEntity headerEntity = new HeaderEntity();
                headerEntity.setKey(MapUtils.getString(stringObjectMap,"name"));
                String defaultValue = MapUtils.getString(stringObjectMap, "defaultValue");
                if(defaultValue == null){
                    throw new DataQueryException("Headernull");
                }
                headerEntity.setValue(defaultValue);
                headerEntityList.add(headerEntity);
            }
        }
        return headerEntityList;
    }
}
