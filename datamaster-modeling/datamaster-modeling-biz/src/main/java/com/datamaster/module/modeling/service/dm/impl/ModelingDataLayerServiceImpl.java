

package com.datamaster.module.modeling.service.dm.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.domain.TreeData;
import com.datamaster.common.core.domain.entity.SysDictData;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.modeling.api.service.dataLayer.IModelingDataLayerApiService;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSaveReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerTreeRespVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerDO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingThemeDomainDO;
import com.datamaster.module.modeling.dal.mapper.dm.ModelingDataLayerMapper;
import com.datamaster.module.modeling.service.dm.IModelingDataLayerService;
import com.datamaster.module.system.service.ISysDictDataService;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数仓分层管理Service业务层处理
 *
 * @author FXB
 * @date 2026-03-24
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelingDataLayerServiceImpl extends ServiceImpl<ModelingDataLayerMapper, ModelingDataLayerDO> implements IModelingDataLayerService, IModelingDataLayerApiService {
    @Resource
    private ModelingDataLayerMapper ModelingDataLayerMapper;

    @Resource
    private ISysDictDataService sysDictDataService;

    @Override
    public PageResult<ModelingDataLayerDO> getModelingDataLayerPage(ModelingDataLayerPageReqVO pageReqVO) {
        return ModelingDataLayerMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createModelingDataLayer(ModelingDataLayerSaveReqVO createReqVO) {
        ModelingDataLayerDO dictType = BeanUtils.toBean(createReqVO, ModelingDataLayerDO.class);
        ModelingDataLayerMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelingDataLayer(ModelingDataLayerSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数仓分层管理
        ModelingDataLayerDO updateObj = BeanUtils.toBean(updateReqVO, ModelingDataLayerDO.class);
        return ModelingDataLayerMapper.updateById(updateObj);
    }

    @Override
    public int removeModelingDataLayer(Collection<Long> idList) {
        // 批量删除数仓分层管理
        return ModelingDataLayerMapper.deleteBatchIds(idList);
    }

    @Override
    public ModelingDataLayerDO getModelingDataLayerById(Long id) {
        return ModelingDataLayerMapper.selectById(id);
    }

    @Override
    public List<ModelingDataLayerDO> getModelingDataLayerList() {
        return ModelingDataLayerMapper.selectList();
    }

    @Override
    public Map<Long, ModelingDataLayerDO> getModelingDataLayerMap() {
        List<ModelingDataLayerDO> ModelingDataLayerList = ModelingDataLayerMapper.selectList();
        return ModelingDataLayerList.stream()
                .collect(Collectors.toMap(
                        ModelingDataLayerDO::getId,
                        ModelingDataLayerDO -> ModelingDataLayerDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数仓分层管理数据
     *
     * @param importExcelList 数仓分层管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importModelingDataLayer(List<ModelingDataLayerRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (ModelingDataLayerRespVO respVO : importExcelList) {
            try {
                ModelingDataLayerDO ModelingDataLayerDO = BeanUtils.toBean(respVO, ModelingDataLayerDO.class);
                Long ModelingDataLayerId = respVO.getId();
                if (isUpdateSupport) {
                    if (ModelingDataLayerId != null) {
                        ModelingDataLayerDO existingModelingDataLayer = ModelingDataLayerMapper.selectById(ModelingDataLayerId);
                        if (existingModelingDataLayer != null) {
                            ModelingDataLayerMapper.updateById(ModelingDataLayerDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + ModelingDataLayerId + " 的数仓分层管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + ModelingDataLayerId + " 的数仓分层管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<ModelingDataLayerDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", ModelingDataLayerId);
                    ModelingDataLayerDO existingModelingDataLayer = ModelingDataLayerMapper.selectOne(queryWrapper);
                    if (existingModelingDataLayer == null) {
                        ModelingDataLayerMapper.insert(ModelingDataLayerDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + ModelingDataLayerId + " 的数仓分层管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + ModelingDataLayerId + " 的数仓分层管理记录已存在。");
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
    public List<ModelingDataLayerTreeRespVO> tree() {
        List<ModelingDataLayerDO> list = this.list();

        List<SysDictData> sysDictDataList = sysDictDataService.selectDictDataList(SysDictData.builder()
                .dictType("Modeling_data_layer_category")
                .status("0")
                .build());

        sysDictDataList.stream().sorted(Comparator.comparingLong(SysDictData::getDictSort));

        List<ModelingDataLayerTreeRespVO> tree = sysDictDataList.stream()
                .map(sysDictData -> ModelingDataLayerTreeRespVO.builder()
                        .id(Long.parseLong(sysDictData.getDictValue()))
                        .parentId(0L)
                        .name(sysDictData.getDictLabel())
                        .build())
                .collect(Collectors.toList());

        list.forEach(ModelingDataLayerDO -> {
            ModelingDataLayerTreeRespVO ModelingDataLayerRespVO = tree.stream()
                    .filter(item -> String.valueOf(item.getId()).equals(ModelingDataLayerDO.getCategory()))
                    .findFirst()
                    .orElse(null);
            if (ModelingDataLayerRespVO != null) {
                List<ModelingDataLayerTreeRespVO> childrenList = ModelingDataLayerRespVO.getChildren();
                if (childrenList == null) {
                    childrenList = new ArrayList();
                    ModelingDataLayerRespVO.setChildren(childrenList);
                }
                ModelingDataLayerTreeRespVO children = BeanUtils.toBean(ModelingDataLayerDO, ModelingDataLayerTreeRespVO.class);
                children.setParentId(Long.parseLong(children.getCategory()));
                childrenList.add(children);
            }
        });
        return tree;
    }

    @Override
    public List<TreeData> getTreeData(String type) {
        MPJLambdaWrapperX<ModelingDataLayerDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper
                .selectAll(ModelingDataLayerDO.class)
                .eq(ModelingDataLayerDO::getValidFlag, "1");
        String statisticsSql = null;
        if (StringUtils.isNotBlank(type)) {
            switch (type) {
                case "1":
                    statisticsSql = "(SELECT COUNT(1) FROM DA_ASSET a WHERE t.ID = a.DATA_LAYER_ID) AS num";
                    break;
            }
            if (StringUtils.isNotBlank(statisticsSql)) {
                lambdaWrapper.select(statisticsSql);
            }
        }
        List<ModelingDataLayerDO> list = this.list(lambdaWrapper);

        List<SysDictData> sysDictDataList = sysDictDataService.selectDictDataList(SysDictData.builder()
                .dictType("Modeling_data_layer_category")
                .status("0")
                .build());

        sysDictDataList.stream().sorted(Comparator.comparingLong(SysDictData::getDictSort));

        List<TreeData> tree = sysDictDataList.stream()
                .map(sysDictData -> TreeData.builder()
                        .id(Long.parseLong(sysDictData.getDictValue()))
                        .name(sysDictData.getDictLabel())
                        .type("4")
                        .build())
                .collect(Collectors.toList());

        list.forEach(ModelingDataLayerDO -> {
            TreeData treeData = tree.stream()
                    .filter(item -> String.valueOf(item.getId()).equals(ModelingDataLayerDO.getCategory()))
                    .findFirst()
                    .orElse(null);
            if (treeData != null) {
                List<TreeData> childrenList = treeData.getChildren();
                if (childrenList == null) {
                    childrenList = new ArrayList();
                    treeData.setChildren(childrenList);
                }
                childrenList.add(TreeData.builder()
                        .id(ModelingDataLayerDO.getId())
                        .name(ModelingDataLayerDO.getName())
                        .type("5")
                        .otherData(JSONObject.of(
                                "engName", ModelingDataLayerDO.getEngName(),
                                "num", ModelingDataLayerDO.getNum()))
                        .build());
            }
        });
        return tree;
    }
}
