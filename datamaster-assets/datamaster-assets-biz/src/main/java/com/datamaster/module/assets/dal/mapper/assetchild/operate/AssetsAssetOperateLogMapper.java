package com.datamaster.module.assets.dal.mapper.assetchild.operate;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateLogDO;
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
public interface AssetsAssetOperateLogMapper extends BaseMapperX<AssetsAssetOperateLogDO> {

    default PageResult<AssetsAssetOperateLogDO> selectPage(AssetsAssetOperateLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AssetsAssetOperateLogDO>()
                .eqIfPresent(AssetsAssetOperateLogDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AssetsAssetOperateLogDO::getDatasourceId, reqVO.getDatasourceId())
                .likeIfPresent(AssetsAssetOperateLogDO::getTableName, reqVO.getTableName())
                .eqIfPresent(AssetsAssetOperateLogDO::getTableComment, reqVO.getTableComment())
                .eqIfPresent(AssetsAssetOperateLogDO::getUpdateWhereMd5, reqVO.getUpdateWhereMd5())
                .eqIfPresent(AssetsAssetOperateLogDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AssetsAssetOperateLogDO::getCreatorId, reqVO.getCreatorId())
                .eqIfPresent(AssetsAssetOperateLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AssetsAssetOperateLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    default PageResult<AssetsAssetOperateLogDO> selectPageNew(AssetsAssetOperateLogPageReqVO reqVO) {

        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        MPJLambdaWrapper<AssetsAssetOperateLogDO> lambdaWrapper = new MPJLambdaWrapper();

        lambdaWrapper.selectAll(AssetsAssetOperateLogDO.class)
                .select("u.nick_name AS nickName, u.user_name AS userName , u.phonenumber AS phoneNumber")
                .leftJoin("SYSTEM_USER u on t.user_id = u.user_id")
                .eq( reqVO.getAssetId()!=null ,AssetsAssetOperateLogDO::getAssetId, reqVO.getAssetId())
                .eq( reqVO.getDatasourceId() != null ,AssetsAssetOperateLogDO::getDatasourceId, reqVO.getDatasourceId())
                .like(StringUtils.isNotBlank(reqVO.getTableName()),AssetsAssetOperateLogDO::getTableName, reqVO.getTableName())
                .eq( StringUtils.isNotBlank(reqVO.getTableComment()) ,AssetsAssetOperateLogDO::getTableComment, reqVO.getTableComment())
                .eq( StringUtils.isNotBlank(reqVO.getUpdateWhereMd5()) ,AssetsAssetOperateLogDO::getUpdateWhereMd5, reqVO.getUpdateWhereMd5())
                .eq( StringUtils.isNotBlank(reqVO.getStatus()) ,AssetsAssetOperateLogDO::getStatus, reqVO.getStatus())
                .eq( reqVO.getCreatorId() != null  ,AssetsAssetOperateLogDO::getCreatorId, reqVO.getCreatorId())
                .between(reqVO.getStartTime() != null && reqVO.getEndTime() != null,
                        AssetsAssetOperateLogDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime());

        return selectJoinPage(reqVO, AssetsAssetOperateLogDO.class, lambdaWrapper);
    }
}
