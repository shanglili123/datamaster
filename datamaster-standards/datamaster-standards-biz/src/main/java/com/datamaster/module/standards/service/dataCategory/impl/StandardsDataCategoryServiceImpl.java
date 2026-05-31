

package com.datamaster.module.standards.service.dataCategory.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategoryPageReqVO;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategoryRespVO;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategorySaveReqVO;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategoryTreeRespVO;
import com.datamaster.module.standards.dal.dataobject.dataCategory.StandardsDataCategoryDO;
import com.datamaster.module.standards.dal.dataobject.dataCategoryCat.StandardsDataCategoryCatDO;
import com.datamaster.module.standards.dal.dataobject.dataLevel.StandardsDataLevelDO;
import com.datamaster.module.standards.dal.mapper.dataCategory.StandardsDataCategoryMapper;
import com.datamaster.module.standards.dal.mapper.dataCategoryCat.StandardsDataCategoryCatMapper;
import com.datamaster.module.standards.service.dataCategory.IStandardsDataCategoryService;
import com.datamaster.module.standards.service.desensitizeRules.IStandardsDesensitizeRuleService;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 数据分类Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDataCategoryServiceImpl extends ServiceImpl<StandardsDataCategoryMapper, StandardsDataCategoryDO> implements IStandardsDataCategoryService {
    @Resource
    private StandardsDataCategoryMapper StandardsDataCategoryMapper;

    @Resource
    private StandardsDataCategoryCatMapper StandardsDataCategoryCatMapper;

    @Resource
    private IStandardsDesensitizeRuleService StandardsDesensitizeRuleService;

    @Override
    public PageResult<StandardsDataCategoryDO> getDgDataCategoryPage(StandardsDataCategoryPageReqVO pageReqVO) {
        return StandardsDataCategoryMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDgDataCategory(StandardsDataCategorySaveReqVO createReqVO) {
        StandardsDataCategoryDO dictType = BeanUtils.toBean(createReqVO, StandardsDataCategoryDO.class);
        StandardsDataCategoryMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDgDataCategory(StandardsDataCategorySaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据分类
        StandardsDataCategoryDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDataCategoryDO.class);
        return StandardsDataCategoryMapper.updateById(updateObj);
    }

    @Override
    public int removeDgDataCategory(Collection<Long> idList) {
        //判断在规则中是否被使用
        Long count = StandardsDesensitizeRuleService.getCountByCategoryIds(idList);
        if (count > 0) {
            throw new ServiceException("存在敏感规则，不允许删除");
        }
        // 批量删除数据分类
        return StandardsDataCategoryMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDataCategoryDO getDgDataCategoryById(Long id) {
        MPJLambdaWrapperX<StandardsDataCategoryDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper
                .select("l.short_name AS dataLevelShortName", "c.name AS catName", "(CASE WHEN r.id IS NOT NULL THEN '1' ELSE '0' END) AS desensitizationRulesFlag")
                .leftJoin("STD_DESENSITIZE_RULE r on t.id = r.DATA_CATEGORY_ID AND r.DEL_FLAG = '0'")
                .leftJoin(StandardsDataLevelDO.class, "l", StandardsDataLevelDO::getId, StandardsDataCategoryDO::getDataLevelId)
                .leftJoin(StandardsDataCategoryCatDO.class, "c", StandardsDataCategoryCatDO::getCode, StandardsDataCategoryDO::getCatCode);
        lambdaWrapper.selectAll(StandardsDataCategoryDO.class)
                .inIfPresent(StandardsDataCategoryDO::getId, id);
        return StandardsDataCategoryMapper.selectOne(lambdaWrapper);
    }

    @Override
    public List<StandardsDataCategoryDO> getDgDataCategoryList() {
        return StandardsDataCategoryMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDataCategoryDO> getDgDataCategoryMap() {
        List<StandardsDataCategoryDO> StandardsDataCategoryList = StandardsDataCategoryMapper.selectList();
        return StandardsDataCategoryList.stream()
                .collect(Collectors.toMap(
                        StandardsDataCategoryDO::getId,
                        StandardsDataCategoryDO -> StandardsDataCategoryDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据分类数据
     *
     * @param importExcelList 数据分类数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importDgDataCategory(List<StandardsDataCategoryRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (StandardsDataCategoryRespVO respVO : importExcelList) {
            try {
                StandardsDataCategoryDO StandardsDataCategoryDO = BeanUtils.toBean(respVO, StandardsDataCategoryDO.class);
                Long StandardsDataCategoryId = respVO.getId();
                if (isUpdateSupport) {
                    if (StandardsDataCategoryId != null) {
                        StandardsDataCategoryDO existingDgDataCategory = StandardsDataCategoryMapper.selectById(StandardsDataCategoryId);
                        if (existingDgDataCategory != null) {
                            StandardsDataCategoryMapper.updateById(StandardsDataCategoryDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + StandardsDataCategoryId + " 的数据分类记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + StandardsDataCategoryId + " 的数据分类记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<StandardsDataCategoryDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", StandardsDataCategoryId);
                    StandardsDataCategoryDO existingDgDataCategory = StandardsDataCategoryMapper.selectOne(queryWrapper);
                    if (existingDgDataCategory == null) {
                        StandardsDataCategoryMapper.insert(StandardsDataCategoryDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + StandardsDataCategoryId + " 的数据分类记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + StandardsDataCategoryId + " 的数据分类记录已存在。");
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
    public List<StandardsDataCategoryTreeRespVO> selectTree(String type) {
        //获取分类类目数据
        List<StandardsDataCategoryCatDO> dataCategoryCatDOList = StandardsDataCategoryCatMapper.selectList();
        List<StandardsDataCategoryTreeRespVO> dataCategoryTreeRespVOList = dataCategoryCatDOList.stream()
                .map(dataCategoryCatDO -> {
                    return StandardsDataCategoryTreeRespVO.builder()
                            .id(dataCategoryCatDO.getId())
                            .parentId(dataCategoryCatDO.getParentId())
                            .name(dataCategoryCatDO.getName())
                            .type("1")
                            .catCode(dataCategoryCatDO.getCode())
                            .build();
                })
                .collect(Collectors.toList());

        Map<Long, StandardsDataCategoryTreeRespVO> dataCategoryCatIdMap = dataCategoryTreeRespVOList.stream().collect(Collectors.toMap(k -> k.getId(), v -> v));

        for (StandardsDataCategoryTreeRespVO StandardsDataCategoryTreeRespVO : dataCategoryTreeRespVOList) {
            StandardsDataCategoryTreeRespVO parent = dataCategoryCatIdMap.get(StandardsDataCategoryTreeRespVO.getParentId());
            if (parent != null) {
                List<StandardsDataCategoryTreeRespVO> children = parent.getChildren();
                if (children == null) {
                    children = new ArrayList<>();
                    parent.setChildren(children);
                }
                children.add(StandardsDataCategoryTreeRespVO);
            }
        }
        Map<String, StandardsDataCategoryTreeRespVO> dataCategoryCatCodeMap = dataCategoryTreeRespVOList.stream().collect(Collectors.toMap(k -> k.getCatCode(), v -> v));

        //获取所有有效的分类


        MPJLambdaWrapperX<StandardsDataCategoryDO> lambdaWrapper = new MPJLambdaWrapperX<>();

        String subQuery = "(CASE WHEN r.id IS NOT NULL THEN '1' ELSE '0' END) AS desensitizationRulesFlag";
        if (StringUtils.equals("2", type)) {
            subQuery = "(CASE WHEN w.id IS NOT NULL THEN '1' ELSE '0' END) AS desensitizationRulesFlag";
        }
        lambdaWrapper.selectAll(StandardsDataCategoryDO.class)
                .select(subQuery)
                .leftJoin("STD_DESENSITIZE_RULE r on t.id = r.DATA_CATEGORY_ID AND r.DEL_FLAG = '0'")
                .leftJoin("STD_DESENSITIZE_WHITELIST w on t.id = w.DATA_CATEGORY_ID AND w.DEL_FLAG = '0'")
                .eq(StandardsDataCategoryDO::getValidFlag, "1");
        List<StandardsDataCategoryDO> dataCategoryDOList = StandardsDataCategoryMapper.selectList(lambdaWrapper);

        for (StandardsDataCategoryDO StandardsDataCategoryDO : dataCategoryDOList) {
            StandardsDataCategoryTreeRespVO StandardsDataCategoryTreeRespVO = dataCategoryCatCodeMap.get(StandardsDataCategoryDO.getCatCode());
            if (StandardsDataCategoryTreeRespVO != null) {
                List<StandardsDataCategoryTreeRespVO> child = StandardsDataCategoryTreeRespVO.getChildren();
                if (child == null) {
                    child = new ArrayList<>();
                    StandardsDataCategoryTreeRespVO.setChildren(child);
                }
                child.add(StandardsDataCategoryTreeRespVO.builder()
                        .id(StandardsDataCategoryDO.getId())
                        .name(StandardsDataCategoryDO.getName())
                        .type("2")
                        .desensitizationRulesFlag(StandardsDataCategoryDO.getDesensitizationRulesFlag())
                        .build());
            }
        }

        return dataCategoryTreeRespVOList.stream()
                .filter(dataCategoryTreeRespVO -> dataCategoryTreeRespVO.getParentId() == 0)
                .collect(Collectors.toList());
    }

    @Override
    public Long getCountByCatCode(String catCode) {
        return baseMapper.selectCount(Wrappers.lambdaQuery(StandardsDataCategoryDO.class)
                .likeRight(StandardsDataCategoryDO::getCatCode, catCode));
    }

    @Override
    public int updateCatCode(String oldCatCode, String newCatCode) {
        return baseMapper.update(
                null,
                Wrappers.<StandardsDataCategoryDO>lambdaUpdate()
                        .set(StandardsDataCategoryDO::getCatCode, newCatCode)
                        .eq(StandardsDataCategoryDO::getCatCode, oldCatCode)
        );
    }

    @Override
    public List<StandardsDataCategoryDO> getDgDataCategoryList(StandardsDataCategoryPageReqVO StandardsDataCategory) {
         //根据参数 StandardsDataCategory 只查询validFlag为true的 数据分类列表不分页
       List<StandardsDataCategoryDO> dataCategoryDOList = StandardsDataCategoryMapper.selectList(Wrappers.lambdaQuery(StandardsDataCategoryDO.class)
                .eq(StandardsDataCategoryDO::getValidFlag, "1"));
       return dataCategoryDOList;

    }
}
