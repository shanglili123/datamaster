

package com.datamaster.module.taxonomy.dal.mapper.theme;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemePageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.theme.TaxonomyThemeDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 主题Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface TaxonomyThemeMapper extends BaseMapperX<TaxonomyThemeDO> {

    default PageResult<TaxonomyThemeDO> selectPage(TaxonomyThemePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id","sort_order", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyThemeDO>()
                .likeIfPresent(TaxonomyThemeDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyThemeDO::getIcon, reqVO.getIcon())
                .eqIfPresent(TaxonomyThemeDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyThemeDO::getDescription, reqVO.getDescription())
                .eqIfPresent(TaxonomyThemeDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyThemeDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
                //按照 createTime 字段降序排序 sort_order升序
                .eq(reqVO.getProjectId() != null, TaxonomyThemeDO::getProjectId, reqVO.getProjectId())
                .orderByAsc(TaxonomyThemeDO::getSortOrder)
                .orderByDesc(TaxonomyThemeDO::getCreateTime));


    }
}
