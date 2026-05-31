

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
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataDomainDO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerDO;
import com.datamaster.module.modeling.dal.mapper.dm.ModelingDataDomainMapper;
import com.datamaster.module.modeling.service.dm.IModelingDataDomainService;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 数据域管理Service业务层处理
 *
 * @author FXB
 * @date 2026-03-24
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelingDataDomainServiceImpl  extends ServiceImpl<ModelingDataDomainMapper,ModelingDataDomainDO> implements IModelingDataDomainService {
    @Resource
    private ModelingDataDomainMapper ModelingDataDomainMapper;

    @Override
    public PageResult<ModelingDataDomainDO> getModelingDataDomainPage(ModelingDataDomainPageReqVO pageReqVO) {
        return ModelingDataDomainMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createModelingDataDomain(ModelingDataDomainSaveReqVO createReqVO) {
        ModelingDataDomainDO dictType = BeanUtils.toBean(createReqVO, ModelingDataDomainDO.class);
        ModelingDataDomainMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelingDataDomain(ModelingDataDomainSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据域管理
        ModelingDataDomainDO updateObj = BeanUtils.toBean(updateReqVO, ModelingDataDomainDO.class);
        return ModelingDataDomainMapper.updateById(updateObj);
    }
    @Override
    public int removeModelingDataDomain(Collection<Long> idList) {
        // 批量删除数据域管理
        return ModelingDataDomainMapper.deleteBatchIds(idList);
    }

    @Override
    public ModelingDataDomainDO getModelingDataDomainById(Long id) {
        MPJLambdaWrapperX<ModelingDataDomainDO> lambdaWrapper = new MPJLambdaWrapperX<>();

        lambdaWrapper.selectAll(ModelingDataDomainDO.class)
                .select("u.NICK_NAME AS ownerUserName","u.PHONENUMBER AS ownerUserPhoneNumber")
                .leftJoin("SYSTEM_USER u on t.OWNER_USER_ID = u.USER_ID AND u.DEL_FLAG = '0'")
                .eq(ModelingDataDomainDO::getId, id);
        return ModelingDataDomainMapper.selectOne(lambdaWrapper);
    }

    @Override
    public List<ModelingDataDomainDO> getModelingDataDomainList() {
        return ModelingDataDomainMapper.selectList();
    }

    @Override
    public Map<Long, ModelingDataDomainDO> getModelingDataDomainMap() {
        List<ModelingDataDomainDO> ModelingDataDomainList = ModelingDataDomainMapper.selectList();
        return ModelingDataDomainList.stream()
                .collect(Collectors.toMap(
                        ModelingDataDomainDO::getId,
                        ModelingDataDomainDO -> ModelingDataDomainDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据域管理数据
         *
         * @param importExcelList 数据域管理数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importModelingDataDomain(List<ModelingDataDomainRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (ModelingDataDomainRespVO respVO : importExcelList) {
                try {
                    ModelingDataDomainDO ModelingDataDomainDO = BeanUtils.toBean(respVO, ModelingDataDomainDO.class);
                    Long ModelingDataDomainId = respVO.getId();
                    if (isUpdateSupport) {
                        if (ModelingDataDomainId != null) {
                            ModelingDataDomainDO existingModelingDataDomain = ModelingDataDomainMapper.selectById(ModelingDataDomainId);
                            if (existingModelingDataDomain != null) {
                                ModelingDataDomainMapper.updateById(ModelingDataDomainDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + ModelingDataDomainId + " 的数据域管理记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + ModelingDataDomainId + " 的数据域管理记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<ModelingDataDomainDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", ModelingDataDomainId);
                        ModelingDataDomainDO existingModelingDataDomain = ModelingDataDomainMapper.selectOne(queryWrapper);
                        if (existingModelingDataDomain == null) {
                            ModelingDataDomainMapper.insert(ModelingDataDomainDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + ModelingDataDomainId + " 的数据域管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + ModelingDataDomainId + " 的数据域管理记录已存在。");
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
    public PageResult<ModelingDataDomainDO> getModelingDataDomainByCategoryId(ModelingDataDomainPageReqVO ModelingDataDomain) {
        return ModelingDataDomainMapper.selectlistBybusinessDomainId(ModelingDataDomain);
    }
}
