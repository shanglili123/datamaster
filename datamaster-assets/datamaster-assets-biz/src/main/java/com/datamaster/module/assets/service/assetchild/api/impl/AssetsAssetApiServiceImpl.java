package com.datamaster.module.assets.service.assetchild.api.impl;

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
import com.datamaster.module.assets.api.service.assetchild.api.IAssetsApiOutService;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.api.AssetsAssetApiDO;
import com.datamaster.module.assets.dal.mapper.assetchild.api.AssetsAssetApiMapper;
import com.datamaster.module.assets.service.assetchild.api.IAssetsAssetApiService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * -APIService
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetApiServiceImpl  extends ServiceImpl<AssetsAssetApiMapper,AssetsAssetApiDO> implements IAssetsAssetApiService, IAssetsApiOutService {
    @Resource
    private AssetsAssetApiMapper AssetsAssetApiMapper;

    @Override
    public PageResult<AssetsAssetApiDO> getAssetApiPage(AssetsAssetApiPageReqVO pageReqVO) {
        return AssetsAssetApiMapper.selectPage(pageReqVO);
    }

    @Override
    public AssetsAssetApiRespVO getAssetApiByAssetId(Long assetId) {
        LambdaQueryWrapperX<AssetsAssetApiDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(AssetsAssetApiDO::getAssetId,assetId);
        AssetsAssetApiDO AssetsAssetApiDO = AssetsAssetApiMapper.selectOne(queryWrapperX);
        return BeanUtils.toBean(AssetsAssetApiDO, AssetsAssetApiRespVO.class);
    }

    @Override
    public Long createAssetApi(AssetsAssetApiSaveReqVO createReqVO) {
        AssetsAssetApiDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetApiDO.class);
        AssetsAssetApiMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAssetApi(AssetsAssetApiSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产-外部API
        AssetsAssetApiDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetApiDO.class);
        return AssetsAssetApiMapper.updateById(updateObj);
    }
    @Override
    public int removeAssetApi(Collection<Long> idList) {
        // 批量删除数据资产-外部API
        return AssetsAssetApiMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetApiDO getAssetApiById(Long id) {
        return AssetsAssetApiMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetApiDO> getAssetApiList() {
        return AssetsAssetApiMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetApiDO> getAssetApiMap() {
        List<AssetsAssetApiDO> AssetsAssetApiList = AssetsAssetApiMapper.selectList();
        return AssetsAssetApiList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetApiDO::getId,
                        AssetsAssetApiDO -> AssetsAssetApiDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     * -API
     *
     * @param importExcelList -API
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    @Override
    public String importAssetApi(List<AssetsAssetApiRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetApiRespVO respVO : importExcelList) {
            try {
                AssetsAssetApiDO AssetsAssetApiDO = BeanUtils.toBean(respVO, AssetsAssetApiDO.class);
                Long AssetsAssetApiId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetApiId != null) {
                        AssetsAssetApiDO existingAssetApi = AssetsAssetApiMapper.selectById(AssetsAssetApiId);
                        if (existingAssetApi != null) {
                            AssetsAssetApiMapper.updateById(AssetsAssetApiDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetApiId + " -API");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetApiId + " -API");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetApiDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetApiId);
                    AssetsAssetApiDO existingAssetApi = AssetsAssetApiMapper.selectOne(queryWrapper);
                    if (existingAssetApi == null) {
                        AssetsAssetApiMapper.insert(AssetsAssetApiDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetApiId + " -API");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetApiId + " -API");
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
    public void queryServiceForwarding(HttpServletResponse response, AssetsAssetApiReqVO AssetsAssetApi) {
        this.executeServiceForwarding(response,AssetsAssetApi.getId(),AssetsAssetApi.getQueryParams());
    }

    @Override
    public void executeServiceForwarding(HttpServletResponse response, Long apiId, Map<String, Object> queryParams) {
        //很具id 获取三方api配置
        AssetsAssetApiDO AssetsAssetApiById = this.getAssetApiById(apiId);

        //判断api信息，例如是否启用等
        chackYapiConfig(AssetsAssetApiById);

        //取出Url
        String url = AssetsAssetApiById.getUrl();

        //封装header
        List<HeaderEntity> headerEntities = packHeadersOrYApiField(queryParams);
        //进行三方api的调取
        try {
            //取出调取方式
            String reqMethod = AssetsAssetApiById.getHttpMethod();
            //取出入参数
            Map<String, Object> params = ( Map<String, Object>)MapUtils.getMap(queryParams, "params", new HashMap<>());
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

    private void chackYapiConfig(AssetsAssetApiDO AssetsAssetApiById) {
        //判断是否为null
        if (AssetsAssetApiById == null) {
            throw new DataQueryException("APIapi");
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
