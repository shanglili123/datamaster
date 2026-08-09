package com.datamaster.common.category.dal.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.vo.CategoryPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.common.core.page.PageResult;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface TaxonomyCategoryMapper extends BaseMapperX<TaxonomyCategoryDO> {

    default PageResult<TaxonomyCategoryDO> selectPage(CategoryPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyCategoryDO>()
                .likeIfPresent(TaxonomyCategoryDO::getName, reqVO.getName())
                .likeRightIfPresent(TaxonomyCategoryDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyCategoryDO::getCatType, reqVO.getCatType())
                .eq(reqVO.getValidFlag() != null, "valid_flag", Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .eq(reqVO.getSpaceId() != null, TaxonomyCategoryDO::getSpaceId, reqVO.getSpaceId())
                .orderByAsc(TaxonomyCategoryDO::getSortOrder));
    }

    default List<TaxonomyCategoryDO> selectList(CategoryPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<TaxonomyCategoryDO>()
                .likeIfPresent(TaxonomyCategoryDO::getName, reqVO.getName())
                .likeRightIfPresent(TaxonomyCategoryDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyCategoryDO::getCatType, reqVO.getCatType())
                .eq(reqVO.getValidFlag() != null, "valid_flag", Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .eq(reqVO.getSpaceId() != null, TaxonomyCategoryDO::getSpaceId, reqVO.getSpaceId())
                .orderByAsc(TaxonomyCategoryDO::getSortOrder));
    }

    @Update(value = "update TAX_CATEGORY set VALID_FLAG = CASE WHEN #{validFlag} THEN '1' ELSE '0' END where code like concat(#{prefixCode}, '%')")
    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);

    Long getCatIdByCatTypeAndCode(@Param("catType") String catType, @Param("catCode") String catCode);
}
