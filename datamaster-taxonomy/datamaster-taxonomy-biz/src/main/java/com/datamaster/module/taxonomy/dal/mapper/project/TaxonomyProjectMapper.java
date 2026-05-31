

package com.datamaster.module.taxonomy.dal.mapper.project;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 项目Mapper接口
 *
 * @author shu
 * @date 2025-01-20
 */
public interface TaxonomyProjectMapper extends BaseMapperX<TaxonomyProjectDO> {

    default PageResult<TaxonomyProjectDO> selectPage(TaxonomyProjectPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyProjectDO>()
                .likeIfPresent(TaxonomyProjectDO::getName, reqVO.getName())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyProjectDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
                .orderByDesc(TaxonomyProjectDO::getCreateTime));
    }

    TaxonomyProjectDO selectById(Long id);


    Page<TaxonomyProjectDO> selectAttProjectListByPage(Page page,@Param("params") TaxonomyProjectPageReqVO reqVO);
}
