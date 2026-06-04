

package com.datamaster.module.taxonomy.dal.mapper.Rel;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.Rel.TaxonomyTagAssetRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 标签与资产关联关系Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
public interface TaxonomyTagAssetRelMapper extends BaseMapperX<TaxonomyTagAssetRelDO> {

    default PageResult<TaxonomyTagAssetRelDO> selectPage(TaxonomyTagAssetRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyTagAssetRelDO>()
                .eqIfPresent(TaxonomyTagAssetRelDO::getTagId, reqVO.getTagId())
                .eqIfPresent(TaxonomyTagAssetRelDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(TaxonomyTagAssetRelDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyTagAssetRelDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .eq(reqVO.getProjectId() != null, TaxonomyTagAssetRelDO::getProjectId, reqVO.getProjectId())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
