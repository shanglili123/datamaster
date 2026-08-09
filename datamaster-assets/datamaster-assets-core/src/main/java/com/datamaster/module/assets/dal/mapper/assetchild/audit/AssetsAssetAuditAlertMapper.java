package com.datamaster.module.assets.dal.mapper.assetchild.audit;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditAlertDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * -Mapper
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
public interface AssetsAssetAuditAlertMapper extends BaseMapperX<AssetsAssetAuditAlertDO> {

    default PageResult<AssetsAssetAuditAlertDO> selectPage(AssetsAssetAuditAlertPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetAuditAlertDO>()
                .eqIfPresent(AssetsAssetAuditAlertDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetAuditAlertDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(AssetsAssetAuditAlertDO::getAuditTime, reqVO.getAuditTime())
                .eqIfPresent(AssetsAssetAuditAlertDO::getAlertTime, reqVO.getAlertTime())
                .eqIfPresent(AssetsAssetAuditAlertDO::getAlertMessage, reqVO.getAlertMessage())
                .eqIfPresent(AssetsAssetAuditAlertDO::getAlertChannels, reqVO.getAlertChannels())
                .eqIfPresent(AssetsAssetAuditAlertDO::getAlertChannelResult, reqVO.getAlertChannelResult())
                .eqIfPresent(AssetsAssetAuditAlertDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetAuditAlertDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
