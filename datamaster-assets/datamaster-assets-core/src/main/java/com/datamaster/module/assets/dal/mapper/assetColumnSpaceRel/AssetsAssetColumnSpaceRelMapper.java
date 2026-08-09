package com.datamaster.module.assets.dal.mapper.assetColumnSpaceRel;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumnSpaceRel.AssetsAssetColumnSpaceRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Param;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据资产字段与空间关联关系 Mapper
 *
 * @author DATAMASTER
 */
public interface AssetsAssetColumnSpaceRelMapper extends BaseMapperX<AssetsAssetColumnSpaceRelDO> {

    default PageResult<AssetsAssetColumnSpaceRelDO> selectPage(AssetsAssetColumnSpaceRelPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetColumnSpaceRelDO>()
                .eqIfPresent(AssetsAssetColumnSpaceRelDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetColumnSpaceRelDO::getColumnId, reqVO.getColumnId())
                .eqIfPresent(AssetsAssetColumnSpaceRelDO::getSpaceId, reqVO.getSpaceId())
                .eqIfPresent(AssetsAssetColumnSpaceRelDO::getSpaceCode, reqVO.getSpaceCode())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    void removeSpaceRelByColumnId(@Param("columnId") Long columnId);
}
