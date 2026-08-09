package com.datamaster.common.category.convert;

import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.vo.CategoryRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface CategoryConvert {

    CategoryConvert INSTANCE = Mappers.getMapper(CategoryConvert.class);

    TaxonomyCategoryDO convert(CategoryRespVO bean);

    CategoryRespVO convert(TaxonomyCategoryDO bean);

    List<CategoryRespVO> convertToRespVOList(List<TaxonomyCategoryDO> list);
}
