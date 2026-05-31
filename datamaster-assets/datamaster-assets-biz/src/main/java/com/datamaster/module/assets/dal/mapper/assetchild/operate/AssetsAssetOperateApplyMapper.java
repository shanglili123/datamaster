package com.datamaster.module.assets.dal.mapper.assetchild.operate;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplyPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateApplyDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
public interface AssetsAssetOperateApplyMapper extends BaseMapperX<AssetsAssetOperateApplyDO> {

    default PageResult<AssetsAssetOperateApplyDO> selectPage(AssetsAssetOperateApplyPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetOperateApplyDO>()
                .eqIfPresent(AssetsAssetOperateApplyDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetOperateApplyDO::getDatasourceId, reqVO.getDatasourceId())
                .likeIfPresent(AssetsAssetOperateApplyDO::getTableName, reqVO.getTableName())
                .eqIfPresent(AssetsAssetOperateApplyDO::getTableComment, reqVO.getTableComment())
                .eqIfPresent(AssetsAssetOperateApplyDO::getOperateType, reqVO.getOperateType())
                .eqIfPresent(AssetsAssetOperateApplyDO::getOperateJson, reqVO.getOperateJson())
                .eqIfPresent(AssetsAssetOperateApplyDO::getOperateTime, reqVO.getOperateTime())
                .eqIfPresent(AssetsAssetOperateApplyDO::getExecuteFlag, reqVO.getExecuteFlag())
                .eqIfPresent(AssetsAssetOperateApplyDO::getExecuteTime, reqVO.getExecuteTime())
                .eqIfPresent(AssetsAssetOperateApplyDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetOperateApplyDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
