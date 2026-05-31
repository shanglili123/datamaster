

package com.datamaster.module.standards.service.whitelist.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aliyun.oss.ServiceException;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelPageReqVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelRespVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeUserRelDO;
import com.datamaster.module.standards.dal.mapper.whitelist.StandardsDesensitizeUserRelMapper;
import com.datamaster.module.standards.service.whitelist.IStandardsDesensitizeUserRelService;
/**
 * 脱敏白名单与用户关联关系Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDesensitizeUserRelServiceImpl  extends ServiceImpl<StandardsDesensitizeUserRelMapper,StandardsDesensitizeUserRelDO> implements IStandardsDesensitizeUserRelService {
    @Resource
    private StandardsDesensitizeUserRelMapper StandardsDesensitizeUserRelMapper;

    @Override
    public PageResult<StandardsDesensitizeUserRelDO> getDgDesensitizeUserRelPage(StandardsDesensitizeUserRelPageReqVO pageReqVO) {
        return StandardsDesensitizeUserRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDgDesensitizeUserRel(StandardsDesensitizeUserRelSaveReqVO createReqVO) {
        StandardsDesensitizeUserRelDO dictType = BeanUtils.toBean(createReqVO, StandardsDesensitizeUserRelDO.class);
        StandardsDesensitizeUserRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDgDesensitizeUserRel(StandardsDesensitizeUserRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新脱敏白名单与用户关联关系
        StandardsDesensitizeUserRelDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDesensitizeUserRelDO.class);
        return StandardsDesensitizeUserRelMapper.updateById(updateObj);
    }
    @Override
    public int removeDgDesensitizeUserRel(Collection<Long> idList) {
        // 批量删除脱敏白名单与用户关联关系
        return StandardsDesensitizeUserRelMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDesensitizeUserRelDO getDgDesensitizeUserRelById(Long id) {
        return StandardsDesensitizeUserRelMapper.selectById(id);
    }

    @Override
    public List<StandardsDesensitizeUserRelDO> getDgDesensitizeUserRelList() {
        return StandardsDesensitizeUserRelMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDesensitizeUserRelDO> getDgDesensitizeUserRelMap() {
        List<StandardsDesensitizeUserRelDO> StandardsDesensitizeUserRelList = StandardsDesensitizeUserRelMapper.selectList();
        return StandardsDesensitizeUserRelList.stream()
                .collect(Collectors.toMap(
                        StandardsDesensitizeUserRelDO::getId,
                        StandardsDesensitizeUserRelDO -> StandardsDesensitizeUserRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入脱敏白名单与用户关联关系数据
         *
         * @param importExcelList 脱敏白名单与用户关联关系数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDgDesensitizeUserRel(List<StandardsDesensitizeUserRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsDesensitizeUserRelRespVO respVO : importExcelList) {
                try {
                    StandardsDesensitizeUserRelDO StandardsDesensitizeUserRelDO = BeanUtils.toBean(respVO, StandardsDesensitizeUserRelDO.class);
                    Long StandardsDesensitizeUserRelId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsDesensitizeUserRelId != null) {
                            StandardsDesensitizeUserRelDO existingDgDesensitizeUserRel = StandardsDesensitizeUserRelMapper.selectById(StandardsDesensitizeUserRelId);
                            if (existingDgDesensitizeUserRel != null) {
                                StandardsDesensitizeUserRelMapper.updateById(StandardsDesensitizeUserRelDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsDesensitizeUserRelId + " 的脱敏白名单与用户关联关系记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsDesensitizeUserRelId + " 的脱敏白名单与用户关联关系记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsDesensitizeUserRelDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsDesensitizeUserRelId);
                        StandardsDesensitizeUserRelDO existingDgDesensitizeUserRel = StandardsDesensitizeUserRelMapper.selectOne(queryWrapper);
                        if (existingDgDesensitizeUserRel == null) {
                            StandardsDesensitizeUserRelMapper.insert(StandardsDesensitizeUserRelDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsDesensitizeUserRelId + " 的脱敏白名单与用户关联关系记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsDesensitizeUserRelId + " 的脱敏白名单与用户关联关系记录已存在。");
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
