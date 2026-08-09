package com.datamaster.module.governance.service.cat.api;

import com.datamaster.common.enums.CatType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.governance.api.cat.dto.TaxonomyDocCatReqDTO;
import com.datamaster.module.governance.api.cat.dto.TaxonomyDocCatRespDTO;
import com.datamaster.module.governance.api.service.cat.ITaxonomyDocCatApiService;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.service.ITaxonomyCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TaxonomyDocCatApiServiceImpl implements ITaxonomyDocCatApiService {

    @Resource
    private ITaxonomyCategoryService taxonomyCategoryService;

    @Override
    public List<TaxonomyDocCatRespDTO> getAttDocCatList(TaxonomyDocCatReqDTO reqDTO) {
        List<TaxonomyCategoryDO> list = taxonomyCategoryService.getCategoryList(CatType.DOCUMENT.getValue());
        return BeanUtils.toBean(list, TaxonomyDocCatRespDTO.class);
    }
}
