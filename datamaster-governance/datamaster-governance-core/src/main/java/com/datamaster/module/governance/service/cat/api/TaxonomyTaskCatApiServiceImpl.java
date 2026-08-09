package com.datamaster.module.governance.service.cat.api;

import com.datamaster.common.enums.CatType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.governance.api.cat.dto.TaxonomyTaskCatReqDTO;
import com.datamaster.module.governance.api.cat.dto.TaxonomyTaskCatRespDTO;
import com.datamaster.module.governance.api.service.cat.ITaxonomyTaskCatApiService;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.service.ITaxonomyCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TaxonomyTaskCatApiServiceImpl implements ITaxonomyTaskCatApiService {

    @Resource
    private ITaxonomyCategoryService taxonomyCategoryService;

    @Override
    public List<TaxonomyTaskCatRespDTO> getAttTaskCatApiList(TaxonomyTaskCatReqDTO reqDTO) {
        List<TaxonomyCategoryDO> list = taxonomyCategoryService.getCategoryList(CatType.TASK.getValue());
        return BeanUtils.toBean(list, TaxonomyTaskCatRespDTO.class);
    }
}
