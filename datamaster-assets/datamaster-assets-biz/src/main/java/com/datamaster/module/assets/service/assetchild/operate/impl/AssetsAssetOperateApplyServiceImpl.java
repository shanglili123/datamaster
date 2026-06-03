package com.datamaster.module.assets.service.assetchild.operate.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplyPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplyRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplySaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateApplyDO;
import com.datamaster.module.assets.dal.mapper.assetchild.operate.AssetsAssetOperateApplyMapper;
import com.datamaster.module.assets.service.assetchild.operate.IAssetsAssetOperateApplyService;

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
public class AssetsAssetOperateApplyServiceImpl  extends ServiceImpl<AssetsAssetOperateApplyMapper,AssetsAssetOperateApplyDO> implements IAssetsAssetOperateApplyService {
    @Resource
    private AssetsAssetOperateApplyMapper AssetsAssetOperateApplyMapper;

    @Override
    public PageResult<AssetsAssetOperateApplyDO> getAssetOperateApplyPage(AssetsAssetOperateApplyPageReqVO pageReqVO) {
        return AssetsAssetOperateApplyMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAssetOperateApply(AssetsAssetOperateApplySaveReqVO createReqVO) {
        AssetsAssetOperateApplyDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetOperateApplyDO.class);
        AssetsAssetOperateApplyMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAssetOperateApply(AssetsAssetOperateApplySaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产操作申请
        AssetsAssetOperateApplyDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetOperateApplyDO.class);
        return AssetsAssetOperateApplyMapper.updateById(updateObj);
    }
    @Override
    public int removeAssetOperateApply(Collection<Long> idList) {
        // 批量删除数据资产操作申请
        return AssetsAssetOperateApplyMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetOperateApplyDO getAssetOperateApplyById(Long id) {
        return AssetsAssetOperateApplyMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetOperateApplyDO> getAssetOperateApplyList() {
        return AssetsAssetOperateApplyMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetOperateApplyDO> getAssetOperateApplyMap() {
        List<AssetsAssetOperateApplyDO> AssetsAssetOperateApplyList = AssetsAssetOperateApplyMapper.selectList();
        return AssetsAssetOperateApplyList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetOperateApplyDO::getId,
                        AssetsAssetOperateApplyDO -> AssetsAssetOperateApplyDO,
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
        public String importAssetOperateApply(List<AssetsAssetOperateApplyRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (AssetsAssetOperateApplyRespVO respVO : importExcelList) {
                try {
                    AssetsAssetOperateApplyDO AssetsAssetOperateApplyDO = BeanUtils.toBean(respVO, AssetsAssetOperateApplyDO.class);
                    Long AssetsAssetOperateApplyId = respVO.getId();
                    if (isUpdateSupport) {
                        if (AssetsAssetOperateApplyId != null) {
                            AssetsAssetOperateApplyDO existingAssetOperateApply = AssetsAssetOperateApplyMapper.selectById(AssetsAssetOperateApplyId);
                            if (existingAssetOperateApply != null) {
                                AssetsAssetOperateApplyMapper.updateById(AssetsAssetOperateApplyDO);
                                successNum++;
                                successMessages.add("ID " + AssetsAssetOperateApplyId + " ");
                            } else {
                                failureNum++;
                                failureMessages.add("ID " + AssetsAssetOperateApplyId + " ");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("ID");
                        }
                    } else {
                        QueryWrapper<AssetsAssetOperateApplyDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", AssetsAssetOperateApplyId);
                        AssetsAssetOperateApplyDO existingAssetOperateApply = AssetsAssetOperateApplyMapper.selectOne(queryWrapper);
                        if (existingAssetOperateApply == null) {
                            AssetsAssetOperateApplyMapper.insert(AssetsAssetOperateApplyDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetOperateApplyId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetOperateApplyId + " ");
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
