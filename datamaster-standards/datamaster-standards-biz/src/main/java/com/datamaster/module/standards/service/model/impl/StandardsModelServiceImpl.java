package com.datamaster.module.standards.service.model.impl;

import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemAssetRelReqDTO;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemAssetRelRespDTO;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemRespDTO;
import com.datamaster.module.standards.api.model.dto.StandardsModelColumnRespDTO;
import com.datamaster.module.standards.api.model.dto.StandardsModelRespDTO;
import com.datamaster.module.standards.api.service.model.IStandardsModelApiService;
import com.datamaster.module.standards.controller.admin.model.vo.*;
import com.datamaster.module.standards.convert.model.StandardsModelColumnConvert;
import com.datamaster.module.standards.convert.model.StandardsModelConvert;
import com.datamaster.module.standards.convert.model.StandardsModelMaterializedConvert;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemAssetRelDO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemDO;
import com.datamaster.module.standards.dal.dataobject.document.StandardsDocumentDO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelColumnDO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelDO;
import com.datamaster.module.standards.dal.mapper.model.StandardsModelMapper;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemAssetRelService;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemService;
import com.datamaster.module.standards.service.document.IStandardsDocumentService;
import com.datamaster.module.standards.service.model.IStandardsModelColumnService;
import com.datamaster.module.standards.service.model.IStandardsModelService;

