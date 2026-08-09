

package com.datamaster.module.governance.dal.mapper.space;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpacePageReqVO;
import com.datamaster.module.governance.dal.dataobject.space.TaxonomySpaceDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 空间Mapper接口
 *
 * @author shu
 * @date 2025-01-20
 */
public interface TaxonomySpaceMapper extends BaseMapperX<TaxonomySpaceDO> {

    default PageResult<TaxonomySpaceDO> selectPage(TaxonomySpacePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomySpaceDO>()
                .likeIfPresent(TaxonomySpaceDO::getName, reqVO.getName())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomySpaceDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
                .orderByDesc(TaxonomySpaceDO::getCreateTime));
    }

    TaxonomySpaceDO selectById(Long id);


    Page<TaxonomySpaceDO> selectSpaceListByPage(Page page,@Param("params") TaxonomySpacePageReqVO reqVO);
}
