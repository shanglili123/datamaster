

package com.datamaster.quality.dal.mapper.datasource;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.quality.controller.da.datasource.vo.DatasourcePageReqVO;
import com.datamaster.quality.dal.dataobject.datasource.DatasourceDO;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据源Mapper接口
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface DatasourceMapper extends BaseMapperX<DatasourceDO> {

    default PageResult<DatasourceDO> selectPage(DatasourcePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<DatasourceDO>()
                .likeIfPresent(DatasourceDO::getDatasourceName, reqVO.getDatasourceName())
                .inIfPresent(DatasourceDO::getDatasourceType, StringUtils.isNotEmpty(reqVO.getDatasourceType()) ? reqVO.getDatasourceType().split(",") : null)
                .eqIfPresent(DatasourceDO::getDatasourceConfig, reqVO.getDatasourceConfig())
                .eqIfPresent(DatasourceDO::getIp, reqVO.getIp())
                .eqIfPresent(DatasourceDO::getPort, reqVO.getPort())
                .eqIfPresent(DatasourceDO::getListCount, reqVO.getListCount())
                .eqIfPresent(DatasourceDO::getSyncCount, reqVO.getSyncCount())
                .eqIfPresent(DatasourceDO::getDataSize, reqVO.getDataSize())
                .eqIfPresent(DatasourceDO::getDescription, reqVO.getDescription())
                .eqIfPresent(DatasourceDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(DatasourceDO::getId, reqVO.getIdList())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(DatasourceDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    public List<DatasourceDO> getDataSourceByAsset();
}