/**
 * 逻辑模型Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsModelServiceImpl extends ServiceImpl<StandardsModelMapper, StandardsModelDO> implements IStandardsModelService, IStandardsModelApiService {
    @Resource
    private StandardsModelMapper StandardsModelMapper;
    @Resource
    private IStandardsModelColumnService iDpModelColumnService;
    @Resource
    private IStandardsDataElemService iDpDataElemService;

    @Resource
    private IStandardsDataElemAssetRelService iDpDataElemAssetRelService;
    @Resource
    private IAssetsDatasourceApiService assetsDatasourceApiService;

    @Resource
    private IStandardsDocumentService StandardsDocumentService;


    /**
     * 根据资产id和代码表id查询数据元信息
     *
     * @param assetId 资产id
     * @param codeId  代码表id
     * @return
     */
    @Override
    public List<StandardsDataElemRespDTO> getDpDataElemListByAssetId(Long assetId, Set<Long> codeId) {
        //查询和资产关联的数据元信息id
        Set<Long> ids = new HashSet<>();
        List<StandardsDataElemAssetRelDO> list = iDpDataElemAssetRelService.lambdaQuery()
                .eq(StandardsDataElemAssetRelDO::getAssetId, assetId)
                .list();
        if (CollectionUtils.isNotEmpty(list)) {
            for (StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO : list) {
                ids.add(Long.valueOf(StandardsDataElemAssetRelDO.getDataElemId()));
            }
        }
        ids.addAll(codeId);
        List<StandardsDataElemDO> StandardsDataElemDOS = new ArrayList<>();
        if (StringUtils.isNotEmpty(ids)) {
            StandardsDataElemDOS = iDpDataElemService.lambdaQuery()
                    .in(StandardsDataElemDO::getId, ids)
                    .list();
            for (StandardsDataElemDO StandardsDataElemDO : StandardsDataElemDOS) {
                Set<Long> columnId = new HashSet<>();
                for (StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO : list) {
                    if (StandardsDataElemAssetRelDO.getDataElemId().equals(StandardsDataElemDO.getId().toString())) {
                        columnId.add(Long.valueOf(StandardsDataElemAssetRelDO.getColumnId()));
                    }
                }
                StandardsDataElemDO.setColumnId(columnId);
            }
        }

        return BeanUtils.toBean(StandardsDataElemDOS, StandardsDataElemRespDTO.class);
    }

    /**
     * 更具模型id查询模型下的字段集合
     *
     * @param modelId 模型id
     */
    @Override
    public List<StandardsModelColumnRespDTO> getModelIdColumnList(Long modelId) {
        StandardsModelColumnSaveReqVO StandardsModelColumnSaveReqVO = new StandardsModelColumnSaveReqVO();
        StandardsModelColumnSaveReqVO.setModelId(modelId);
        List<StandardsModelColumnDO> StandardsModelColumnList = iDpModelColumnService.getDpModelColumnList(StandardsModelColumnSaveReqVO);
        List<StandardsModelColumnRespDTO> StandardsModelColumnRespDTOList = BeanUtils.toBean(StandardsModelColumnList, StandardsModelColumnRespDTO.class);
        return StandardsModelColumnRespDTOList;
    }

    /**
     * 根据字段id获取数据元id集合
     *
     * @param columnId
     * @return
     */
    @Override
    public Set<Long> getDpDataElemListByAssetIdApi(Long columnId) {
        Set<Long> result = new HashSet<>();
        List<StandardsDataElemAssetRelDO> list = iDpDataElemAssetRelService.lambdaQuery()
                .select(StandardsDataElemAssetRelDO::getDataElemId)
                .eq(StandardsDataElemAssetRelDO::getColumnId, columnId)
                .eq(StandardsDataElemAssetRelDO::getDelFlag, "0")
                .list();
        if (CollectionUtils.isNotEmpty(list)) {
            for (StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO : list) {
                result.add(Long.valueOf(StandardsDataElemAssetRelDO.getDataElemId()));
            }
        }

        return result;
    }

    @Override
    public List<StandardsDataElemAssetRelRespDTO> getDpDataElemListByColumnIdInApi(Collection<Long> columnIds) {
        List<StandardsDataElemAssetRelDO> list = iDpDataElemAssetRelService.lambdaQuery()
                .in(StandardsDataElemAssetRelDO::getColumnId, columnIds)
                .eq(StandardsDataElemAssetRelDO::getDelFlag, "0")
                .list();
        return BeanUtils.toBean(list, StandardsDataElemAssetRelRespDTO.class);
    }

    @Override
    public Set<Long> getDpDataElemListByAssetIdAndColumnId(Long assetId, Long columnId) {
        Set<Long> result = new HashSet<>();
        List<StandardsDataElemAssetRelDO> list = iDpDataElemAssetRelService.lambdaQuery()
                .select(StandardsDataElemAssetRelDO::getDataElemId)
                .eq(StandardsDataElemAssetRelDO::getAssetId, assetId)
                .eq(StandardsDataElemAssetRelDO::getColumnId, columnId)
                .list();
        if (CollectionUtils.isNotEmpty(list)) {
            for (StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO : list) {
                result.add(Long.valueOf(StandardsDataElemAssetRelDO.getDataElemId()));
            }
        }
        return result;
    }


    /**
     * 更新数据元和资产关系数据
     *
     * @param StandardsDataElemAssetRel
     * @return
     */
    @Override
    public boolean updateElementAssetRelation(StandardsDataElemAssetRelReqDTO StandardsDataElemAssetRel) {
        boolean save = false;
        Long assetId = StandardsDataElemAssetRel.getAssetId();
        iDpDataElemAssetRelService.lambdaUpdate()
                .eq(StandardsDataElemAssetRelDO::getAssetId, assetId)
                .remove();
        Set<Long> elementIds = StandardsDataElemAssetRel.getElementIds();
        List<StandardsDataElemAssetRelDO> StandardsDataElemAssetRelDOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(elementIds)) {
            StandardsDataElemAssetRelDOList = elementIds.stream().map(item -> {
                StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO = new StandardsDataElemAssetRelDO();
                StandardsDataElemAssetRelDO.setAssetId(String.valueOf(assetId));//资产id
                StandardsDataElemAssetRelDO.setDataElemId(String.valueOf(item));//数据元id
                StandardsDataElemAssetRelDO.setDataElemType("1");//是数据元
                StandardsDataElemAssetRelDO.setTableName(StandardsDataElemAssetRel.getTableName());
                StandardsDataElemAssetRelDO.setColumnId(String.valueOf(StandardsDataElemAssetRel.getColumnId()));
                StandardsDataElemAssetRelDO.setColumnName(StandardsDataElemAssetRel.getColumnName());
                return StandardsDataElemAssetRelDO;
            }).collect(Collectors.toList());
        }
        for (StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO : StandardsDataElemAssetRelDOList) {
            save = iDpDataElemAssetRelService.save(StandardsDataElemAssetRelDO);
        }
        return save;
    }

    /**
     * 插入数据元和资产关系数据
     *
     * @param StandardsDataElemAssetRel
     * @return
     */
    @Override
    public boolean insertElementAssetRelation(List<StandardsDataElemAssetRelReqDTO> StandardsDataElemAssetRel) {
        boolean result = false;
        if (CollectionUtils.isNotEmpty(StandardsDataElemAssetRel)) {
            //StandardsDataElemAssetRelReqDTO 转换为 StandardsDataElemAssetRelDO
            List<StandardsDataElemAssetRelDO> StandardsDataElemAssetRelDOList = StandardsDataElemAssetRel.stream().map(item -> {
                StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO = new StandardsDataElemAssetRelDO();
                BeanUtil.copyProperties(item, StandardsDataElemAssetRelDO);
                return StandardsDataElemAssetRelDO;
            }).collect(Collectors.toList());
//            result = iDpDataElemAssetRelService.saveBatch(StandardsDataElemAssetRelDOList);
            for (StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO : StandardsDataElemAssetRelDOList) {
                result = iDpDataElemAssetRelService.save(StandardsDataElemAssetRelDO);
            }
        }
        return result;
    }

    @Override
    public Long getCountByCatCode(String catCode) {
        return baseMapper.selectCount(Wrappers.lambdaQuery(StandardsModelDO.class)
                .likeRight(StandardsModelDO::getCatCode, catCode));
    }

    /**
     * 根据数据元id查询数据元信息
     *
     * @param ids
     * @return
     */
    @Override
    public List<StandardsDataElemRespDTO> getDpDataElemListByIdsApi(Set<Long> ids) {

        List<StandardsDataElemDO> list = iDpDataElemService.lambdaQuery()
                .in(StandardsDataElemDO::getId, ids)
                .eq(StandardsDataElemDO::getDelFlag, "0")
                .list();
        //将list的类型转换为StandardsDataElemRespDTO
        return list.stream().map(item -> {
            StandardsDataElemRespDTO StandardsModelColumnRespDTO = new StandardsDataElemRespDTO();
            BeanUtil.copyProperties(item, StandardsModelColumnRespDTO);
            return StandardsModelColumnRespDTO;
        }).collect(Collectors.toList());
    }

    /**
     * 根据逻辑模型ID获取逻辑模型列信息
     *
     * @param modelId 逻辑模型ID
     * @return 逻辑模型列信息
     */
    @Override
    public List<StandardsModelColumnRespDTO> getDpModelColumnListByModelIdApi(Long modelId) {
        List<StandardsModelColumnDO> list = iDpModelColumnService.lambdaQuery()
                .eq(StandardsModelColumnDO::getModelId, modelId)
                .list();
        //将list的类型转换为StandardsModelColumnRespDTO
        return list.stream().map(item -> {
            StandardsModelColumnRespDTO StandardsModelColumnRespDTO = new StandardsModelColumnRespDTO();
            BeanUtil.copyProperties(item, StandardsModelColumnRespDTO);
            return StandardsModelColumnRespDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public PageResult<StandardsModelDO> getDpModelPage(StandardsModelPageReqVO pageReqVO) {
        PageResult<StandardsModelDO> StandardsModelDOPageResult = StandardsModelMapper.selectPage(pageReqVO);
        List<StandardsModelDO> rows = (List<StandardsModelDO>) StandardsModelDOPageResult.getRows();
        if (CollectionUtils.isEmpty(rows)) {
            return StandardsModelDOPageResult;
        }
        for (StandardsModelDO row : rows) {
            //字段
            StandardsModelColumnSaveReqVO StandardsModelColumnSaveReqVO = new StandardsModelColumnSaveReqVO();
            StandardsModelColumnSaveReqVO.setModelId(row.getId());
            long count = iDpModelColumnService.countByDpModelColumn(StandardsModelColumnSaveReqVO);
            row.setColumnCount(count);

            //资产

        }
        StandardsModelDOPageResult.setRows(rows);
        return StandardsModelDOPageResult;
    }

    @Override
    public Long createDpModel(StandardsModelSaveReqVO createReqVO) {
        StandardsModelDO dictType = BeanUtils.toBean(createReqVO, StandardsModelDO.class);
        StandardsModelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpModel(StandardsModelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新逻辑模型
        StandardsModelDO updateObj = BeanUtils.toBean(updateReqVO, StandardsModelDO.class);
        return StandardsModelMapper.updateById(updateObj);
    }

    @Override
    public int removeDpModel(Collection<Long> idList) {
        // 批量删除逻辑模型
        return StandardsModelMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsModelDO getDpModelById(Long id) {
        MPJLambdaWrapper<StandardsModelDO> mpjLambdaWrapper = new MPJLambdaWrapper();
        mpjLambdaWrapper.selectAll(StandardsModelDO.class)
                .select("t2.name AS catName")
                .leftJoin("TAX_MODEL_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .eq(StandardsModelDO::getId, id);
        StandardsModelDO StandardsModelDO = StandardsModelMapper.selectJoinOne(StandardsModelDO.class, mpjLambdaWrapper);
        if (StandardsModelDO == null) {
            return null;
        }
        if ("2".equals(StandardsModelDO.getCreateType())) {
            AssetsDatasourceRespDTO datasource = assetsDatasourceApiService.getDatasourceById(StandardsModelDO.getDatasourceId());
            StandardsModelDO.setPort(datasource.getPort());
            StandardsModelDO.setIp(datasource.getIp());
            StandardsModelDO.setDatasourceConfig(datasource.getDatasourceConfig());
            StandardsModelDO.setDatasourceType(datasource.getDatasourceType());
            StandardsModelDO.setDatasourceName(datasource.getDatasourceName());
        }
        if(StandardsModelDO.getDocumentId() != null){
            StandardsDocumentDO StandardsDocumentById = StandardsDocumentService.getDpDocumentById(StandardsModelDO.getDocumentId());
            StandardsDocumentById = StandardsDocumentById == null ? new StandardsDocumentDO():StandardsDocumentById;
            StandardsModelDO.setDocumentCode(StandardsDocumentById.getCode());
            StandardsModelDO.setDocumentName(StandardsDocumentById.getName());
            StandardsModelDO.setDocumentType(StandardsDocumentById.getType());
        }
        return StandardsModelDO;
    }

    /**
     * 根据逻辑模型ID获取逻辑模型信息
     *
     * @param id
     * @return
     */
    @Override
    public StandardsModelRespDTO getDpModelByIdApi(Long id) {
        StandardsModelRespDTO dto = new StandardsModelRespDTO();
        StandardsModelDO StandardsModelDO = StandardsModelMapper.selectById(id);
        BeanUtil.copyProperties(StandardsModelDO, dto);
        return dto;
    }


    @Override
    public List<StandardsModelDO> getDpModelList() {
        return StandardsModelMapper.selectList();
    }

    @Override
    public Map<Long, StandardsModelDO> getDpModelMap() {
        List<StandardsModelDO> StandardsModelList = StandardsModelMapper.selectList();
        return StandardsModelList.stream()
                .collect(Collectors.toMap(
                        StandardsModelDO::getId,
                        StandardsModelDO -> StandardsModelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入逻辑模型数据
     *
     * @param importExcelList 逻辑模型数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importDpModel(List<StandardsModelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (StandardsModelRespVO respVO : importExcelList) {
            try {
                StandardsModelDO StandardsModelDO = BeanUtils.toBean(respVO, StandardsModelDO.class);
                Long StandardsModelId = respVO.getId();
                if (isUpdateSupport) {
                    if (StandardsModelId != null) {
                        StandardsModelDO existingDpModel = StandardsModelMapper.selectById(StandardsModelId);
                        if (existingDpModel != null) {
                            StandardsModelMapper.updateById(StandardsModelDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + StandardsModelId + " 的逻辑模型记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + StandardsModelId + " 的逻辑模型记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<StandardsModelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", StandardsModelId);
                    StandardsModelDO existingDpModel = StandardsModelMapper.selectOne(queryWrapper);
                    if (existingDpModel == null) {
                        StandardsModelMapper.insert(StandardsModelDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + StandardsModelId + " 的逻辑模型记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + StandardsModelId + " 的逻辑模型记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
        }
        return resultMsg.toString();
    }

    @Override
    public int removeDpModelAndColumnAll(List<Long> asList) {
        int i = StandardsModelMapper.deleteBatchIds(asList);
        iDpModelColumnService.removeDpModelColumnByModelId(asList);
        return i > 0 ? 1 : 0;
    }

    @Override
    public Boolean updateStatus(Long id, Long status) {
        return this.update(Wrappers.lambdaUpdate(StandardsModelDO.class)
                .eq(StandardsModelDO::getId, id)
                .set(StandardsModelDO::getStatus, status));
    }

    @Override
    public int updateCatCode(String oldCatCode, String newCatCode) {
        return StandardsModelMapper.update(null, Wrappers.lambdaUpdate(StandardsModelDO.class)
                .eq(StandardsModelDO::getCatCode, oldCatCode)
                .set(StandardsModelDO::getCatCode, newCatCode));
    }
}
