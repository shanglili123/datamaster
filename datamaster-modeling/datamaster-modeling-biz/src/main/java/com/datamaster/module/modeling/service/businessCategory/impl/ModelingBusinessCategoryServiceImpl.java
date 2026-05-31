

package com.datamaster.module.modeling.service.businessCategory.impl;

import com.alibaba.fastjson2.JSONObject;
import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.domain.TreeData;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.modeling.api.service.businessCategory.IModelingBusinessCategoryApiService;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategoryPageReqVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategoryRespVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategorySaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessCategoryDO;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessDomainRelDO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataDomainDO;
import com.datamaster.module.modeling.dal.mapper.businessCategory.ModelingBusinessCategoryMapper;
import com.datamaster.module.modeling.dal.mapper.businessCategory.ModelingBusinessDomainRelMapper;
import com.datamaster.module.modeling.dal.mapper.dm.ModelingDataDomainMapper;
import com.datamaster.module.modeling.service.businessCategory.IModelingBusinessCategoryService;
import com.datamaster.module.system.mapper.SysUserMapper;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务分类Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-04-08
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelingBusinessCategoryServiceImpl extends ServiceImpl<ModelingBusinessCategoryMapper, ModelingBusinessCategoryDO> implements IModelingBusinessCategoryService, IModelingBusinessCategoryApiService {
    @Resource
    private ModelingBusinessCategoryMapper ModelingBusinessCategoryMapper;

    @Resource
    private ModelingBusinessDomainRelMapper ModelingBusinessDomainRelMapper;

    @Resource
    private ModelingDataDomainMapper ModelingDataDomainMapper;

    @Resource
    private SysUserMapper ModelingUserMapper;


    @Override
    public PageResult<ModelingBusinessCategoryDO> getModelingBusinessCategoryPage(ModelingBusinessCategoryPageReqVO pageReqVO) {
        PageResult<ModelingBusinessCategoryDO> pageResult = ModelingBusinessCategoryMapper.selectPage(pageReqVO);
      /*  //根据业务分类id 查询数据域集合 存入DmBusinessCategoryDO
        pageResult.getRows().forEach(item -> {
            item.setDataDomainList(ModelingDataDomainMapper.selectlistBybusinessDomainId(item.getId()));
        });*/
        return pageResult;
    }

    @Override
    public Long createModelingBusinessCategory(ModelingBusinessCategorySaveReqVO createReqVO) {
        ModelingBusinessCategoryDO dictType = BeanUtils.toBean(createReqVO, ModelingBusinessCategoryDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        ModelingBusinessCategoryMapper.insert(dictType);
        // 插入数据域关联关系
        if (dictType.getDomainList() != null && !dictType.getDomainList().isEmpty()) {
            dictType.getDomainList().forEach(domain -> {
                domain.setBusinessCategoryId(dictType.getId());
                domain.setBusinessCategoryName(dictType.getName());
            });
            ModelingBusinessDomainRelMapper.insertBatch(dictType.getDomainList());
        }
        return dictType.getId();
    }

    @Override
    public int updateModelingBusinessCategory(ModelingBusinessCategorySaveReqVO updateReqVO) {
        ModelingBusinessCategoryDO catDO = baseMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        //判断是否选择了他自己
        if (catDO.getId().equals(updateReqVO.getParentId())) {
            throw new com.datamaster.common.exception.ServiceException("切换上级不能选择自身作为上级类目");
        }

        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            baseMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            ModelingBusinessCategoryDO parent = baseMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new com.datamaster.common.exception.ServiceException("须先启用父级");
            }
        }
        //修改上下级判断
        boolean flag = false;
        if (!catDO.getParentId().equals(updateReqVO.getParentId())) {
            updateReqVO.setCode(createCode(updateReqVO.getParentId(), null));
            flag = true;
        }

        // 更新业务分类
        ModelingBusinessCategoryDO updateObj = BeanUtils.toBean(updateReqVO, ModelingBusinessCategoryDO.class);
        // 插入新的数据域关联关系
        if (updateObj.getDomainList() != null && !updateObj.getDomainList().isEmpty()) {
            //先根据业务分类id 删除旧的关联域
            ModelingBusinessDomainRelMapper.delete(new LambdaQueryWrapper<ModelingBusinessDomainRelDO>().eq(ModelingBusinessDomainRelDO::getBusinessCategoryId, updateObj.getId()));
            updateObj.getDomainList().forEach(domain -> {
                domain.setBusinessCategoryId(updateObj.getId());
                domain.setBusinessCategoryName(updateObj.getName());
            });
            ModelingBusinessDomainRelMapper.insertBatch(updateObj.getDomainList());
        }

        int i = ModelingBusinessCategoryMapper.updateById(updateObj);
        //判断上下级是否发生了改变
        if (flag) {
            //更改所有下级
            changeCodeByPid(updateObj.getId(), updateObj.getCode());
        }

        return i;
    }

    @Override
    public int removeModelingBusinessCategory(Collection<Long> idList) {
        //xxxi先查询是否包含子业务分类  如果包含 则提示删除子业务分类后在删除
        if (idList.stream()
                .anyMatch(id -> ModelingBusinessCategoryMapper.selectCount(new LambdaQueryWrapper<ModelingBusinessCategoryDO>().eq(ModelingBusinessCategoryDO::getParentId, id)) > 0)) {
            throw new IllegalArgumentException("业务分类下存在子业务分类，不能删除");
        }
        // 先删除业务分类关联的域域关系业务分类
        ModelingBusinessDomainRelMapper.delete(new LambdaQueryWrapper<ModelingBusinessDomainRelDO>().in(ModelingBusinessDomainRelDO::getBusinessCategoryId, idList));
        // 删除业务分类关联
        return ModelingBusinessCategoryMapper.deleteBatchIds(idList);
    }

    @Override
    public ModelingBusinessCategoryDO getModelingBusinessCategoryById(Long id) {
        ModelingBusinessCategoryDO ModelingBusinessCategoryDO = ModelingBusinessCategoryMapper.selectById(id);
        //查询用户表 将ownerId 转换为ownerName
        if (ModelingBusinessCategoryDO.getOwnerId() != null) {
            SysUser sysUser = ModelingUserMapper.selectUserById(ModelingBusinessCategoryDO.getOwnerId());
            if (sysUser != null) {
                ModelingBusinessCategoryDO.setOwnerName(sysUser.getNickName());
            }
        }
        //查询业务分类表 将parentId 转换为parentName
        if (ModelingBusinessCategoryDO.getParentId() != null) {
            ModelingBusinessCategoryDO categoryDO = ModelingBusinessCategoryMapper.selectById(ModelingBusinessCategoryDO.getParentId());
            if (categoryDO != null) {
                ModelingBusinessCategoryDO.setParentName(categoryDO.getName());
            }
        }
        //根据业务分类id 查询数据域ID集合 存入DmBusinessCategoryDO
        ModelingBusinessCategoryDO.setDomainIds(ModelingBusinessDomainRelMapper.selectList(new LambdaQueryWrapper<ModelingBusinessDomainRelDO>().eq(ModelingBusinessDomainRelDO::getBusinessCategoryId, id))
                .stream()
                .map(ModelingBusinessDomainRelDO::getDataDomainId)
                .map(String::valueOf)
                .collect(Collectors.toList()));
        /*if (ModelingBusinessCategoryDO != null) {
            ModelingBusinessCategoryDO.setDataDomainList(ModelingDataDomainMapper.selectlistBybusinessDomainId(id));
        }*/
        return ModelingBusinessCategoryDO;
    }

    @Override
    public List<ModelingBusinessCategoryDO> getModelingBusinessCategoryList(ModelingBusinessCategoryPageReqVO ModelingBusinessCategory) {
        List<ModelingBusinessCategoryDO> pageResult = ModelingBusinessCategoryMapper.selectAllList(ModelingBusinessCategory);
        //根据业务域id 查询数据域集合 存入DmBusinessCategoryDO
        pageResult.forEach(item -> {
            List<ModelingBusinessDomainRelDO> cc = ModelingBusinessDomainRelMapper.selectList(new LambdaQueryWrapper<ModelingBusinessDomainRelDO>().eq(ModelingBusinessDomainRelDO::getBusinessCategoryId, item.getId()));
            item.setDomainList(cc);
        });
        return pageResult;
    }


    @Override
    public Map<Long, ModelingBusinessCategoryDO> getModelingBusinessCategoryMap() {
        List<ModelingBusinessCategoryDO> ModelingBusinessCategoryList = ModelingBusinessCategoryMapper.selectList();
        return ModelingBusinessCategoryList.stream()
                .collect(Collectors.toMap(ModelingBusinessCategoryDO::getId, ModelingBusinessCategoryDO -> ModelingBusinessCategoryDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing));
    }


    /**
     * 导入业务分类数据
     *
     * @param importExcelList 业务分类数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importModelingBusinessCategory(List<ModelingBusinessCategoryRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (ModelingBusinessCategoryRespVO respVO : importExcelList) {
            try {
                ModelingBusinessCategoryDO ModelingBusinessCategoryDO = BeanUtils.toBean(respVO, ModelingBusinessCategoryDO.class);
                Long ModelingBusinessCategoryId = respVO.getId();
                if (isUpdateSupport) {
                    if (ModelingBusinessCategoryId != null) {
                        ModelingBusinessCategoryDO existingModelingBusinessCategory = ModelingBusinessCategoryMapper.selectById(ModelingBusinessCategoryId);
                        if (existingModelingBusinessCategory != null) {
                            ModelingBusinessCategoryMapper.updateById(ModelingBusinessCategoryDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + ModelingBusinessCategoryId + " 的业务分类记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + ModelingBusinessCategoryId + " 的业务分类记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<ModelingBusinessCategoryDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", ModelingBusinessCategoryId);
                    ModelingBusinessCategoryDO existingModelingBusinessCategory = ModelingBusinessCategoryMapper.selectOne(queryWrapper);
                    if (existingModelingBusinessCategory == null) {
                        ModelingBusinessCategoryMapper.insert(ModelingBusinessCategoryDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + ModelingBusinessCategoryId + " 的业务分类记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + ModelingBusinessCategoryId + " 的业务分类记录已存在。");
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
    public String createCode(Long parentId, String parentCode) {
        String categoryCode = null;
        /*
         * 分成三种情况
         * 1.数据库无数据 调用YouBianCodeUtil.getNextYouBianCode(null);
         * 2.添加子节点，无兄弟元素 YouBianCodeUtil.getSubYouBianCode(parentCode,null);
         * 3.添加子节点有兄弟元素 YouBianCodeUtil.getNextYouBianCode(lastCode);
         * */
        //找同类 确定上一个最大的code值
        LambdaQueryWrapper<ModelingBusinessCategoryDO> query = new LambdaQueryWrapper<ModelingBusinessCategoryDO>().eq(ModelingBusinessCategoryDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), ModelingBusinessCategoryDO::getCode, parentCode)
                .isNotNull(ModelingBusinessCategoryDO::getCode)
                .orderByDesc(ModelingBusinessCategoryDO::getCode);
        List<ModelingBusinessCategoryDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                ModelingBusinessCategoryDO parent = baseMapper.selectById(parentId);
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
        List<ModelingBusinessCategoryDO> list = baseMapper.selectList(Wrappers.lambdaQuery(ModelingBusinessCategoryDO.class)
                .eq(ModelingBusinessCategoryDO::getParentId, pid)
                .orderByAsc(ModelingBusinessCategoryDO::getCreateTime));
        if (list != null && list.size() > 0) {
            list.forEach(e -> {
                String codeOld = e.getCode();
                String codeNew = createCode(e.getParentId(), parentCode);
                e.setCode(codeNew);
                baseMapper.updateById(e);
                this.changeCodeByPid(e.getId(), e.getCode());
            });
        }
    }


    @Override
    public List<TreeData> getTreeData(String type) {
        //获取所有开启的业务分类
        List<ModelingBusinessCategoryDO> ModelingBusinessCategoryDOList = baseMapper.selectList(Wrappers.lambdaQuery(ModelingBusinessCategoryDO.class)
                .eq(ModelingBusinessCategoryDO::getValidFlag, "1")
                .orderByAsc(ModelingBusinessCategoryDO::getSortOrder));

        //组装业务分类树
        List<TreeData> treeDataList = ModelingBusinessCategoryDOList.stream()
                .map(v -> TreeData.builder()
                        .id(v.getId())
                        .parentId(v.getParentId())
                        .name(v.getName())
                        .type("1")
                        .otherData(JSONObject.of(
                                "code", v.getCode(),
                                "engName", v.getEngName()
                        ))
                        .build())
                .collect(Collectors.toList());

        Map<Long, TreeData> ModelingBusinessCategoryMap = treeDataList.stream()
                .collect(Collectors.toMap(k -> k.getId(), v -> v));

        for (TreeData treeData : treeDataList) {
            TreeData parent = ModelingBusinessCategoryMap.get(treeData.getParentId());
            if (parent != null) {
                List<TreeData> children = parent.getChildren();
                if (children == null) {
                    children = new ArrayList<>();
                    parent.setChildren(children);
                }
                children.add(treeData);
            }
        }

        //获取跟业务分类有关联的数据域
        MPJLambdaWrapperX<ModelingDataDomainDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        String statisticsSql = null;
        if (StringUtils.isNotBlank(type)) {
            switch (type) {
                case "1":
                    statisticsSql = "(SELECT COUNT(1) FROM DA_ASSET a WHERE t.ID = a.DATA_DOMAIN_ID) AS num";
                    break;
            }
            if (StringUtils.isNotBlank(statisticsSql)) {
                lambdaWrapper.select(statisticsSql);
            }
        }
        lambdaWrapper.selectAll(ModelingDataDomainDO.class)
                .selectAs(ModelingBusinessDomainRelDO::getBusinessCategoryId, "businessCategoryId")
                .innerJoin(ModelingBusinessDomainRelDO.class, ModelingBusinessDomainRelDO::getDataDomainId, ModelingDataDomainDO::getId)
                .innerJoin(ModelingBusinessCategoryDO.class, ModelingBusinessCategoryDO::getId, ModelingBusinessDomainRelDO::getBusinessCategoryId)
                .eq(ModelingBusinessCategoryDO::getValidFlag, "1");
        List<ModelingDataDomainDO> dataDomainDOList = ModelingDataDomainMapper.selectList(lambdaWrapper);

        //将数据域和业务分类关联起来
        for (ModelingDataDomainDO ModelingDataDomainDO : dataDomainDOList) {
            TreeData parent = ModelingBusinessCategoryMap.get(ModelingDataDomainDO.getBusinessCategoryId());
            if (parent != null) {
                List<TreeData> children = parent.getChildren();
                if (children == null) {
                    children = new ArrayList<>();
                    parent.setChildren(children);
                }
                children.add(TreeData.builder()
                        .id(ModelingDataDomainDO.getId())
                        .parentId(ModelingDataDomainDO.getBusinessCategoryId())
                        .name(ModelingDataDomainDO.getName())
                        .type("2")
                        .otherData(JSONObject.of(
                                "code", parent.getOtherData().getString("code"),
                                "engName", ModelingDataDomainDO.getEngName(),
                                "num", ModelingDataDomainDO.getNum()))
                        .build());
            }
        }
        return treeDataList.stream()
                .filter(dataCategoryTreeRespVO -> dataCategoryTreeRespVO.getParentId() == 0)
                .collect(Collectors.toList());
    }
}
