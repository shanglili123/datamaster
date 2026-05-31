package com.datamaster.module.standards.service.model.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnPageReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelColumnDO;
import com.datamaster.module.standards.dal.mapper.model.StandardsModelColumnMapper;
import com.datamaster.module.standards.service.model.IStandardsModelColumnService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 逻辑模型属性信息Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsModelColumnServiceImpl extends ServiceImpl<StandardsModelColumnMapper, StandardsModelColumnDO>
        implements IStandardsModelColumnService {
    @Resource
    private StandardsModelColumnMapper StandardsModelColumnMapper;

    @Override
    public PageResult<StandardsModelColumnDO> getDpModelColumnPage(StandardsModelColumnPageReqVO pageReqVO) {
        return StandardsModelColumnMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDpModelColumn(StandardsModelColumnSaveReqVO createReqVO) {
        StandardsModelColumnDO dictType = BeanUtils.toBean(createReqVO, StandardsModelColumnDO.class);
        StandardsModelColumnMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpModelColumn(StandardsModelColumnSaveReqVO updateReqVO) {
        // 相关校验

        // 更新逻辑模型属性信息
        StandardsModelColumnDO updateObj = BeanUtils.toBean(updateReqVO, StandardsModelColumnDO.class);
        return StandardsModelColumnMapper.updateById(updateObj);
    }

    @Override
    public int removeDpModelColumn(Collection<Long> idList) {
        // 批量删除逻辑模型属性信息
        return StandardsModelColumnMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeDpModelColumnByModelId(Collection<Long> modelIdList) {
        return StandardsModelColumnMapper.delete(new LambdaQueryWrapperX<StandardsModelColumnDO>()
                .in(StandardsModelColumnDO::getModelId, modelIdList));
    }

    @Override
    public StandardsModelColumnDO getDpModelColumnById(Long id) {
        return StandardsModelColumnMapper.selectById(id);
    }

    @Override
    public List<StandardsModelColumnDO> getDpModelColumnList() {
        return StandardsModelColumnMapper.selectList();
    }

    @Override
    public List<StandardsModelColumnDO> getDpModelColumnList(StandardsModelColumnSaveReqVO reqVO) {
        StandardsModelColumnPageReqVO StandardsModelColumnPageReqVO = BeanUtils.toBean(reqVO, StandardsModelColumnPageReqVO.class);
        MPJLambdaWrapper<StandardsModelColumnDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(StandardsModelColumnDO.class)
                .select("t2.NAME AS dataElemName")
                .leftJoin("STD_DATA_ELEM t2 on t.DATA_ELEM_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .eq(StandardsModelColumnPageReqVO.getModelId() != null, StandardsModelColumnDO::getModelId,
                        StandardsModelColumnPageReqVO.getModelId())
                .like(StringUtils.isNotBlank(StandardsModelColumnPageReqVO.getEngName()), StandardsModelColumnDO::getEngName,
                        StandardsModelColumnPageReqVO.getEngName())
                .like(StringUtils.isNotBlank(StandardsModelColumnPageReqVO.getCnName()), StandardsModelColumnDO::getCnName,
                        StandardsModelColumnPageReqVO.getCnName())
                .eq(StringUtils.isNotBlank(StandardsModelColumnPageReqVO.getColumnType()), StandardsModelColumnDO::getColumnType,
                        StandardsModelColumnPageReqVO.getColumnType())
                .eq(StandardsModelColumnPageReqVO.getColumnLength() != null, StandardsModelColumnDO::getColumnLength,
                        StandardsModelColumnPageReqVO.getColumnLength())
                .eq(StandardsModelColumnPageReqVO.getColumnScale() != null, StandardsModelColumnDO::getColumnScale,
                        StandardsModelColumnPageReqVO.getColumnScale())
                .eq(StringUtils.isNotBlank(StandardsModelColumnPageReqVO.getDefaultValue()), StandardsModelColumnDO::getDefaultValue,
                        StandardsModelColumnPageReqVO.getDefaultValue())
                .eq(StandardsModelColumnPageReqVO.getPkFlag() != null, StandardsModelColumnDO::getPkFlag,
                        StandardsModelColumnPageReqVO.getPkFlag())
                .eq(StandardsModelColumnPageReqVO.getNullableFlag() != null, StandardsModelColumnDO::getNullableFlag,
                        StandardsModelColumnPageReqVO.getNullableFlag())
                .eq(StandardsModelColumnPageReqVO.getSortOrder() != null, StandardsModelColumnDO::getSortOrder,
                        StandardsModelColumnPageReqVO.getSortOrder())
                .eq(StringUtils.isNotBlank(StandardsModelColumnPageReqVO.getAuthorityDept()),
                        StandardsModelColumnDO::getAuthorityDept, StandardsModelColumnPageReqVO.getAuthorityDept())
                .eq(StandardsModelColumnPageReqVO.getDataElemId() != null, StandardsModelColumnDO::getDataElemId,
                        StandardsModelColumnPageReqVO.getDataElemId())
                .eq(StandardsModelColumnPageReqVO.getCreateTime() != null, StandardsModelColumnDO::getCreateTime,
                        StandardsModelColumnPageReqVO.getCreateTime());
        return StandardsModelColumnMapper.selectJoinList(StandardsModelColumnDO.class, wrapper);
    }

    @Override
    public long countByDpModelColumn(StandardsModelColumnSaveReqVO reqVO) {
        LambdaQueryWrapperX<StandardsModelColumnDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eqIfPresent(StandardsModelColumnDO::getModelId, reqVO.getModelId())
                .likeIfPresent(StandardsModelColumnDO::getEngName, reqVO.getEngName())
                .likeIfPresent(StandardsModelColumnDO::getCnName, reqVO.getCnName())
                .eqIfPresent(StandardsModelColumnDO::getColumnType, reqVO.getColumnType())
                .eqIfPresent(StandardsModelColumnDO::getColumnLength, reqVO.getColumnLength())
                .eqIfPresent(StandardsModelColumnDO::getColumnScale, reqVO.getColumnScale())
                .eqIfPresent(StandardsModelColumnDO::getDefaultValue, reqVO.getDefaultValue())
                .eqIfPresent(StandardsModelColumnDO::getPkFlag, reqVO.getPkFlag())
                .eqIfPresent(StandardsModelColumnDO::getNullableFlag, reqVO.getNullableFlag())
                .eqIfPresent(StandardsModelColumnDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(StandardsModelColumnDO::getAuthorityDept, reqVO.getAuthorityDept())
                .eqIfPresent(StandardsModelColumnDO::getDataElemId, reqVO.getDataElemId())
                .eqIfPresent(StandardsModelColumnDO::getCreateTime, reqVO.getCreateTime());
        return StandardsModelColumnMapper.selectCount(queryWrapper);
    }

    @Override
    public Map<Long, StandardsModelColumnDO> getDpModelColumnMap() {
        List<StandardsModelColumnDO> StandardsModelColumnList = StandardsModelColumnMapper.selectList();
        return StandardsModelColumnList.stream()
                .collect(Collectors.toMap(
                        StandardsModelColumnDO::getId,
                        StandardsModelColumnDO -> StandardsModelColumnDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing));
    }

    /**
     * 导入逻辑模型属性信息数据
     *
     * @param importExcelList 逻辑模型属性信息数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importDpModelColumn(List<StandardsModelColumnRespVO> importExcelList, boolean isUpdateSupport,
            String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (StandardsModelColumnRespVO respVO : importExcelList) {
            try {
                StandardsModelColumnDO StandardsModelColumnDO = BeanUtils.toBean(respVO, StandardsModelColumnDO.class);
                Long StandardsModelColumnId = respVO.getId();
                if (isUpdateSupport) {
                    if (StandardsModelColumnId != null) {
                        StandardsModelColumnDO existingDpModelColumn = StandardsModelColumnMapper.selectById(StandardsModelColumnId);
                        if (existingDpModelColumn != null) {
                            StandardsModelColumnMapper.updateById(StandardsModelColumnDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + StandardsModelColumnId + " 的逻辑模型属性信息记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + StandardsModelColumnId + " 的逻辑模型属性信息记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<StandardsModelColumnDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", StandardsModelColumnId);
                    StandardsModelColumnDO existingDpModelColumn = StandardsModelColumnMapper.selectOne(queryWrapper);
                    if (existingDpModelColumn == null) {
                        StandardsModelColumnMapper.insert(StandardsModelColumnDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + StandardsModelColumnId + " 的逻辑模型属性信息记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + StandardsModelColumnId + " 的逻辑模型属性信息记录已存在。");
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

    /**
     * 批量插入逻辑模型属性信息数据
     *
     * @param StandardsModelColumnList 逻辑模型属性信息数据列表
     * @return 结果
     */
    @Override
    public Boolean createDpModelColumnList(List<StandardsModelColumnSaveReqVO> StandardsModelColumnList) {
        List<StandardsModelColumnDO> StandardsModelColumnDOList = BeanUtils.toBean(StandardsModelColumnList, StandardsModelColumnDO.class);
        for (StandardsModelColumnDO StandardsModelColumnDO : StandardsModelColumnDOList) {
            StandardsModelColumnMapper.insert(StandardsModelColumnDO);
        }
        // Boolean aBoolean = StandardsModelColumnMapper.insertBatch(StandardsModelColumnDOList);
        return true;
    }

    /**
     * 批量修改和插入逻辑模型属性信息数据
     *
     * @param StandardsModelColumnList 逻辑模型属性信息数据列表
     * @return 结果
     */
    @Override
    public Boolean updateDpModelColumnList(List<StandardsModelColumnSaveReqVO> StandardsModelColumnList) {
        List<StandardsModelColumnDO> StandardsModelColumnDOList = BeanUtils.toBean(StandardsModelColumnList, StandardsModelColumnDO.class);
        Long modelId = StandardsModelColumnDOList.get(0) == null ? null : StandardsModelColumnDOList.get(0).getModelId();
        StandardsModelColumnSaveReqVO StandardsModelColumnSaveReqVO = new StandardsModelColumnSaveReqVO();
        StandardsModelColumnSaveReqVO.setModelId(modelId);
        List<StandardsModelColumnDO> modelColumnList = this.getDpModelColumnList(StandardsModelColumnSaveReqVO);
        // 用于存储StandardsModelColumnDOList中的所有ID
        Set<Long> newIds = new HashSet<>();
        for (StandardsModelColumnDO StandardsModelColumnDO : StandardsModelColumnDOList) {
            if (StandardsModelColumnDO.getId() != null) {
                StandardsModelColumnMapper.updateById(StandardsModelColumnDO);
                newIds.add(StandardsModelColumnDO.getId());
            } else {
                StandardsModelColumnMapper.insert(StandardsModelColumnDO);
            }
        }
        // 删除modelColumnList中存在但StandardsModelColumnDOList中不存在的记录
        for (StandardsModelColumnDO existingColumn : modelColumnList) {
            if (!newIds.contains(existingColumn.getId())) {
                StandardsModelColumnMapper.deleteById(existingColumn.getId());
            }
        }
        return true;
    }
}
