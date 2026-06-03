package com.datamaster.module.assets.service.assetchild.audit.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditSchedulePageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditScheduleRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditScheduleSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditScheduleDO;
import com.datamaster.module.assets.dal.mapper.assetchild.audit.AssetsAssetAuditScheduleMapper;
import com.datamaster.module.assets.service.assetchild.audit.IAssetsAssetAuditScheduleService;

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
 * @date 2025-05-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetAuditScheduleServiceImpl  extends ServiceImpl<AssetsAssetAuditScheduleMapper,AssetsAssetAuditScheduleDO> implements IAssetsAssetAuditScheduleService {
    @Resource
    private AssetsAssetAuditScheduleMapper AssetsAssetAuditScheduleMapper;

    @Override
    public PageResult<AssetsAssetAuditScheduleDO> getAssetAuditSchedulePage(AssetsAssetAuditSchedulePageReqVO pageReqVO) {
        return AssetsAssetAuditScheduleMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAssetAuditSchedule(AssetsAssetAuditScheduleSaveReqVO createReqVO) {
        AssetsAssetAuditScheduleDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetAuditScheduleDO.class);
        AssetsAssetAuditScheduleMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAssetAuditSchedule(AssetsAssetAuditScheduleSaveReqVO updateReqVO) {
        // 相关校验

        // 更新资产稽查调度
        AssetsAssetAuditScheduleDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetAuditScheduleDO.class);
        return AssetsAssetAuditScheduleMapper.updateById(updateObj);
    }
    @Override
    public int removeAssetAuditSchedule(Collection<Long> idList) {
        // 批量删除资产稽查调度
        return AssetsAssetAuditScheduleMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetAuditScheduleDO getAssetAuditScheduleById(Long id) {
        return AssetsAssetAuditScheduleMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetAuditScheduleDO> getAssetAuditScheduleList() {
        return AssetsAssetAuditScheduleMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetAuditScheduleDO> getAssetAuditScheduleMap() {
        List<AssetsAssetAuditScheduleDO> AssetsAssetAuditScheduleList = AssetsAssetAuditScheduleMapper.selectList();
        return AssetsAssetAuditScheduleList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetAuditScheduleDO::getId,
                        AssetsAssetAuditScheduleDO -> AssetsAssetAuditScheduleDO,
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
    public String importAssetAuditSchedule(List<AssetsAssetAuditScheduleRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetAuditScheduleRespVO respVO : importExcelList) {
            try {
                AssetsAssetAuditScheduleDO AssetsAssetAuditScheduleDO = BeanUtils.toBean(respVO, AssetsAssetAuditScheduleDO.class);
                Long AssetsAssetAuditScheduleId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetAuditScheduleId != null) {
                        AssetsAssetAuditScheduleDO existingAssetAuditSchedule = AssetsAssetAuditScheduleMapper.selectById(AssetsAssetAuditScheduleId);
                        if (existingAssetAuditSchedule != null) {
                            AssetsAssetAuditScheduleMapper.updateById(AssetsAssetAuditScheduleDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetAuditScheduleId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetAuditScheduleId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetAuditScheduleDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetAuditScheduleId);
                    AssetsAssetAuditScheduleDO existingAssetAuditSchedule = AssetsAssetAuditScheduleMapper.selectOne(queryWrapper);
                    if (existingAssetAuditSchedule == null) {
                        AssetsAssetAuditScheduleMapper.insert(AssetsAssetAuditScheduleDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetAuditScheduleId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetAuditScheduleId + " ");
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
