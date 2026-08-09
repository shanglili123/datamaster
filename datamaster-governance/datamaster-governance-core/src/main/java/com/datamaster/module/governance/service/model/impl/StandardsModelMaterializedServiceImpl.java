package com.datamaster.module.governance.service.model.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.governance.controller.admin.model.vo.*;
import com.datamaster.module.governance.dal.dataobject.model.StandardsModelMaterializedDO;
import com.datamaster.module.governance.dal.mapper.model.StandardsModelMaterializedMapper;
import com.datamaster.module.governance.service.model.IStandardsModelMaterializedService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物化模型记录Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsModelMaterializedServiceImpl  extends ServiceImpl<StandardsModelMaterializedMapper,StandardsModelMaterializedDO> implements IStandardsModelMaterializedService {
    @Resource
    private StandardsModelMaterializedMapper StandardsModelMaterializedMapper;

    @Override
    public PageResult<StandardsModelMaterializedDO> getDpModelMaterializedPage(StandardsModelMaterializedPageReqVO pageReqVO) {
        return StandardsModelMaterializedMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDpModelMaterialized(StandardsModelMaterializedSaveReqVO createReqVO) {
        StandardsModelMaterializedDO dictType = BeanUtils.toBean(createReqVO, StandardsModelMaterializedDO.class);
        StandardsModelMaterializedMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpModelMaterialized(StandardsModelMaterializedSaveReqVO updateReqVO) {
        // 相关校验

        // 更新物化模型记录
        StandardsModelMaterializedDO updateObj = BeanUtils.toBean(updateReqVO, StandardsModelMaterializedDO.class);
        return StandardsModelMaterializedMapper.updateById(updateObj);
    }
    @Override
    public int removeDpModelMaterialized(Collection<Long> idList) {
        // 批量删除物化模型记录
        return StandardsModelMaterializedMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsModelMaterializedDO getDpModelMaterializedById(Long id) {
        return StandardsModelMaterializedMapper.selectById(id);
    }

    @Override
    public List<StandardsModelMaterializedDO> getDpModelMaterializedList() {
        return StandardsModelMaterializedMapper.selectList();
    }

    @Override
    public Map<Long, StandardsModelMaterializedDO> getDpModelMaterializedMap() {
        List<StandardsModelMaterializedDO> StandardsModelMaterializedList = StandardsModelMaterializedMapper.selectList();
        return StandardsModelMaterializedList.stream()
                .collect(Collectors.toMap(
                        StandardsModelMaterializedDO::getId,
                        StandardsModelMaterializedDO -> StandardsModelMaterializedDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入物化模型记录数据
         *
         * @param importExcelList 物化模型记录数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDpModelMaterialized(List<StandardsModelMaterializedRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsModelMaterializedRespVO respVO : importExcelList) {
                try {
                    StandardsModelMaterializedDO StandardsModelMaterializedDO = BeanUtils.toBean(respVO, StandardsModelMaterializedDO.class);
                    Long StandardsModelMaterializedId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsModelMaterializedId != null) {
                            StandardsModelMaterializedDO existingDpModelMaterialized = StandardsModelMaterializedMapper.selectById(StandardsModelMaterializedId);
                            if (existingDpModelMaterialized != null) {
                                StandardsModelMaterializedMapper.updateById(StandardsModelMaterializedDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsModelMaterializedId + " 的物化模型记录记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsModelMaterializedId + " 的物化模型记录记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsModelMaterializedDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsModelMaterializedId);
                        StandardsModelMaterializedDO existingDpModelMaterialized = StandardsModelMaterializedMapper.selectOne(queryWrapper);
                        if (existingDpModelMaterialized == null) {
                            StandardsModelMaterializedMapper.insert(StandardsModelMaterializedDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsModelMaterializedId + " 的物化模型记录记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsModelMaterializedId + " 的物化模型记录记录已存在。");
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
