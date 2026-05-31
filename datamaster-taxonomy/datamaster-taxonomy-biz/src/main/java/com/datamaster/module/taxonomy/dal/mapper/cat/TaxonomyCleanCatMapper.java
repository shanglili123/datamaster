

package com.datamaster.module.taxonomy.dal.mapper.cat;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyCleanCatDO;
import java.util.Arrays;

import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 清洗规则类目Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-08-11
 */
public interface TaxonomyCleanCatMapper extends BaseMapperX<TaxonomyCleanCatDO> {

    default PageResult<TaxonomyCleanCatDO> selectPage(TaxonomyCleanCatPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyCleanCatDO>()
                .likeIfPresent(TaxonomyCleanCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyCleanCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(TaxonomyCleanCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyCleanCatDO::getDescription, reqVO.getDescription())
                .eqIfPresent(TaxonomyCleanCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyCleanCatDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyCleanCatDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    @Update(value = "update TAX_CLEAN_CAT set VALID_FLAG=CASE WHEN #{validFlag} THEN '1' ELSE '0' END where code like concat(#{prefixCode}, '%')")
    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);

}
