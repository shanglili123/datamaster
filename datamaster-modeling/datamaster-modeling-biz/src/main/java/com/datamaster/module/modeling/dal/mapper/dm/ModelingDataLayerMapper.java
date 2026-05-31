

package com.datamaster.module.modeling.dal.mapper.dm;

import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 数仓分层管理Mapper接口
 *
 * @author FXB
 * @date 2026-03-24
 */
public interface ModelingDataLayerMapper extends BaseMapperX<ModelingDataLayerDO> {

    default PageResult<ModelingDataLayerDO> selectPage(ModelingDataLayerPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ModelingDataLayerDO>()
                .likeIfPresent(ModelingDataLayerDO::getName, reqVO.getName())
                .likeIfPresent(ModelingDataLayerDO::getEngName, reqVO.getEngName())
                .eqIfPresent(ModelingDataLayerDO::getOwnerUserId, reqVO.getOwnerUserId())
                .eqIfPresent(ModelingDataLayerDO::getCategory, reqVO.getCategory())
                .likeIfPresent(ModelingDataLayerDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelingDataLayerDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelingDataLayerDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
