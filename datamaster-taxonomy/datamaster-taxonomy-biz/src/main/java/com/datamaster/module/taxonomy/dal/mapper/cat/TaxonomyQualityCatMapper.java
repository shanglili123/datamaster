

package com.datamaster.module.taxonomy.dal.mapper.cat;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyQualityCatDO;
import java.util.Arrays;

import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 数据质量类目Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
public interface TaxonomyQualityCatMapper extends BaseMapperX<TaxonomyQualityCatDO> {

    default PageResult<TaxonomyQualityCatDO> selectPage(TaxonomyQualityCatPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyQualityCatDO>()
                .likeIfPresent(TaxonomyQualityCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyQualityCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(TaxonomyQualityCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyQualityCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyQualityCatDO::getDescription, reqVO.getDescription())
                .eqIfPresent(TaxonomyQualityCatDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getValidFlag() != null, "valid_flag", Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyQualityCatDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .eq(reqVO.getProjectId() != null, TaxonomyQualityCatDO::getProjectId, reqVO.getProjectId())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    @Update(value = "update TAX_QUALITY_CAT set VALID_FLAG = CASE WHEN #{validFlag} THEN '1' ELSE '0' END where code like concat(#{prefixCode}, '%')")
    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);

}
