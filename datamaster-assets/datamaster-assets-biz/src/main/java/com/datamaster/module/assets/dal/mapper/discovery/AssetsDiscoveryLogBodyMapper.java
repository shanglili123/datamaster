package com.datamaster.module.assets.dal.mapper.discovery;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryLogBodyPageReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryLogBodyDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * -Mapper
 *
 * @author DATAMASTER
 * @date 2025-10-15
 */
public interface AssetsDiscoveryLogBodyMapper extends BaseMapperX<AssetsDiscoveryLogBodyDO> {

    default PageResult<AssetsDiscoveryLogBodyDO> selectPage(AssetsDiscoveryLogBodyPageReqVO reqVO) {
        // 允许排序字段，防止 SQL 注入
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("tm", "task_id", "create_time", "update_time"));

        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsDiscoveryLogBodyDO>()
                .eqIfPresent(AssetsDiscoveryLogBodyDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AssetsDiscoveryLogBodyDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(AssetsDiscoveryLogBodyDO::getDelFlag, reqVO.getDelFlag())
                .likeIfPresent(AssetsDiscoveryLogBodyDO::getLogContent, reqVO.getLogContent())
                .betweenIfPresent(AssetsDiscoveryLogBodyDO::getTm, reqVO.getBeginTm(), reqVO.getEndTm())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
