

package com.datamaster.module.taxonomy.dal.mapper.cat;

import org.apache.ibatis.annotations.Param;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyModelCatDO;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 逻辑模型类目管理Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface TaxonomyModelCatMapper extends BaseMapperX<TaxonomyModelCatDO> {

    default PageResult<TaxonomyModelCatDO> selectPage(TaxonomyModelCatPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
//        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyModelCatDO>()
                .likeIfPresent(TaxonomyModelCatDO::getName, reqVO.getName())
                .likeRightIfPresent(TaxonomyModelCatDO::getCode, reqVO.getCode())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyModelCatDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
                .eq(reqVO.getProjectId() != null, TaxonomyModelCatDO::getProjectId, reqVO.getProjectId())
                .orderByAsc(TaxonomyModelCatDO::getSortOrder));
    }

    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);

}
