package com.datamaster.module.governance.service.cat.impl;

import com.datamaster.common.category.dal.mapper.TaxonomyCategoryMapper;
import com.datamaster.common.enums.CatType;
import com.datamaster.module.governance.api.cat.ITaxonomyCatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyCatServiceImpl implements ITaxonomyCatService {

    @Resource
    private TaxonomyCategoryMapper taxonomyCategoryMapper;

    @Override
    public Long getCatIdByCatTypeAndCode(String catType, String catCode) {
        return taxonomyCategoryMapper.getCatIdByCatTypeAndCode(catType, catCode);
    }

    @Override
    @Deprecated
    public Long getCatIdByTableNameAndCatCode(String tableName, String catCode) {
        String catType = mapTableNameToCatType(tableName);
        if (catType == null) {
            return null;
        }
        return taxonomyCategoryMapper.getCatIdByCatTypeAndCode(catType, catCode);
    }

    private String mapTableNameToCatType(String tableName) {
        if (tableName == null) return null;
        switch (tableName.toUpperCase()) {
            case "TAX_ASSET_CAT":       return CatType.ASSET.getValue();
            case "TAX_API_CAT":         return CatType.API.getValue();
            case "TAX_TASK_CAT":        return CatType.TASK.getValue();
            case "TAX_MODEL_CAT":       return CatType.MODEL.getValue();
            case "TAX_DATA_ELEM_CAT":   return CatType.DATA_ELEM.getValue();
            case "TAX_DATA_DEV_CAT":    return CatType.DATA_DEV.getValue();
            case "TAX_DOCUMENT_CAT":    return CatType.DOCUMENT.getValue();
            case "TAX_QUALITY_CAT":     return CatType.QUALITY.getValue();
            case "TAX_TAG_CAT":         return CatType.TAG.getValue();
            case "TAX_CLEAN_CAT":       return CatType.CLEAN.getValue();
            case "CAT":                 return CatType.TASK.getValue();
            case "TAX_JOB_CAT":         return CatType.DATA_DEV.getValue();
            default:                    return null;
        }
    }
}
