package com.datamaster.common.category.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.vo.CategoryPageReqVO;
import com.datamaster.common.category.vo.CategoryRespVO;
import com.datamaster.common.category.vo.CategorySaveReqVO;
import com.datamaster.common.core.page.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ITaxonomyCategoryService extends IService<TaxonomyCategoryDO> {

    PageResult<TaxonomyCategoryDO> getCategoryPage(CategoryPageReqVO pageReqVO);

    Long createCategory(CategorySaveReqVO createReqVO);

    int updateCategory(CategorySaveReqVO updateReqVO);

    int removeCategory(Collection<Long> idList, String catType);

    TaxonomyCategoryDO getCategoryById(Long id);

    List<TaxonomyCategoryDO> getCategoryList(String catType);

    List<TaxonomyCategoryDO> getCategoryList(CategoryPageReqVO reqVO);

    Map<Long, TaxonomyCategoryDO> getCategoryMap(String catType);

    String importCategory(List<CategoryRespVO> importExcelList, boolean isUpdateSupport, String operName);

    String createCode(Long parentId, String parentCode, String catType);
}
