package com.datamaster.module.assets.controller.admin.assetColumn.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemRuleRelRespDTO;

@Data
@AllArgsConstructor
public class AssetsAssetColumnRelRuleVO {

    private AssetsAssetColumnDO assetColumn;
    private StandardsDataElemRuleRelRespDTO elemRuleRel;

}
