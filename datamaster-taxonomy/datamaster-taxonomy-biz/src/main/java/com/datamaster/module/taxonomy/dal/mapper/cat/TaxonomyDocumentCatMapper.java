

package com.datamaster.module.taxonomy.dal.mapper.cat;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDocumentCatDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 标准信息分类管理Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
public interface TaxonomyDocumentCatMapper extends BaseMapperX<TaxonomyDocumentCatDO> {

    default PageResult<TaxonomyDocumentCatDO> selectPage(TaxonomyDocumentCatPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyDocumentCatDO>()
                .likeIfPresent(TaxonomyDocumentCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyDocumentCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(TaxonomyDocumentCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyDocumentCatDO::getDescription, reqVO.getDescription())
                .eqIfPresent(TaxonomyDocumentCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyDocumentCatDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyDocumentCatDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    @Update(value = "update TAX_DOCUMENT_CAT set VALID_FLAG=CASE WHEN #{validFlag} THEN '1' ELSE '0' END where code like concat(#{prefixCode}, '%')")
    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);

}
