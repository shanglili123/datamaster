package com.datamaster.module.assets.service.assetchild.projectRel.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.projectRel.AssetsAssetProjectRelDO;
import com.datamaster.module.assets.dal.mapper.assetchild.projectRel.AssetsAssetProjectRelMapper;
import com.datamaster.module.assets.service.assetchild.projectRel.IAssetsAssetProjectRelService;

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
 * @date 2025-04-18
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetProjectRelServiceImpl  extends ServiceImpl<AssetsAssetProjectRelMapper,AssetsAssetProjectRelDO> implements IAssetsAssetProjectRelService {
    @Resource
    private AssetsAssetProjectRelMapper AssetsAssetProjectRelMapper;

    @Override
    public PageResult<AssetsAssetProjectRelDO> getDaAssetProjectRelPage(AssetsAssetProjectRelPageReqVO pageReqVO) {
        return AssetsAssetProjectRelMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AssetsAssetProjectRelDO> getDaAssetProjectRelList(AssetsAssetProjectRelPageReqVO pageReqVO) {
        return null;
    }

    @Override
    public Long createDaAssetProjectRel(AssetsAssetProjectRelSaveReqVO createReqVO) {
        this.removeProjectRelByAssetId(createReqVO.getAssetId());
        AssetsAssetProjectRelDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetProjectRelDO.class);
        AssetsAssetProjectRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int removeProjectRelByAssetId(Long assetId) {
        AssetsAssetProjectRelMapper.removeProjectRelByAssetId(assetId);
        return 1;
    }

    @Override
    public int updateDaAssetProjectRel(AssetsAssetProjectRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产与项目关联关系
        AssetsAssetProjectRelDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetProjectRelDO.class);
        return AssetsAssetProjectRelMapper.updateById(updateObj);
    }
    @Override
    public int removeDaAssetProjectRel(Collection<Long> idList) {
        // 批量删除数据资产与项目关联关系
        return AssetsAssetProjectRelMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetProjectRelDO getDaAssetProjectRelById(Long id) {
        return AssetsAssetProjectRelMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetProjectRelDO> getDaAssetProjectRelList() {
        return AssetsAssetProjectRelMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetProjectRelDO> getDaAssetProjectRelMap() {
        List<AssetsAssetProjectRelDO> AssetsAssetProjectRelList = AssetsAssetProjectRelMapper.selectList();
        return AssetsAssetProjectRelList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetProjectRelDO::getId,
                        AssetsAssetProjectRelDO -> AssetsAssetProjectRelDO,
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
    public String importDaAssetProjectRel(List<AssetsAssetProjectRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetProjectRelRespVO respVO : importExcelList) {
            try {
                AssetsAssetProjectRelDO AssetsAssetProjectRelDO = BeanUtils.toBean(respVO, AssetsAssetProjectRelDO.class);
                Long AssetsAssetProjectRelId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetProjectRelId != null) {
                        AssetsAssetProjectRelDO existingDaAssetProjectRel = AssetsAssetProjectRelMapper.selectById(AssetsAssetProjectRelId);
                        if (existingDaAssetProjectRel != null) {
                            AssetsAssetProjectRelMapper.updateById(AssetsAssetProjectRelDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetProjectRelId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetProjectRelId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetProjectRelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetProjectRelId);
                    AssetsAssetProjectRelDO existingDaAssetProjectRel = AssetsAssetProjectRelMapper.selectOne(queryWrapper);
                    if (existingDaAssetProjectRel == null) {
                        AssetsAssetProjectRelMapper.insert(AssetsAssetProjectRelDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetProjectRelId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetProjectRelId + " ");
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
