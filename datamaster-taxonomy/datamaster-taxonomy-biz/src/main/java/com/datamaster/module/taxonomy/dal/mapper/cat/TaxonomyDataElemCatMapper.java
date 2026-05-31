

package com.datamaster.module.taxonomy.dal.mapper.cat;

import org.apache.ibatis.annotations.Param;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDataElemCatDO;

import com.datamaster.common.core.page.PageResult;

import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 数据元类目管理Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface TaxonomyDataElemCatMapper extends BaseMapperX<TaxonomyDataElemCatDO> {

    default PageResult<TaxonomyDataElemCatDO> selectPage(TaxonomyDataElemCatPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
//        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyDataElemCatDO>()
                .likeIfPresent(TaxonomyDataElemCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyDataElemCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(TaxonomyDataElemCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyDataElemCatDO::getDescription, reqVO.getDescription())
                .likeRightIfPresent(TaxonomyDataElemCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyDataElemCatDO::getCreateTime, reqVO.getCreateTime())

                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyDataElemCatDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
                .orderByAsc(TaxonomyDataElemCatDO::getSortOrder));
    }


    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);

}
