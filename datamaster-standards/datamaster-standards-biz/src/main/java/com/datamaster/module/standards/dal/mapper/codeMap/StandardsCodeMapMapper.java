package com.datamaster.module.standards.dal.mapper.codeMap;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapPageReqVO;
import com.datamaster.module.standards.dal.dataobject.codeMap.StandardsCodeMapDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据元代码映射Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface StandardsCodeMapMapper extends BaseMapperX<StandardsCodeMapDO> {

    default PageResult<StandardsCodeMapDO> selectPage(StandardsCodeMapPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<StandardsCodeMapDO>()
                .eqIfPresent(StandardsCodeMapDO::getDataElemId, reqVO.getDataElemId())
                .eqIfPresent(StandardsCodeMapDO::getOriginalValue, reqVO.getOriginalValue())
                .likeIfPresent(StandardsCodeMapDO::getCodeName, reqVO.getCodeName())
                .eqIfPresent(StandardsCodeMapDO::getCodeValue, reqVO.getCodeValue())
                .eqIfPresent(StandardsCodeMapDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getProjectId() != null, StandardsCodeMapDO::getProjectId, reqVO.getProjectId())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(StandardsCodeMapDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
