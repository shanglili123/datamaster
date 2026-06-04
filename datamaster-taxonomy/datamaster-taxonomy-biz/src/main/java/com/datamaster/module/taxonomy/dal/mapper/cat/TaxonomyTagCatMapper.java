

package com.datamaster.module.taxonomy.dal.mapper.cat;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTagCatDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 标签类目管理Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
public interface TaxonomyTagCatMapper extends BaseMapperX<TaxonomyTagCatDO> {

    default PageResult<TaxonomyTagCatDO> selectPage(TaxonomyTagCatPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyTagCatDO>()
                .likeIfPresent(TaxonomyTagCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyTagCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(TaxonomyTagCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyTagCatDO::getDescription, reqVO.getDescription())
                .likeRightIfPresent(TaxonomyTagCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyTagCatDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyTagCatDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .eq(reqVO.getProjectId() != null, TaxonomyTagCatDO::getProjectId, reqVO.getProjectId())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    @Update(value = "update TAX_TAG_CAT set VALID_FLAG=CASE WHEN #{validFlag} THEN '1' ELSE '0' END where code like concat(#{prefixCode}, '%')")
    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);

}
