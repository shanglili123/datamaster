package com.datamaster.module.assets.dal.mapper.assetColumnProjectRel;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo.AssetsAssetColumnProjectRelPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumnProjectRel.AssetsAssetColumnProjectRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Param;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据资产字段与项目关联关系 Mapper
 *
 * @author DATAMASTER
 */
public interface AssetsAssetColumnProjectRelMapper extends BaseMapperX<AssetsAssetColumnProjectRelDO> {

    default PageResult<AssetsAssetColumnProjectRelDO> selectPage(AssetsAssetColumnProjectRelPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetColumnProjectRelDO>()
                .eqIfPresent(AssetsAssetColumnProjectRelDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetColumnProjectRelDO::getColumnId, reqVO.getColumnId())
                .eqIfPresent(AssetsAssetColumnProjectRelDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(AssetsAssetColumnProjectRelDO::getProjectCode, reqVO.getProjectCode())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    void removeProjectRelByColumnId(@Param("columnId") Long columnId);
}
