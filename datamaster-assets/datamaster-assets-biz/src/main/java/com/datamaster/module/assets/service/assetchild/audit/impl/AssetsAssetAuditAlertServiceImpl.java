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
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditAlertDO;
import com.datamaster.module.assets.dal.mapper.assetchild.audit.AssetsAssetAuditAlertMapper;
import com.datamaster.module.assets.service.assetchild.audit.IAssetsAssetAuditAlertService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetAuditAlertServiceImpl  extends ServiceImpl<AssetsAssetAuditAlertMapper,AssetsAssetAuditAlertDO> implements IAssetsAssetAuditAlertService {
    @Resource
    private AssetsAssetAuditAlertMapper AssetsAssetAuditAlertMapper;

    @Override
    public PageResult<AssetsAssetAuditAlertDO> getDaAssetAuditAlertPage(AssetsAssetAuditAlertPageReqVO pageReqVO) {
        return AssetsAssetAuditAlertMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDaAssetAuditAlert(AssetsAssetAuditAlertSaveReqVO createReqVO) {
        AssetsAssetAuditAlertDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetAuditAlertDO.class);
        AssetsAssetAuditAlertMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDaAssetAuditAlert(AssetsAssetAuditAlertSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产-质量预警
        AssetsAssetAuditAlertDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetAuditAlertDO.class);
        return AssetsAssetAuditAlertMapper.updateById(updateObj);
    }
    @Override
    public int removeDaAssetAuditAlert(Collection<Long> idList) {
        // 批量删除数据资产-质量预警
        return AssetsAssetAuditAlertMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetAuditAlertDO getDaAssetAuditAlertById(Long id) {
        return AssetsAssetAuditAlertMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetAuditAlertDO> getDaAssetAuditAlertList() {
        return AssetsAssetAuditAlertMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetAuditAlertDO> getDaAssetAuditAlertMap() {
        List<AssetsAssetAuditAlertDO> AssetsAssetAuditAlertList = AssetsAssetAuditAlertMapper.selectList();
        return AssetsAssetAuditAlertList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetAuditAlertDO::getId,
                        AssetsAssetAuditAlertDO -> AssetsAssetAuditAlertDO,
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
    public String importDaAssetAuditAlert(List<AssetsAssetAuditAlertRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetAuditAlertRespVO respVO : importExcelList) {
            try {
                AssetsAssetAuditAlertDO AssetsAssetAuditAlertDO = BeanUtils.toBean(respVO, AssetsAssetAuditAlertDO.class);
                Long AssetsAssetAuditAlertId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetAuditAlertId != null) {
                        AssetsAssetAuditAlertDO existingDaAssetAuditAlert = AssetsAssetAuditAlertMapper.selectById(AssetsAssetAuditAlertId);
                        if (existingDaAssetAuditAlert != null) {
                            AssetsAssetAuditAlertMapper.updateById(AssetsAssetAuditAlertDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetAuditAlertId + " -");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetAuditAlertId + " -");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetAuditAlertDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetAuditAlertId);
                    AssetsAssetAuditAlertDO existingDaAssetAuditAlert = AssetsAssetAuditAlertMapper.selectOne(queryWrapper);
                    if (existingDaAssetAuditAlert == null) {
                        AssetsAssetAuditAlertMapper.insert(AssetsAssetAuditAlertDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetAuditAlertId + " -");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetAuditAlertId + " -");
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
