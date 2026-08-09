package com.datamaster.module.governance.service.cat.api;

import com.datamaster.common.enums.CatType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.governance.api.cat.dto.TaxonomyApiCatReqDTO;
import com.datamaster.module.governance.api.cat.dto.TaxonomyApiCatRespDTO;
import com.datamaster.module.governance.api.service.cat.ITaxonomyApiCatApiService;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.service.ITaxonomyCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TaxonomyApiCatApiServiceImpl implements ITaxonomyApiCatApiService {

    @Resource
    private ITaxonomyCategoryService taxonomyCategoryService;

    @Override
    public List<TaxonomyApiCatRespDTO> getAttApiCatList(TaxonomyApiCatReqDTO reqDTO) {
        List<TaxonomyCategoryDO> list = taxonomyCategoryService.getCategoryList(CatType.API.getValue());
        return BeanUtils.toBean(list, TaxonomyApiCatRespDTO.class);
    }
}
