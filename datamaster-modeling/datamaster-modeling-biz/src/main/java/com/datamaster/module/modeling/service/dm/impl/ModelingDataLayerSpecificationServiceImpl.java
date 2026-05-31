

package com.datamaster.module.modeling.service.dm.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerSpecificationDO;
import com.datamaster.module.modeling.dal.mapper.dm.ModelingDataLayerSpecificationMapper;
import com.datamaster.module.modeling.service.dm.IModelingDataLayerSpecificationService;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 数仓分层-规范管理Service业务层处理
 *
 * @author FXB
 * @date 2026-03-24
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelingDataLayerSpecificationServiceImpl extends ServiceImpl<ModelingDataLayerSpecificationMapper, ModelingDataLayerSpecificationDO> implements IModelingDataLayerSpecificationService {
    @Resource
    private ModelingDataLayerSpecificationMapper ModelingDataLayerSpecificationMapper;

    @Override
    public PageResult<ModelingDataLayerSpecificationDO> getModelingDataLayerSpecificationPage(ModelingDataLayerSpecificationPageReqVO pageReqVO) {
        return ModelingDataLayerSpecificationMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createModelingDataLayerSpecification(ModelingDataLayerSpecificationSaveReqVO createReqVO) {
        ModelingDataLayerSpecificationDO dictType = BeanUtils.toBean(createReqVO, ModelingDataLayerSpecificationDO.class);
        ModelingDataLayerSpecificationMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelingDataLayerSpecification(ModelingDataLayerSpecificationSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数仓分层-规范管理
        ModelingDataLayerSpecificationDO updateObj = BeanUtils.toBean(updateReqVO, ModelingDataLayerSpecificationDO.class);
        return ModelingDataLayerSpecificationMapper.updateById(updateObj);
    }

    @Override
    public int removeModelingDataLayerSpecification(Collection<Long> idList) {
        // 批量删除数仓分层-规范管理
        return ModelingDataLayerSpecificationMapper.deleteBatchIds(idList);
    }

    @Override
    public ModelingDataLayerSpecificationDO getModelingDataLayerSpecificationById(Long id) {
        MPJLambdaWrapperX<ModelingDataLayerSpecificationDO> lambdaWrapper = new MPJLambdaWrapperX<>();

        lambdaWrapper.selectAll(ModelingDataLayerSpecificationDO.class)
                .select("u.NICK_NAME AS ownerUserName","u.PHONENUMBER AS ownerUserPhoneNumber")
                .leftJoin("SYSTEM_USER u on t.OWNER_USER_ID = u.USER_ID AND u.DEL_FLAG = '0'")
                .eq(ModelingDataLayerSpecificationDO::getId, id);
        return ModelingDataLayerSpecificationMapper.selectOne(lambdaWrapper);
    }

    @Override
    public List<ModelingDataLayerSpecificationDO> getModelingDataLayerSpecificationPage() {
        return ModelingDataLayerSpecificationMapper.selectList();
    }

    @Override
    public Map<Long, ModelingDataLayerSpecificationDO> getModelingDataLayerSpecificationMap() {
        List<ModelingDataLayerSpecificationDO> ModelingDataLayerSpecificationList = ModelingDataLayerSpecificationMapper.selectList();
        return ModelingDataLayerSpecificationList.stream()
                .collect(Collectors.toMap(
                        ModelingDataLayerSpecificationDO::getId,
                        ModelingDataLayerSpecificationDO -> ModelingDataLayerSpecificationDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数仓分层-规范管理数据
     *
     * @param importExcelList 数仓分层-规范管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importModelingDataLayerSpecification(List<ModelingDataLayerSpecificationRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (ModelingDataLayerSpecificationRespVO respVO : importExcelList) {
            try {
                ModelingDataLayerSpecificationDO ModelingDataLayerSpecificationDO = BeanUtils.toBean(respVO, ModelingDataLayerSpecificationDO.class);
                Long ModelingDataLayerSpecificationId = respVO.getId();
                if (isUpdateSupport) {
                    if (ModelingDataLayerSpecificationId != null) {
                        ModelingDataLayerSpecificationDO existingModelingDataLayerSpecification = ModelingDataLayerSpecificationMapper.selectById(ModelingDataLayerSpecificationId);
                        if (existingModelingDataLayerSpecification != null) {
                            ModelingDataLayerSpecificationMapper.updateById(ModelingDataLayerSpecificationDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + ModelingDataLayerSpecificationId + " 的数仓分层-规范管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + ModelingDataLayerSpecificationId + " 的数仓分层-规范管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<ModelingDataLayerSpecificationDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", ModelingDataLayerSpecificationId);
                    ModelingDataLayerSpecificationDO existingModelingDataLayerSpecification = ModelingDataLayerSpecificationMapper.selectOne(queryWrapper);
                    if (existingModelingDataLayerSpecification == null) {
                        ModelingDataLayerSpecificationMapper.insert(ModelingDataLayerSpecificationDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + ModelingDataLayerSpecificationId + " 的数仓分层-规范管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + ModelingDataLayerSpecificationId + " 的数仓分层-规范管理记录已存在。");
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
