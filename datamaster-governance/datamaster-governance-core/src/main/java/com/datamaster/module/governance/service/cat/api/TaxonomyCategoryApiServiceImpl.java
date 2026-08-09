package com.datamaster.module.governance.service.cat.api;

import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.governance.api.cat.dto.CategoryReqDTO;
import com.datamaster.module.governance.api.cat.dto.CategoryRespDTO;
import com.datamaster.module.governance.api.service.cat.ITaxonomyCategoryApiService;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.service.ITaxonomyCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TaxonomyCategoryApiServiceImpl implements ITaxonomyCategoryApiService {

    @Resource
    private ITaxonomyCategoryService taxonomyCategoryService;

    @Override
    public List<CategoryRespDTO> getCategoryList(CategoryReqDTO reqDTO) {
        List<TaxonomyCategoryDO> list = taxonomyCategoryService.getCategoryList(reqDTO.getCatType());
        return BeanUtils.toBean(list, CategoryRespDTO.class);
    }
}
