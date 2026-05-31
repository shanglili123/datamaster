package com.datamaster.module.assets.service.discovery.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.*;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryColumnDO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTableDO;
import com.datamaster.module.assets.dal.mapper.discovery.AssetsDiscoveryTableMapper;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryColumnService;
import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryTableService;
import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryTaskService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    private IAssetsAssetService IAssetsAssetService;
    @Resource
    @Lazy
    private IAssetsDiscoveryTaskService IAssetsDiscoveryTaskService;

    @Override
    public PageResult<AssetsDiscoveryTableDO> getDaDiscoveryTablePage(AssetsDiscoveryTablePageReqVO pageReqVO) {
        return AssetsDiscoveryTableMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AssetsDiscoveryTableDO> getDaDiscoveryTableList(AssetsDiscoveryTablePageReqVO reqVO) {

        MPJLambdaWrapper<AssetsDiscoveryTableDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(AssetsDiscoveryTableDO.class)
                .eq(reqVO.getTaskId() != null, AssetsDiscoveryTableDO::getTaskId, reqVO.getTaskId())
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

        return AssetsDiscoveryTableMapper.selectList(wrapper);
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

        AssetsAssetPageReqVO AssetsAssetPageReqVO = new AssetsAssetPageReqVO(AssetsDiscoveryTableById);
        //兼容表的备注为空时候，导致的资产地图为空
        AssetsAssetPageReqVO.setName(AssetsDiscoveryTable.getAssetName());
        AssetsAssetPageReqVO.setDatasourceId(String.valueOf(AssetsDiscoveryTaskById.getDatasourceId()));
        AssetsAssetPageReqVO.setCatCode(AssetsDiscoveryTable.getCatCode());
        List<String> themeIdList = new ArrayList<>();
        themeIdList.add(StringUtils.isEmpty(themeId) ? "1":themeId);
        AssetsAssetPageReqVO.setThemeIdList(themeIdList);
        AssetsAssetPageReqVO.setSource("1");
        if(StringUtils.equals("2",status)){
            List<AssetsAssetColumnSaveReqVO> columnSaveReqVOList = AssetsDiscoveryColumnList.stream().map(itam -> new AssetsAssetColumnSaveReqVO(itam)).collect(Collectors.toList());
            IAssetsAssetService.insertAssetByDiscoveryInfo(AssetsAssetPageReqVO,columnSaveReqVOList);
        }else {
            IAssetsAssetService.updateAssetByDiscoveryInfo(AssetsAssetPageReqVO);
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
