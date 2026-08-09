package com.datamaster.module.governance.api.service.cat;

import com.datamaster.module.governance.api.cat.dto.CategoryReqDTO;
import com.datamaster.module.governance.api.cat.dto.CategoryRespDTO;

import java.util.List;

public interface ITaxonomyCategoryApiService {

    List<CategoryRespDTO> getCategoryList(CategoryReqDTO reqDTO);
}
