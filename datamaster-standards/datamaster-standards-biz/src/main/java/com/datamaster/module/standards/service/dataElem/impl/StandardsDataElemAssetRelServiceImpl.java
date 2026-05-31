package com.datamaster.module.standards.service.dataElem.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemAssetRelDO;
import com.datamaster.module.standards.dal.mapper.dataElem.StandardsDataElemAssetRelMapper;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemAssetRelService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据元数据资产关联信息Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDataElemAssetRelServiceImpl  extends ServiceImpl<StandardsDataElemAssetRelMapper, StandardsDataElemAssetRelDO> implements IStandardsDataElemAssetRelService {
    @Resource
    private StandardsDataElemAssetRelMapper StandardsDataElemAssetRelMapper;

    @Override
    public PageResult<StandardsDataElemAssetRelDO> getDpDataElemAssetRelPage(StandardsDataElemAssetRelPageReqVO pageReqVO) {
        return StandardsDataElemAssetRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDpDataElemAssetRel(StandardsDataElemAssetRelSaveReqVO createReqVO) {
        StandardsDataElemAssetRelDO dictType = BeanUtils.toBean(createReqVO, StandardsDataElemAssetRelDO.class);
        StandardsDataElemAssetRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpDataElemAssetRel(StandardsDataElemAssetRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据元数据资产关联信息
        StandardsDataElemAssetRelDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDataElemAssetRelDO.class);
        return StandardsDataElemAssetRelMapper.updateById(updateObj);
    }
    @Override
    public int removeDpDataElemAssetRel(Collection<Long> idList) {
        // 批量删除数据元数据资产关联信息
        return StandardsDataElemAssetRelMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDataElemAssetRelDO getDpDataElemAssetRelById(Long id) {
        return StandardsDataElemAssetRelMapper.selectById(id);
    }

    @Override
    public List<StandardsDataElemAssetRelDO> getDpDataElemAssetRelList() {
        return StandardsDataElemAssetRelMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDataElemAssetRelDO> getDpDataElemAssetRelMap() {
        List<StandardsDataElemAssetRelDO> StandardsDataElemAssetRelList = StandardsDataElemAssetRelMapper.selectList();
        return StandardsDataElemAssetRelList.stream()
                .collect(Collectors.toMap(
                        StandardsDataElemAssetRelDO::getId,
                        StandardsDataElemAssetRelDO -> StandardsDataElemAssetRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据元数据资产关联信息数据
         *
         * @param importExcelList 数据元数据资产关联信息数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDpDataElemAssetRel(List<StandardsDataElemAssetRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsDataElemAssetRelRespVO respVO : importExcelList) {
                try {
                    StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO = BeanUtils.toBean(respVO, StandardsDataElemAssetRelDO.class);
                    Long StandardsDataElemAssetRelId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsDataElemAssetRelId != null) {
                            StandardsDataElemAssetRelDO existingDpDataElemAssetRel = StandardsDataElemAssetRelMapper.selectById(StandardsDataElemAssetRelId);
                            if (existingDpDataElemAssetRel != null) {
                                StandardsDataElemAssetRelMapper.updateById(StandardsDataElemAssetRelDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsDataElemAssetRelId + " 的数据元数据资产关联信息记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsDataElemAssetRelId + " 的数据元数据资产关联信息记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsDataElemAssetRelDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsDataElemAssetRelId);
                        StandardsDataElemAssetRelDO existingDpDataElemAssetRel = StandardsDataElemAssetRelMapper.selectOne(queryWrapper);
                        if (existingDpDataElemAssetRel == null) {
                            StandardsDataElemAssetRelMapper.insert(StandardsDataElemAssetRelDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsDataElemAssetRelId + " 的数据元数据资产关联信息记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsDataElemAssetRelId + " 的数据元数据资产关联信息记录已存在。");
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
}
