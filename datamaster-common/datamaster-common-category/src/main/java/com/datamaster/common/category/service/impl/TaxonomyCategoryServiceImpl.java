package com.datamaster.common.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.dal.mapper.TaxonomyCategoryMapper;
import com.datamaster.common.category.service.ITaxonomyCategoryService;
import com.datamaster.common.category.validator.CategoryValidator;
import com.datamaster.common.category.vo.CategoryPageReqVO;
import com.datamaster.common.category.vo.CategoryRespVO;
import com.datamaster.common.category.vo.CategorySaveReqVO;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyCategoryServiceImpl extends ServiceImpl<TaxonomyCategoryMapper, TaxonomyCategoryDO> implements ITaxonomyCategoryService {

    @Resource
    private List<CategoryValidator> validators;

    private Map<String, CategoryValidator> validatorMap;

    @PostConstruct
    public void init() {
        validatorMap = validators != null
                ? validators.stream().collect(Collectors.toMap(CategoryValidator::catType, v -> v))
                : Collections.emptyMap();
    }

    @Override
    public PageResult<TaxonomyCategoryDO> getCategoryPage(CategoryPageReqVO pageReqVO) {
        return baseMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCategory(CategorySaveReqVO createReqVO) {
        TaxonomyCategoryDO category = BeanUtils.toBean(createReqVO, TaxonomyCategoryDO.class);
        category.setCode(createCode(createReqVO.getParentId(), null, createReqVO.getCatType()));
        baseMapper.insert(category);
        return category.getId();
    }

    @Override
    public int updateCategory(CategorySaveReqVO updateReqVO) {
        TaxonomyCategoryDO catDO = baseMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        CategoryValidator validator = validatorMap.get(catDO.getCatType());
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            if (validator != null) {
                validator.validateDisable(catDO.getCode());
            }
            baseMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            TaxonomyCategoryDO parent = baseMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
            if (validator != null) {
                validator.validateEnable(catDO.getId(), catDO.getParentId());
            }
        }
        TaxonomyCategoryDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyCategoryDO.class);
        return baseMapper.updateById(updateObj);
    }

    @Override
    public int removeCategory(Collection<Long> idList, String catType) {
        int count = 0;
        CategoryValidator validator = validatorMap.get(catType);
        for (Long id : idList) {
            TaxonomyCategoryDO cat = baseMapper.selectById(id);
            if (cat == null) {
                continue;
            }
            if (validator != null) {
                validator.validateDelete(cat.getCode());
            }
            count += baseMapper.delete(Wrappers.lambdaQuery(TaxonomyCategoryDO.class)
                    .eq(TaxonomyCategoryDO::getCatType, catType)
                    .likeRight(TaxonomyCategoryDO::getCode, cat.getCode()));
        }
        return count;
    }

    @Override
    public TaxonomyCategoryDO getCategoryById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<TaxonomyCategoryDO> getCategoryList(String catType) {
        return baseMapper.selectList(TaxonomyCategoryDO::getCatType, catType);
    }

    @Override
    public List<TaxonomyCategoryDO> getCategoryList(CategoryPageReqVO reqVO) {
        return baseMapper.selectList(reqVO);
    }

    @Override
    public Map<Long, TaxonomyCategoryDO> getCategoryMap(String catType) {
        List<TaxonomyCategoryDO> list = getCategoryList(catType);
        return list.stream()
                .collect(Collectors.toMap(
                        TaxonomyCategoryDO::getId,
                        item -> item,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public String importCategory(List<CategoryRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CategoryRespVO respVO : importExcelList) {
            try {
                TaxonomyCategoryDO category = BeanUtils.toBean(respVO, TaxonomyCategoryDO.class);
                Long categoryId = respVO.getId();
                if (isUpdateSupport) {
                    if (categoryId != null) {
                        TaxonomyCategoryDO existing = baseMapper.selectById(categoryId);
                        if (existing != null) {
                            baseMapper.updateById(category);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + categoryId + " 的类目记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + categoryId + " 的类目记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    TaxonomyCategoryDO existing = baseMapper.selectById(categoryId);
                    if (existing == null) {
                        baseMapper.insert(category);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + categoryId + " 的类目记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + categoryId + " 的类目记录已存在。");
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
    public String createCode(Long parentId, String parentCode, String catType) {
        String categoryCode;
        LambdaQueryWrapper<TaxonomyCategoryDO> query = new LambdaQueryWrapper<TaxonomyCategoryDO>()
                .eq(TaxonomyCategoryDO::getCatType, catType)
                .eq(TaxonomyCategoryDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyCategoryDO::getCode, parentCode)
                .isNotNull(TaxonomyCategoryDO::getCode)
                .orderByDesc(TaxonomyCategoryDO::getCode);
        List<TaxonomyCategoryDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                TaxonomyCategoryDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }

}
