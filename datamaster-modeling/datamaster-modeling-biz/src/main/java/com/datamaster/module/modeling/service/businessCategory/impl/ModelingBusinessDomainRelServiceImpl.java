

package com.datamaster.module.modeling.service.businessCategory.impl;

import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelPageReqVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelRespVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessDomainRelDO;
import com.datamaster.module.modeling.dal.mapper.businessCategory.ModelingBusinessDomainRelMapper;
import com.datamaster.module.modeling.service.businessCategory.IModelingBusinessDomainRelService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务分类数据域关联关系Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelingBusinessDomainRelServiceImpl extends ServiceImpl<ModelingBusinessDomainRelMapper,ModelingBusinessDomainRelDO> implements IModelingBusinessDomainRelService {
    @Resource
    private ModelingBusinessDomainRelMapper ModelingBusinessDomainRelMapper;

    @Override
    public PageResult<ModelingBusinessDomainRelDO> getModelingBusinessDomainRelPage(ModelingBusinessDomainRelPageReqVO pageReqVO) {
        return ModelingBusinessDomainRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createModelingBusinessDomainRel(ModelingBusinessDomainRelSaveReqVO createReqVO) {
        ModelingBusinessDomainRelDO dictType = BeanUtils.toBean(createReqVO, ModelingBusinessDomainRelDO.class);
        ModelingBusinessDomainRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelingBusinessDomainRel(ModelingBusinessDomainRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新业务分类数据域关联关系
        ModelingBusinessDomainRelDO updateObj = BeanUtils.toBean(updateReqVO, ModelingBusinessDomainRelDO.class);
        return ModelingBusinessDomainRelMapper.updateById(updateObj);
    }
    @Override
    public int removeModelingBusinessDomainRel(Collection<Long> idList) {
        // 批量删除业务分类数据域关联关系
        return ModelingBusinessDomainRelMapper.deleteBatchIds(idList);
    }

    @Override
    public ModelingBusinessDomainRelDO getModelingBusinessDomainRelById(Long id) {
        return ModelingBusinessDomainRelMapper.selectById(id);
    }

    @Override
    public List<ModelingBusinessDomainRelDO> getModelingBusinessDomainRelList() {
        return ModelingBusinessDomainRelMapper.selectList();
    }

    @Override
    public Map<Long, ModelingBusinessDomainRelDO> getModelingBusinessDomainRelMap() {
        List<ModelingBusinessDomainRelDO> ModelingBusinessDomainRelList = ModelingBusinessDomainRelMapper.selectList();
        return ModelingBusinessDomainRelList.stream()
                .collect(Collectors.toMap(
                        ModelingBusinessDomainRelDO::getId,
                        ModelingBusinessDomainRelDO -> ModelingBusinessDomainRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入业务分类数据域关联关系数据
         *
         * @param importExcelList 业务分类数据域关联关系数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importModelingBusinessDomainRel(List<ModelingBusinessDomainRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (ModelingBusinessDomainRelRespVO respVO : importExcelList) {
                try {
                    ModelingBusinessDomainRelDO ModelingBusinessDomainRelDO = BeanUtils.toBean(respVO, ModelingBusinessDomainRelDO.class);
                    Long ModelingBusinessDomainRelId = respVO.getId();
                    if (isUpdateSupport) {
                        if (ModelingBusinessDomainRelId != null) {
                            ModelingBusinessDomainRelDO existingModelingBusinessDomainRel = ModelingBusinessDomainRelMapper.selectById(ModelingBusinessDomainRelId);
                            if (existingModelingBusinessDomainRel != null) {
                                ModelingBusinessDomainRelMapper.updateById(ModelingBusinessDomainRelDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + ModelingBusinessDomainRelId + " 的业务分类数据域关联关系记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + ModelingBusinessDomainRelId + " 的业务分类数据域关联关系记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<ModelingBusinessDomainRelDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", ModelingBusinessDomainRelId);
                        ModelingBusinessDomainRelDO existingModelingBusinessDomainRel = ModelingBusinessDomainRelMapper.selectOne(queryWrapper);
                        if (existingModelingBusinessDomainRel == null) {
                            ModelingBusinessDomainRelMapper.insert(ModelingBusinessDomainRelDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + ModelingBusinessDomainRelId + " 的业务分类数据域关联关系记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + ModelingBusinessDomainRelId + " 的业务分类数据域关联关系记录已存在。");
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
    public Integer removeModelingBusinessDomainRelByDomainId(Long domainId, Long businessCategoryId) {
         if (domainId == null || businessCategoryId == null || businessCategoryId == 0) {
              throw new ServiceException("数据域ID或业务分类ID不能为空！");
         }
        //根据数据域ID和业务分类ID删除关联关系
        return ModelingBusinessDomainRelMapper.delete(new LambdaQueryWrapper<ModelingBusinessDomainRelDO>()
               .eq(ModelingBusinessDomainRelDO::getDataDomainId, domainId)
               .eq(ModelingBusinessDomainRelDO::getBusinessCategoryId, businessCategoryId));
    }
}
