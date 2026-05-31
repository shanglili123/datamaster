package com.datamaster.module.assets.service.assetColumn.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnRespVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.mapper.assetColumn.AssetsAssetColumnMapper;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.assetColumn.IAssetsAssetColumnService;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemAssetRelReqDTO;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemRespDTO;
import com.datamaster.module.standards.api.service.dataElem.IDataElemRuleRelService;
import com.datamaster.module.standards.api.service.model.IStandardsModelApiService;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service
 *
 * @author lhs
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetColumnServiceImpl extends ServiceImpl<AssetsAssetColumnMapper, AssetsAssetColumnDO> implements IAssetsAssetColumnService {
    @Resource
    private AssetsAssetColumnMapper AssetsAssetColumnMapper;
    @Resource
    private IStandardsModelApiService iStandardsModelApiService;
    @Resource
    private IDataElemRuleRelService dataElemRuleRelService;
    @Resource
    private IAssetsAssetService AssetsAssetService;

    @Override
    public AjaxResult getColumnByAssetId(AssetsAssetColumnPageReqVO pageReqVO) {
        if (StringUtils.isEmpty(pageReqVO.getAssetId())) {//资产id不能为空
            return AjaxResult.error("id");
        }
        List<AssetsAssetColumnDO> list = this.lambdaQuery()
                .eq(AssetsAssetColumnDO::getAssetId, pageReqVO.getAssetId())
                .eq(AssetsAssetColumnDO::getDelFlag, "0")
                .orderByAsc(AssetsAssetColumnDO::getId)
                .list();

        for (AssetsAssetColumnDO AssetsAssetColumnDO : list) {
            Set<Long> dpDataElemListByAssetIdApi = iStandardsModelApiService.getDpDataElemListByAssetIdAndColumnId(AssetsAssetColumnDO.getAssetId(), AssetsAssetColumnDO.getId());
            AssetsAssetColumnDO.setElementId(dpDataElemListByAssetIdApi);
            if (dpDataElemListByAssetIdApi.size() > 0) {
                AssetsAssetColumnDO.setCleanRuleList(dataElemRuleRelService.listByDataElemIdList(new ArrayList<>(dpDataElemListByAssetIdApi), "2"));
            }
        }
        return AjaxResult.success(list);
    }

    @Override
    public List<AssetsAssetColumnDO> getDaAssetColumnList(AssetsAssetColumnPageReqVO pageReqVO) {
        MPJLambdaWrapper<AssetsAssetColumnDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.eq(StringUtils.isNotEmpty(pageReqVO.getAssetId()), AssetsAssetColumnDO::getAssetId, pageReqVO.getAssetId())
                .eq(StringUtils.isNotEmpty(pageReqVO.getSensitiveLevelId()), AssetsAssetColumnDO::getSensitiveLevelId, pageReqVO.getSensitiveLevelId());
        return AssetsAssetColumnMapper.selectList(lambdaWrapper);
    }

    @Override
    public PageResult<AssetsAssetColumnDO> getDaAssetColumnPage(AssetsAssetColumnPageReqVO pageReqVO) {
        if (StringUtils.isEmpty(pageReqVO.getAssetId())) {
            return PageResult.empty();
        }
        PageResult<AssetsAssetColumnDO> AssetsAssetColumnDOPageResult = AssetsAssetColumnMapper.selectPage(pageReqVO);
        Set<Long> ids = new HashSet<>();
        List<?> rows = AssetsAssetColumnDOPageResult.getRows();
        for (Object row : rows) {
            AssetsAssetColumnDO AssetsAssetColumnDO = (AssetsAssetColumnDO) row;
            ids.add(AssetsAssetColumnDO.getDataElemCodeId());
        }
        List<StandardsDataElemRespDTO> dpDataElemListByAssetId = iStandardsModelApiService.getDpDataElemListByAssetId(Long.valueOf(pageReqVO.getAssetId()), ids);
        for (Object row : rows) {
            AssetsAssetColumnDO AssetsAssetColumnDO = (AssetsAssetColumnDO) row;
            String elementName = "";
            for (StandardsDataElemRespDTO dpDataElemRespDTO : dpDataElemListByAssetId) {
                if (dpDataElemRespDTO.getId().equals(AssetsAssetColumnDO.getDataElemCodeId())) {
                    AssetsAssetColumnDO.setDataElemCodeName(dpDataElemRespDTO.getName());
                }
                if (dpDataElemRespDTO.getColumnId() != null && dpDataElemRespDTO.getColumnId().contains(AssetsAssetColumnDO.getId())) {
                    elementName = dpDataElemRespDTO.getName() + "";
                }
            }
            AssetsAssetColumnDO.setRelDataElmeName(elementName);
        }
        return AssetsAssetColumnDOPageResult;
    }

    @Override
    public Long createDaAssetColumn(AssetsAssetColumnSaveReqVO createReqVO) {
        AssetsAssetColumnDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetColumnDO.class);
        AssetsAssetColumnMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDaAssetColumn(AssetsAssetColumnSaveReqVO updateReqVO) {
        AssetsAssetDO AssetsAssetDO = AssetsAssetService.getById(updateReqVO.getAssetId());
        if (AssetsAssetDO == null) {
            throw new ServiceException("");
        }
        //维护数据元数据资产关联信息表信息
        StandardsDataElemAssetRelReqDTO dpDataElemAssetRelReqDTO = new StandardsDataElemAssetRelReqDTO();
        dpDataElemAssetRelReqDTO.setTableName(AssetsAssetDO.getTableName());
        dpDataElemAssetRelReqDTO.setColumnName(updateReqVO.getColumnName());
        dpDataElemAssetRelReqDTO.setColumnId(updateReqVO.getId());
        dpDataElemAssetRelReqDTO.setAssetId(AssetsAssetDO.getId());
        dpDataElemAssetRelReqDTO.setElementIds(updateReqVO.getElementId());
        boolean b = iStandardsModelApiService.updateElementAssetRelation(dpDataElemAssetRelReqDTO);
//
    if(!b){
//
    throw new ServiceException("数据元和资产关系数据更新失败");
//
    }
        //不是代码，将代码表关联id制空
        AssetsAssetColumnDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetColumnDO.class);
        if (StringUtils.isEmpty(updateObj.getDataElemCodeFlag()) || "0".equals(updateObj.getDataElemCodeFlag())) {
            updateObj.setDataElemCodeId(null);
        }
        // 更新数据资产字段
        return AssetsAssetColumnMapper.updateDaAssetColumn(updateObj);
    }

    @Override
    public int removeDaAssetColumn(Collection<Long> idList) {
        // 批量删除数据资产字段
        return AssetsAssetColumnMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetColumnDO getDaAssetColumnById(Long id) {
        AssetsAssetColumnDO AssetsAssetColumnDO = AssetsAssetColumnMapper.selectById(id);
        //查询数据元id
        Set<Long> dpDataElemListByAssetIdApi = iStandardsModelApiService.getDpDataElemListByAssetIdApi(AssetsAssetColumnDO.getId());
        AssetsAssetColumnDO.setElementId(dpDataElemListByAssetIdApi);
        return AssetsAssetColumnDO;
    }

    @Override
    public List<AssetsAssetColumnDO> getDaAssetColumnList() {
        return AssetsAssetColumnMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetColumnDO> getDaAssetColumnMap() {
        List<AssetsAssetColumnDO> AssetsAssetColumnList = AssetsAssetColumnMapper.selectList();
        return AssetsAssetColumnList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetColumnDO::getId,
                        AssetsAssetColumnDO -> AssetsAssetColumnDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    @Override
    public String importDaAssetColumn(List<AssetsAssetColumnRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetColumnRespVO respVO : importExcelList) {
            try {
                AssetsAssetColumnDO AssetsAssetColumnDO = BeanUtils.toBean(respVO, AssetsAssetColumnDO.class);
                Long AssetsAssetColumnId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetColumnId != null) {
                        AssetsAssetColumnDO existingDaAssetColumn = AssetsAssetColumnMapper.selectById(AssetsAssetColumnId);
                        if (existingDaAssetColumn != null) {
                            AssetsAssetColumnMapper.updateById(AssetsAssetColumnDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetColumnId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetColumnId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetColumnDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetColumnId);
                    AssetsAssetColumnDO existingDaAssetColumn = AssetsAssetColumnMapper.selectOne(queryWrapper);
                    if (existingDaAssetColumn == null) {
                        AssetsAssetColumnMapper.insert(AssetsAssetColumnDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetColumnId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetColumnId + " ");
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
}
