package com.datamaster.module.standards.service.dataElem.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodePageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodeRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodeSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemCodeDO;
import com.datamaster.module.standards.dal.mapper.dataElem.StandardsDataElemCodeMapper;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemCodeService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据元代码Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDataElemCodeServiceImpl extends ServiceImpl<StandardsDataElemCodeMapper, StandardsDataElemCodeDO> implements IStandardsDataElemCodeService {
    @Resource
    private StandardsDataElemCodeMapper StandardsDataElemCodeMapper;

    @Override
    public PageResult<StandardsDataElemCodeDO> getDpDataElemCodePage(StandardsDataElemCodePageReqVO pageReqVO) {
        return StandardsDataElemCodeMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDpDataElemCode(StandardsDataElemCodeSaveReqVO createReqVO) {
        StandardsDataElemCodeDO dictType = BeanUtils.toBean(createReqVO, StandardsDataElemCodeDO.class);
        StandardsDataElemCodeMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpDataElemCode(StandardsDataElemCodeSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据元代码
        StandardsDataElemCodeDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDataElemCodeDO.class);
        return StandardsDataElemCodeMapper.updateById(updateObj);
    }

    @Override
    public int removeDpDataElemCode(Collection<Long> idList) {
        // 批量删除数据元代码
        return StandardsDataElemCodeMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDataElemCodeDO getDpDataElemCodeById(Long id) {
        return StandardsDataElemCodeMapper.selectById(id);
    }

    @Override
    public List<StandardsDataElemCodeDO> getDpDataElemCodeList() {
        return StandardsDataElemCodeMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDataElemCodeDO> getDpDataElemCodeMap() {
        List<StandardsDataElemCodeDO> StandardsDataElemCodeList = StandardsDataElemCodeMapper.selectList();
        return StandardsDataElemCodeList.stream()
                .collect(Collectors.toMap(
                        StandardsDataElemCodeDO::getId,
                        StandardsDataElemCodeDO -> StandardsDataElemCodeDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据元代码数据
     *
     * @param importExcelList 数据元代码数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importDpDataElemCode(List<StandardsDataElemCodeRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (StandardsDataElemCodeRespVO respVO : importExcelList) {
            try {
                StandardsDataElemCodeDO StandardsDataElemCodeDO = BeanUtils.toBean(respVO, StandardsDataElemCodeDO.class);
                Long StandardsDataElemCodeId = respVO.getId();
                if (isUpdateSupport) {
                    if (StandardsDataElemCodeId != null) {
                        StandardsDataElemCodeDO existingDpDataElemCode = StandardsDataElemCodeMapper.selectById(StandardsDataElemCodeId);
                        if (existingDpDataElemCode != null) {
                            StandardsDataElemCodeMapper.updateById(StandardsDataElemCodeDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + StandardsDataElemCodeId + " 的数据元代码记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + StandardsDataElemCodeId + " 的数据元代码记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<StandardsDataElemCodeDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", StandardsDataElemCodeId);
                    StandardsDataElemCodeDO existingDpDataElemCode = StandardsDataElemCodeMapper.selectOne(queryWrapper);
                    if (existingDpDataElemCode == null) {
                        StandardsDataElemCodeMapper.insert(StandardsDataElemCodeDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + StandardsDataElemCodeId + " 的数据元代码记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + StandardsDataElemCodeId + " 的数据元代码记录已存在。");
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
    public Integer validateCodeValue(String dataElemId, String codeValue, String id) {
        return baseMapper.selectCount(Wrappers.lambdaQuery(StandardsDataElemCodeDO.class)
                .ne(StringUtils.isNotBlank(id), StandardsDataElemCodeDO::getId, id)
                .eq(StandardsDataElemCodeDO::getDataElemId, dataElemId)
                .eq(StandardsDataElemCodeDO::getCodeValue, codeValue)) > 0 ? 0 : 1;
    }
}
