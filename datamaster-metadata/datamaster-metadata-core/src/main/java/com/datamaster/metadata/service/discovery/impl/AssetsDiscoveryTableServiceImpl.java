package com.datamaster.metadata.service.discovery.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetReqDTO;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetRespDTO;
import com.datamaster.module.assets.api.assetColumn.dto.AssetsAssetColumnReqDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsAssetApiOutService;
import com.datamaster.metadata.controller.discovery.vo.*;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryColumnDO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTableDO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTaskDO;
import com.datamaster.metadata.dal.mapper.discovery.AssetsDiscoveryTableMapper;
import com.datamaster.metadata.service.discovery.IAssetsDiscoveryColumnService;
import com.datamaster.metadata.service.discovery.IAssetsDiscoveryTableService;
import com.datamaster.metadata.service.discovery.IAssetsDiscoveryTaskService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsDiscoveryTableServiceImpl  extends ServiceImpl<AssetsDiscoveryTableMapper,AssetsDiscoveryTableDO> implements IAssetsDiscoveryTableService {
    @Resource
    private AssetsDiscoveryTableMapper AssetsDiscoveryTableMapper;
    @Resource
    @Lazy
    private IAssetsDiscoveryColumnService IAssetsDiscoveryColumnService;
    @Resource
    @Lazy
    private IAssetsAssetApiOutService assetsAssetApiOutService;
    @Resource
    @Lazy
    private IAssetsDiscoveryTaskService IAssetsDiscoveryTaskService;

    @Override
    public PageResult<AssetsDiscoveryTableDO> getDaDiscoveryTablePage(AssetsDiscoveryTablePageReqVO pageReqVO) {
        return AssetsDiscoveryTableMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AssetsDiscoveryTableDO> getDaDiscoveryTableList(AssetsDiscoveryTablePageReqVO reqVO) {
        List<Long> taskIds = new ArrayList<>();
        if (reqVO.getDatasourceId() != null) {
            List<AssetsDiscoveryTaskDO> taskList = IAssetsDiscoveryTaskService.list(
                    Wrappers.<AssetsDiscoveryTaskDO>lambdaQuery()
                            .eq(AssetsDiscoveryTaskDO::getDatasourceId, reqVO.getDatasourceId())
                            .orderByDesc(AssetsDiscoveryTaskDO::getLastExecuteTime)
                            .orderByDesc(AssetsDiscoveryTaskDO::getUpdateTime)
                            .orderByDesc(AssetsDiscoveryTaskDO::getId)
            );
            if (CollectionUtils.isEmpty(taskList)) {
                return new ArrayList<>();
            }
            taskIds = taskList.stream().map(AssetsDiscoveryTaskDO::getId).collect(Collectors.toList());
        }
        final List<Long> datasourceTaskIds = taskIds;

        MPJLambdaWrapper<AssetsDiscoveryTableDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(AssetsDiscoveryTableDO.class)
                .eq(reqVO.getTaskId() != null, AssetsDiscoveryTableDO::getTaskId, reqVO.getTaskId())
                .and(reqVO.getDatasourceId() != null, w -> w.eq(AssetsDiscoveryTableDO::getDatasourceId, reqVO.getDatasourceId()).or().in(AssetsDiscoveryTableDO::getTaskId, datasourceTaskIds))
                .like(StringUtils.isNotBlank(reqVO.getTableName()), AssetsDiscoveryTableDO::getTableName, reqVO.getTableName())
                .eq(StringUtils.isNotBlank(reqVO.getTableComment()), AssetsDiscoveryTableDO::getTableComment, reqVO.getTableComment())
                .eq(StringUtils.isNotBlank(reqVO.getChangeFlag()), AssetsDiscoveryTableDO::getChangeFlag, reqVO.getChangeFlag())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), AssetsDiscoveryTableDO::getStatus, reqVO.getStatus())
                .eq(StringUtils.isNotBlank(reqVO.getIgnoreFlag()), AssetsDiscoveryTableDO::getIgnoreFlag, reqVO.getIgnoreFlag());
        if(StringUtils.isNotBlank(reqVO.getKeyword())){
            // 新增的 keyword 模糊匹配
            wrapper.and(q -> q.like(AssetsDiscoveryTableDO::getTableName, reqVO.getKeyword())
                    .or()
                    .like(AssetsDiscoveryTableDO::getTableComment, reqVO.getKeyword()));
        }

        List<AssetsDiscoveryTableDO> tableList = AssetsDiscoveryTableMapper.selectList(wrapper);
        markAssetCreated(reqVO.getDatasourceId(), tableList);
        return tableList;
    }

    private void markAssetCreated(Long datasourceId, List<AssetsDiscoveryTableDO> tableList) {
        if (datasourceId == null || tableList == null || tableList.isEmpty()) {
            return;
        }
        List<AssetsAssetRespDTO> assetList = assetsAssetApiOutService.getAssetRespByDataSourceId(datasourceId, null);
        Map<String, AssetsAssetRespDTO> assetMap = new HashMap<>();
        for (AssetsAssetRespDTO asset : assetList) {
            if (asset.getTableName() != null && !assetMap.containsKey(asset.getTableName())) {
                assetMap.put(asset.getTableName(), asset);
            }
        }
        for (AssetsDiscoveryTableDO table : tableList) {
            table.setDatasourceId(datasourceId);
            AssetsAssetRespDTO asset = assetMap.get(table.getTableName());
            table.setAssetCreatedFlag(asset != null);
            if (asset != null) {
                table.setAssetId(asset.getId());
            }
        }
    }

    @Override
    public Long createDaDiscoveryTable(AssetsDiscoveryTableSaveReqVO createReqVO) {
        AssetsDiscoveryTableDO dictType = BeanUtils.toBean(createReqVO, AssetsDiscoveryTableDO.class);
        AssetsDiscoveryTableMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public Long createDaDiscoveryTable(AssetsDiscoveryTableDO createReqVO) {
        AssetsDiscoveryTableMapper.insert(createReqVO);
        return createReqVO.getId();
    }

    @Override
    public int updateDaDiscoveryTable(AssetsDiscoveryTableSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据发现库信息
        AssetsDiscoveryTableDO updateObj = BeanUtils.toBean(updateReqVO, AssetsDiscoveryTableDO.class);
        return AssetsDiscoveryTableMapper.updateById(updateObj);
    }
    @Override
    public int updateDaDiscoveryTable(AssetsDiscoveryTableDO updateReqVO) {
        // 更新数据发现库信息
        return AssetsDiscoveryTableMapper.updateById(updateReqVO);
    }
    @Override
    public int removeDaDiscoveryTable(Collection<Long> idList) {
        // 批量删除数据发现库信息
        return AssetsDiscoveryTableMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsDiscoveryTableDO getDaDiscoveryTableById(Long id) {
        return AssetsDiscoveryTableMapper.selectById(id);
    }

    @Override
    public List<AssetsDiscoveryTableDO> getDaDiscoveryTableList() {
        return AssetsDiscoveryTableMapper.selectList();
    }

    @Override
    public Map<Long, AssetsDiscoveryTableDO> getDaDiscoveryTableMap() {
        List<AssetsDiscoveryTableDO> AssetsDiscoveryTableList = AssetsDiscoveryTableMapper.selectList();
        return AssetsDiscoveryTableList.stream()
                .collect(Collectors.toMap(
                        AssetsDiscoveryTableDO::getId,
                        AssetsDiscoveryTableDO -> AssetsDiscoveryTableDO,
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
    public String importDaDiscoveryTable(List<AssetsDiscoveryTableRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsDiscoveryTableRespVO respVO : importExcelList) {
            try {
                AssetsDiscoveryTableDO AssetsDiscoveryTableDO = BeanUtils.toBean(respVO, AssetsDiscoveryTableDO.class);
                Long AssetsDiscoveryTableId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsDiscoveryTableId != null) {
                        AssetsDiscoveryTableDO existingDaDiscoveryTable = AssetsDiscoveryTableMapper.selectById(AssetsDiscoveryTableId);
                        if (existingDaDiscoveryTable != null) {
                            AssetsDiscoveryTableMapper.updateById(AssetsDiscoveryTableDO);
                            successNum++;
                            successMessages.add("ID " + AssetsDiscoveryTableId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsDiscoveryTableId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsDiscoveryTableDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsDiscoveryTableId);
                    AssetsDiscoveryTableDO existingDaDiscoveryTable = AssetsDiscoveryTableMapper.selectOne(queryWrapper);
                    if (existingDaDiscoveryTable == null) {
                        AssetsDiscoveryTableMapper.insert(AssetsDiscoveryTableDO);
                        successNum++;
                        successMessages.add("ID " + AssetsDiscoveryTableId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsDiscoveryTableId + " ");
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
    public Integer commitOrRevokeDiscoveryInfo(AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTable) {
        //状态;1:待提交，2:已提交
        String status = AssetsDiscoveryTable.getStatus();
        //是否忽略;0:否，1：是
        String ignoreFlag = AssetsDiscoveryTable.getIgnoreFlag();
        String themeId = AssetsDiscoveryTable.getThemeId();

        //获取信息
        AssetsDiscoveryTableDO AssetsDiscoveryTableById = this.getDaDiscoveryTableById(AssetsDiscoveryTable.getId());
        if(AssetsDiscoveryTableById == null){
            throw new ServiceException("");
        }
        AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById = IAssetsDiscoveryTaskService.getDaDiscoveryTaskById(AssetsDiscoveryTableById.getTaskId());
        if(AssetsDiscoveryTaskById == null){
            throw new ServiceException("");
        }

        if(StringUtils.equals(AssetsDiscoveryTableById.getStatus(),status) && StringUtils.equals(AssetsDiscoveryTableById.getIgnoreFlag(),ignoreFlag)){
            return 1;
        }
        AssetsDiscoveryColumnPageReqVO reqVO = new AssetsDiscoveryColumnPageReqVO();
        reqVO.setTableId(AssetsDiscoveryTableById.getId());
        List<AssetsDiscoveryColumnDO> AssetsDiscoveryColumnList = IAssetsDiscoveryColumnService.getDaDiscoveryColumnList(reqVO);

        AssetsAssetReqDTO assetsAssetReqDTO = BeanUtils.toBean(AssetsDiscoveryTableById, AssetsAssetReqDTO.class);
        //兼容表的备注为空时候，导致的资产地图为空
        assetsAssetReqDTO.setName(AssetsDiscoveryTable.getAssetName());
        assetsAssetReqDTO.setDatasourceId(AssetsDiscoveryTaskById.getDatasourceId());
        assetsAssetReqDTO.setCatCode(AssetsDiscoveryTable.getCatCode());
        List<String> themeIdList = new ArrayList<>();
        themeIdList.add(StringUtils.isEmpty(themeId) ? "1":themeId);
        assetsAssetReqDTO.setSource("1");
        if(StringUtils.equals("2",status)){
            List<AssetsAssetColumnReqDTO> columnReqDTOList = AssetsDiscoveryColumnList.stream()
                    .map(item -> BeanUtils.toBean(item, AssetsAssetColumnReqDTO.class))
                    .collect(Collectors.toList());
            assetsAssetApiOutService.insertAssetByDiscoveryInfo(assetsAssetReqDTO, columnReqDTOList, themeIdList);
        }else {
            assetsAssetApiOutService.updateAssetByDiscoveryInfo(assetsAssetReqDTO);
        }

        return this.updateDaDiscoveryTable(AssetsDiscoveryTable);
    }

    @Override
    public Integer updateByTaskIdListAndTableNameStatus(AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTable) {
        LambdaQueryWrapperX<AssetsDiscoveryTableDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.inIfPresent(AssetsDiscoveryTableDO::getTaskId,AssetsDiscoveryTable.getTaskIdList())
                .eqIfPresent(AssetsDiscoveryTableDO::getTableName,AssetsDiscoveryTable.getTableName());
        AssetsDiscoveryTableDO AssetsDiscoveryTableDO = BeanUtils.toBean(AssetsDiscoveryTable, AssetsDiscoveryTableDO.class);
        return AssetsDiscoveryTableMapper.update(AssetsDiscoveryTableDO,queryWrapperX);
    }
}
