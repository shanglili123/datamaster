

package com.datamaster.module.taxonomy.dal.mapper.cat;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTaskCatDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据集成任务类目管理Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
public interface TaxonomyTaskCatMapper extends BaseMapperX<TaxonomyTaskCatDO> {

    default PageResult<TaxonomyTaskCatDO> selectPage(TaxonomyTaskCatPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyTaskCatDO>()
                .likeIfPresent(TaxonomyTaskCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyTaskCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(TaxonomyTaskCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyTaskCatDO::getDescription, reqVO.getDescription())
                .eqIfPresent(TaxonomyTaskCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyTaskCatDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(TaxonomyTaskCatDO::getProjectId,reqVO.getProjectId())
                .eqIfPresent(TaxonomyTaskCatDO::getProjectCode,reqVO.getProjectCode())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyTaskCatDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
