package com.datamaster.module.assets.service.discovery.impl;

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
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryColumnDO;
import com.datamaster.module.assets.dal.mapper.discovery.AssetsDiscoveryColumnMapper;
import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryColumnService;

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
public class AssetsDiscoveryColumnServiceImpl  extends ServiceImpl<AssetsDiscoveryColumnMapper,AssetsDiscoveryColumnDO> implements IAssetsDiscoveryColumnService {
    @Resource
    private AssetsDiscoveryColumnMapper AssetsDiscoveryColumnMapper;

    @Override
    public PageResult<AssetsDiscoveryColumnDO> getDaDiscoveryColumnPage(AssetsDiscoveryColumnPageReqVO pageReqVO) {
        return AssetsDiscoveryColumnMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AssetsDiscoveryColumnDO> getDaDiscoveryColumnList(AssetsDiscoveryColumnPageReqVO reqVO) {
        MPJLambdaWrapper<AssetsDiscoveryColumnDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(AssetsDiscoveryColumnDO.class)
                .eq(reqVO.getTaskId() != null, AssetsDiscoveryColumnDO::getTaskId, reqVO.getTaskId())
                .eq( reqVO.getTableId() != null, AssetsDiscoveryColumnDO::getTableId, reqVO.getTableId())
                .like(StringUtils.isNotBlank(reqVO.getColumnName()), AssetsDiscoveryColumnDO::getColumnName, reqVO.getColumnName())
                .eq(StringUtils.isNotBlank(reqVO.getColumnComment()), AssetsDiscoveryColumnDO::getColumnComment, reqVO.getColumnComment())
                .eq(StringUtils.isNotBlank(reqVO.getColumnType()), AssetsDiscoveryColumnDO::getColumnType, reqVO.getColumnType())
                .eq(StringUtils.isNotBlank(reqVO.getNullableFlag()), AssetsDiscoveryColumnDO::getNullableFlag, reqVO.getNullableFlag())
                .eq(StringUtils.isNotBlank(reqVO.getPkFlag()), AssetsDiscoveryColumnDO::getPkFlag, reqVO.getPkFlag())
                .eq(StringUtils.isNotBlank(reqVO.getDefaultValue()), AssetsDiscoveryColumnDO::getDefaultValue, reqVO.getDefaultValue());

        return AssetsDiscoveryColumnMapper.selectList(wrapper);
    }

    @Override
    public Long createDaDiscoveryColumn(AssetsDiscoveryColumnSaveReqVO createReqVO) {
        AssetsDiscoveryColumnDO dictType = BeanUtils.toBean(createReqVO, AssetsDiscoveryColumnDO.class);
        AssetsDiscoveryColumnMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public Long createDaDiscoveryColumn(AssetsDiscoveryColumnDO createReqVO) {
        AssetsDiscoveryColumnMapper.insert(createReqVO);
        return createReqVO.getId();
    }

    @Override
    public int updateDaDiscoveryColumn(AssetsDiscoveryColumnSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据发现字段
        AssetsDiscoveryColumnDO updateObj = BeanUtils.toBean(updateReqVO, AssetsDiscoveryColumnDO.class);
        return AssetsDiscoveryColumnMapper.updateById(updateObj);
    }

    @Override
    public int updateDaDiscoveryColumn(AssetsDiscoveryColumnDO updateReqVO) {
        // 相关校验

        // 更新数据发现字段
        return AssetsDiscoveryColumnMapper.updateById(updateReqVO);
    }
    @Override
    public int removeDaDiscoveryColumn(Collection<Long> idList) {
        // 批量删除数据发现字段
        return AssetsDiscoveryColumnMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsDiscoveryColumnDO getDaDiscoveryColumnById(Long id) {
        return AssetsDiscoveryColumnMapper.selectById(id);
    }

    @Override
    public List<AssetsDiscoveryColumnDO> getDaDiscoveryColumnList() {
        return AssetsDiscoveryColumnMapper.selectList();
    }

    @Override
    public Map<Long, AssetsDiscoveryColumnDO> getDaDiscoveryColumnMap() {
        List<AssetsDiscoveryColumnDO> AssetsDiscoveryColumnList = AssetsDiscoveryColumnMapper.selectList();
        return AssetsDiscoveryColumnList.stream()
                .collect(Collectors.toMap(
                        AssetsDiscoveryColumnDO::getId,
                        AssetsDiscoveryColumnDO -> AssetsDiscoveryColumnDO,
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
        public String importDaDiscoveryColumn(List<AssetsDiscoveryColumnRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (AssetsDiscoveryColumnRespVO respVO : importExcelList) {
                try {
                    AssetsDiscoveryColumnDO AssetsDiscoveryColumnDO = BeanUtils.toBean(respVO, AssetsDiscoveryColumnDO.class);
                    Long AssetsDiscoveryColumnId = respVO.getId();
                    if (isUpdateSupport) {
                        if (AssetsDiscoveryColumnId != null) {
                            AssetsDiscoveryColumnDO existingDaDiscoveryColumn = AssetsDiscoveryColumnMapper.selectById(AssetsDiscoveryColumnId);
                            if (existingDaDiscoveryColumn != null) {
                                AssetsDiscoveryColumnMapper.updateById(AssetsDiscoveryColumnDO);
                                successNum++;
                                successMessages.add("ID " + AssetsDiscoveryColumnId + " ");
                            } else {
                                failureNum++;
                                failureMessages.add("ID " + AssetsDiscoveryColumnId + " ");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("ID");
                        }
                    } else {
                        QueryWrapper<AssetsDiscoveryColumnDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", AssetsDiscoveryColumnId);
                        AssetsDiscoveryColumnDO existingDaDiscoveryColumn = AssetsDiscoveryColumnMapper.selectOne(queryWrapper);
                        if (existingDaDiscoveryColumn == null) {
                            AssetsDiscoveryColumnMapper.insert(AssetsDiscoveryColumnDO);
                            successNum++;
                            successMessages.add("ID " + AssetsDiscoveryColumnId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsDiscoveryColumnId + " ");
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
