package com.datamaster.module.governance.service.cat.api;

import com.datamaster.common.enums.CatType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.governance.api.cat.dto.TaxonomyDataDevCatReqDTO;
import com.datamaster.module.governance.api.cat.dto.TaxonomyDataDevCatRespDTO;
import com.datamaster.module.governance.api.service.cat.ITaxonomyDataDevCatApiService;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.service.ITaxonomyCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TaxonomyDataDevCatApiServiceImpl implements ITaxonomyDataDevCatApiService {

    @Resource
    private ITaxonomyCategoryService taxonomyCategoryService;

    @Override
    public List<TaxonomyDataDevCatRespDTO> getAttDataDevCatApiList(TaxonomyDataDevCatReqDTO reqDTO) {
        List<TaxonomyCategoryDO> list = taxonomyCategoryService.getCategoryList(CatType.DATA_DEV.getValue());
        return BeanUtils.toBean(list, TaxonomyDataDevCatRespDTO.class);
    }
}
