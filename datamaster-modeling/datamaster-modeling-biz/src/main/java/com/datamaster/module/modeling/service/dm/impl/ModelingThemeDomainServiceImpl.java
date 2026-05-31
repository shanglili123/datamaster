

package com.datamaster.module.modeling.service.dm.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import com.datamaster.common.core.domain.TreeData;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.modeling.api.service.themeDomain.IModelingThemeDomainApiService;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataDomainDO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerDO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingThemeDomainDO;
import com.datamaster.module.modeling.dal.mapper.dm.ModelingThemeDomainMapper;
import com.datamaster.module.modeling.service.dm.IModelingThemeDomainService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 主题域管理Service业务层处理
 *
 * @author FXB
 * @date 2026-03-24
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelingThemeDomainServiceImpl extends ServiceImpl<ModelingThemeDomainMapper, ModelingThemeDomainDO> implements IModelingThemeDomainService, IModelingThemeDomainApiService {
    @Resource
    private ModelingThemeDomainMapper ModelingThemeDomainMapper;

    @Override
    public PageResult<ModelingThemeDomainDO> getModelingThemeDomainPage(ModelingThemeDomainPageReqVO pageReqVO) {
        return ModelingThemeDomainMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createModelingThemeDomain(ModelingThemeDomainSaveReqVO createReqVO) {
        ModelingThemeDomainDO dictType = BeanUtils.toBean(createReqVO, ModelingThemeDomainDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        ModelingThemeDomainMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelingThemeDomain(ModelingThemeDomainSaveReqVO updateReqVO) {
        ModelingThemeDomainDO catDO = baseMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        //判断是否选择了他自己
        if (catDO.getId().equals(updateReqVO.getParentId())) {
            throw new ServiceException("切换上级不能选择自身作为上级类目");
        }
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            baseMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            ModelingThemeDomainDO parent = baseMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }
        //修改上下级判断
        boolean flag = false;
        if (!catDO.getParentId().equals(updateReqVO.getParentId())) {
            updateReqVO.setCode(createCode(updateReqVO.getParentId(), null));
            flag = true;
        }

        // 更新数据服务类目管理
        ModelingThemeDomainDO updateObj = BeanUtils.toBean(updateReqVO, ModelingThemeDomainDO.class);
        int i = baseMapper.updateById(updateObj);

        //判断上下级是否发生了改变
        if (flag) {
            //更改所有下级
            changeCodeByPid(updateObj.getId(), updateObj.getCode());
        }
        return i;
    }

    @Override
    public int removeModelingThemeDomain(Collection<Long> idList) {
        // 批量删除主题域管理
        return ModelingThemeDomainMapper.deleteBatchIds(idList);
    }

    @Override
    public ModelingThemeDomainDO getModelingThemeDomainById(Long id) {
        MPJLambdaWrapperX<ModelingThemeDomainDO> lambdaWrapper = new MPJLambdaWrapperX<>();

        lambdaWrapper.selectAll(ModelingThemeDomainDO.class)
                .select("u.NICK_NAME AS ownerUserName","u.PHONENUMBER AS ownerUserPhoneNumber")
                .leftJoin("SYSTEM_USER u on t.OWNER_USER_ID = u.USER_ID AND u.DEL_FLAG = '0'")
                .eq(ModelingThemeDomainDO::getId, id);
        return ModelingThemeDomainMapper.selectOne(lambdaWrapper);
    }

    @Override
    public List<ModelingThemeDomainDO> getModelingThemeDomainList() {
        return ModelingThemeDomainMapper.selectList();
    }

    @Override
    public Map<Long, ModelingThemeDomainDO> getModelingThemeDomainMap() {
        List<ModelingThemeDomainDO> ModelingThemeDomainList = ModelingThemeDomainMapper.selectList();
        return ModelingThemeDomainList.stream()
                .collect(Collectors.toMap(
                        ModelingThemeDomainDO::getId,
                        ModelingThemeDomainDO -> ModelingThemeDomainDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入主题域管理数据
     *
     * @param importExcelList 主题域管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importModelingThemeDomain(List<ModelingThemeDomainRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (ModelingThemeDomainRespVO respVO : importExcelList) {
            try {
                ModelingThemeDomainDO ModelingThemeDomainDO = BeanUtils.toBean(respVO, ModelingThemeDomainDO.class);
                Long ModelingThemeDomainId = respVO.getId();
                if (isUpdateSupport) {
                    if (ModelingThemeDomainId != null) {
                        ModelingThemeDomainDO existingModelingThemeDomain = ModelingThemeDomainMapper.selectById(ModelingThemeDomainId);
                        if (existingModelingThemeDomain != null) {
                            ModelingThemeDomainMapper.updateById(ModelingThemeDomainDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + ModelingThemeDomainId + " 的主题域管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + ModelingThemeDomainId + " 的主题域管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<ModelingThemeDomainDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", ModelingThemeDomainId);
                    ModelingThemeDomainDO existingModelingThemeDomain = ModelingThemeDomainMapper.selectOne(queryWrapper);
                    if (existingModelingThemeDomain == null) {
                        ModelingThemeDomainMapper.insert(ModelingThemeDomainDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + ModelingThemeDomainId + " 的主题域管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + ModelingThemeDomainId + " 的主题域管理记录已存在。");
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
    public List<ModelingThemeDomainDO> getModelingThemeDomainList(ModelingThemeDomainPageReqVO reqVO) {
        MPJLambdaWrapperX<ModelingThemeDomainDO> lambdaWrapper = new MPJLambdaWrapperX<>();

        lambdaWrapper.selectAll(ModelingThemeDomainDO.class)
                .select("u.NICK_NAME AS ownerUserName", "u.PHONENUMBER AS ownerUserPhoneNumber", "layer.name AS dataLayerName")
                .leftJoin("SYSTEM_USER u on t.OWNER_USER_ID = u.USER_ID AND u.DEL_FLAG = '0'")
                .leftJoin(ModelingDataLayerDO.class, "layer", ModelingDataLayerDO::getId, ModelingThemeDomainDO::getDataLayerId);

        lambdaWrapper.eqIfPresent(ModelingThemeDomainDO::getCode, reqVO.getCode())
                .likeIfPresent(ModelingThemeDomainDO::getName, reqVO.getName())
                .likeIfPresent(ModelingThemeDomainDO::getEngName, reqVO.getEngName())
                .eqIfPresent(ModelingThemeDomainDO::getParentId, reqVO.getParentId())
                .eqIfPresent(ModelingThemeDomainDO::getOwnerUserId, reqVO.getOwnerUserId())
                .eqIfPresent(ModelingThemeDomainDO::getDataLayerId, reqVO.getDataLayerId())
                .likeIfPresent(ModelingThemeDomainDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelingThemeDomainDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelingThemeDomainDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderByDesc(ModelingThemeDomainDO::getCreateTime);
        return ModelingThemeDomainMapper.selectList(lambdaWrapper);
    }

    @Override
    public String createCode(Long parentId, String parentCode) {
        String categoryCode = null;
        /*
         * 分成三种情况
         * 1.数据库无数据 调用YouBianCodeUtil.getNextYouBianCode(null);
         * 2.添加子节点，无兄弟元素 YouBianCodeUtil.getSubYouBianCode(parentCode,null);
         * 3.添加子节点有兄弟元素 YouBianCodeUtil.getNextYouBianCode(lastCode);
         * */
        //找同类 确定上一个最大的code值
        LambdaQueryWrapper<ModelingThemeDomainDO> query = new LambdaQueryWrapper<ModelingThemeDomainDO>()
                .eq(ModelingThemeDomainDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), ModelingThemeDomainDO::getCode, parentCode)
                .isNotNull(ModelingThemeDomainDO::getCode)
                .orderByDesc(ModelingThemeDomainDO::getCode);
        List<ModelingThemeDomainDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                ModelingThemeDomainDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }

    @Override
    public void changeCodeByPid(Long pid, String parentCode) {
        List<ModelingThemeDomainDO> list = baseMapper.selectList(Wrappers.lambdaQuery(ModelingThemeDomainDO.class)
                .eq(ModelingThemeDomainDO::getParentId, pid)
                .orderByAsc(ModelingThemeDomainDO::getCreateTime));
        if (list != null && list.size() > 0) {
            list.forEach(e -> {
                String codeNew = createCode(e.getParentId(), parentCode);
                e.setCode(codeNew);
                baseMapper.updateById(e);
                this.changeCodeByPid(e.getId(), e.getCode());
            });
        }
    }

    @Override
    public List<TreeData> getTreeData(String type) {
        //获取所有开启的数据
        MPJLambdaWrapperX<ModelingThemeDomainDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper.selectAll(ModelingThemeDomainDO.class)
                .eq(ModelingThemeDomainDO::getValidFlag, "1");
        String statisticsSql = null;
        if (StringUtils.isNotBlank(type)) {
            switch (type) {
                case "1":
                    statisticsSql = "(SELECT COUNT(1) FROM DA_ASSET a WHERE t.ID = a.THEME_DOMAIN_ID) AS num";
                    break;
            }
            if (StringUtils.isNotBlank(statisticsSql)) {
                lambdaWrapper.select(statisticsSql);
            }
        }
        List<ModelingThemeDomainDO> list = baseMapper.selectList(lambdaWrapper);

        //组装业务分类树
        Map<Long, TreeData> treeDataMap = list.stream()
                .collect(Collectors.toMap(k -> k.getId(), v -> TreeData.builder()
                        .id(v.getId())
                        .parentId(v.getParentId())
                        .name(v.getName())
                        .type("3")
                        .otherData(JSONObject.of(
                                "code", v.getCode(),
                                "engName", v.getEngName(),
                                "num", v.getNum()))
                        .build()));

        for (TreeData treeData : treeDataMap.values()) {
            TreeData parent = treeDataMap.get(treeData.getParentId());
            if (parent != null) {
                List<TreeData> children = parent.getChildren();
                if (children == null) {
                    children = new ArrayList<>();
                    parent.setChildren(children);
                }
                children.add(treeData);
            }
        }
        return treeDataMap.values()
                .stream()
                .filter(treeData -> treeData.getParentId() == 0)
                .collect(Collectors.toList());
    }
}
