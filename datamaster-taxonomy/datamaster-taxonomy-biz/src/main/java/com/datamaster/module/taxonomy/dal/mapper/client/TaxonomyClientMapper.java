

package com.datamaster.module.taxonomy.dal.mapper.client;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.client.TaxonomyClientDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 应用管理Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-18
 */
public interface TaxonomyClientMapper extends BaseMapperX<TaxonomyClientDO> {

    default PageResult<TaxonomyClientDO> selectPage(TaxonomyClientPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyClientDO>()
                .eqIfPresent(TaxonomyClientDO::getId, reqVO.getId())
                .likeIfPresent(TaxonomyClientDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyClientDO::getType, reqVO.getType())
                .eqIfPresent(TaxonomyClientDO::getSecret, reqVO.getSecret())
                .eqIfPresent(TaxonomyClientDO::getHomepageUrl, reqVO.getHomepageUrl())
                .eqIfPresent(TaxonomyClientDO::getAllowUrl, reqVO.getAllowUrl())
                .eqIfPresent(TaxonomyClientDO::getSyncUrl, reqVO.getSyncUrl())
                .eqIfPresent(TaxonomyClientDO::getLogo, reqVO.getLogo())
                .eqIfPresent(TaxonomyClientDO::getDescription, reqVO.getDescription())
                .eqIfPresent(TaxonomyClientDO::getPublicFlag, reqVO.getPublicFlag())
                .eqIfPresent(TaxonomyClientDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyClientDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
