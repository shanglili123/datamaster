package com.datamaster.module.standards.service.dataElem.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemRuleRelRespDTO;
import com.datamaster.module.standards.api.service.dataElem.IDataElemRuleRelService;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemRuleRelDO;
import com.datamaster.module.standards.dal.mapper.dataElem.StandardsDataElemRuleRelMapper;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemRuleRelService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据元数据规则关联信息Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDataElemRuleRelServiceImpl extends ServiceImpl<StandardsDataElemRuleRelMapper, StandardsDataElemRuleRelDO> implements IStandardsDataElemRuleRelService, IDataElemRuleRelService {
    @Resource
    private StandardsDataElemRuleRelMapper StandardsDataElemRuleRelMapper;

    @Override
    public PageResult<StandardsDataElemRuleRelDO> getDpDataElemRuleRelPage(StandardsDataElemRuleRelPageReqVO pageReqVO) {
        return StandardsDataElemRuleRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDpDataElemRuleRel(StandardsDataElemRuleRelSaveReqVO createReqVO) {
        if ("1".equals(createReqVO.getType())) {
            Assert.notNull(createReqVO.getRuleType(), "ruleType null");
        }
        if (createReqVO.getStatus() == null) {
            createReqVO.setStatus("1");
        }
        StandardsDataElemRuleRelDO dictType = BeanUtils.toBean(createReqVO, StandardsDataElemRuleRelDO.class);
        StandardsDataElemRuleRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpDataElemRuleRel(StandardsDataElemRuleRelSaveReqVO updateReqVO) {
        if ("1".equals(updateReqVO.getType())) {
            Assert.notNull(updateReqVO.getRuleType(), "ruleType null");
        }
        if (updateReqVO.getStatus() == null) {
            updateReqVO.setStatus("1");
        }
        // 更新数据元数据规则关联信息
        StandardsDataElemRuleRelDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDataElemRuleRelDO.class);
        return StandardsDataElemRuleRelMapper.updateById(updateObj);
    }

    @Override
    public int removeDpDataElemRuleRel(Collection<Long> idList) {
        // 批量删除数据元数据规则关联信息
        return StandardsDataElemRuleRelMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDataElemRuleRelDO getDpDataElemRuleRelById(Long id) {
        return StandardsDataElemRuleRelMapper.selectById(id);
    }

    @Override
    public List<StandardsDataElemRuleRelDO> getDpDataElemRuleRelList() {
        return StandardsDataElemRuleRelMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDataElemRuleRelDO> getDpDataElemRuleRelMap() {
        List<StandardsDataElemRuleRelDO> StandardsDataElemRuleRelList = StandardsDataElemRuleRelMapper.selectList();
        return StandardsDataElemRuleRelList.stream()
                .collect(Collectors.toMap(
                        StandardsDataElemRuleRelDO::getId,
                        StandardsDataElemRuleRelDO -> StandardsDataElemRuleRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据元数据规则关联信息数据
     *
     * @param importExcelList 数据元数据规则关联信息数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importDpDataElemRuleRel(List<StandardsDataElemRuleRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (StandardsDataElemRuleRelRespVO respVO : importExcelList) {
            try {
                StandardsDataElemRuleRelDO StandardsDataElemRuleRelDO = BeanUtils.toBean(respVO, StandardsDataElemRuleRelDO.class);
                Long StandardsDataElemRuleRelId = respVO.getId();
                if (isUpdateSupport) {
                    if (StandardsDataElemRuleRelId != null) {
                        StandardsDataElemRuleRelDO existingDpDataElemRuleRel = StandardsDataElemRuleRelMapper.selectById(StandardsDataElemRuleRelId);
                        if (existingDpDataElemRuleRel != null) {
                            StandardsDataElemRuleRelMapper.updateById(StandardsDataElemRuleRelDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + StandardsDataElemRuleRelId + " 的数据元数据规则关联信息记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + StandardsDataElemRuleRelId + " 的数据元数据规则关联信息记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<StandardsDataElemRuleRelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", StandardsDataElemRuleRelId);
                    StandardsDataElemRuleRelDO existingDpDataElemRuleRel = StandardsDataElemRuleRelMapper.selectOne(queryWrapper);
                    if (existingDpDataElemRuleRel == null) {
                        StandardsDataElemRuleRelMapper.insert(StandardsDataElemRuleRelDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + StandardsDataElemRuleRelId + " 的数据元数据规则关联信息记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + StandardsDataElemRuleRelId + " 的数据元数据规则关联信息记录已存在。");
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
    public List<StandardsDataElemRuleRelRespDTO> listByDataElemIdList(Collection<Long> dataElemIdList, String type) {
        List<StandardsDataElemRuleRelDO> StandardsDataElemRuleRelDOS = baseMapper.listByDataElemIdList(dataElemIdList, type);
        return BeanUtils.toBean(StandardsDataElemRuleRelDOS, StandardsDataElemRuleRelRespDTO.class);
    }

}
