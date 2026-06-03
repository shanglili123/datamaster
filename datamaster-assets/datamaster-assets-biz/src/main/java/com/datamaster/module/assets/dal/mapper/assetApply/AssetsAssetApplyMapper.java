package com.datamaster.module.assets.dal.mapper.assetApply;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.collections4.CollectionUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplyPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetApply.AssetsAssetApplyDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;

/**
 * Mapper
 *
 * @author shu
 * @date 2025-03-19
 */
public interface AssetsAssetApplyMapper extends BaseMapperX<AssetsAssetApplyDO> {

    default PageResult<AssetsAssetApplyDO> selectPage(AssetsAssetApplyPageReqVO reqVO) {
        MPJLambdaWrapper<AssetsAssetApplyDO> lambdaWrapper = new MPJLambdaWrapper<>();
        lambdaWrapper.selectAll(AssetsAssetApplyDO.class)
                .select("t2.NAME AS assetName, t2.TABLE_NAME AS assetTableName, t5.NAME AS catAssetName, t5.CODE AS carAssetCode, t3.NAME AS projectName")
                .leftJoin("AST_ASSET t2 on t.ASSET_ID = t2.ID")
                .leftJoin("TAX_PROJECT t3 on t.PROJECT_ID = t3.ID AND t3.DEL_FLAG = '0'")
                .leftJoin("TAX_ASSET_CAT t5 on t2.CAT_CODE = t5.CODE AND t5.DEL_FLAG = '0'")
                .eq("t2.DEL_FLAG", "0")
                .in(CollectionUtils.isNotEmpty(reqVO.getAssetIds()), "t2.ID", reqVO.getAssetIds())
                .like(StringUtils.isNotEmpty(reqVO.getAssetName()), "t2.NAME", reqVO.getAssetName())
                .like(StringUtils.isNotEmpty(reqVO.getCreateBy()), AssetsAssetApplyDO::getCreateBy, reqVO.getCreateBy())
                .eq(StringUtils.isNotEmpty(reqVO.getStatus()), AssetsAssetApplyDO::getStatus, reqVO.getStatus())
                .eq(StringUtils.isNotEmpty(reqVO.getCatAssetCode()), "t5.CODE", reqVO.getCatAssetCode())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()),
                        StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return selectJoinPage(reqVO, AssetsAssetApplyDO.class, lambdaWrapper);
    }
}
