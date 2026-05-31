package com.datamaster.module.standards.dal.mapper.dataElem;


import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodePageReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemCodeDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据元代码Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface StandardsDataElemCodeMapper extends BaseMapperX<StandardsDataElemCodeDO> {

    default PageResult<StandardsDataElemCodeDO> selectPage(StandardsDataElemCodePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<StandardsDataElemCodeDO>()
                .eqIfPresent(StandardsDataElemCodeDO::getDataElemId, reqVO.getDataElemId())
                .eqIfPresent(StandardsDataElemCodeDO::getCodeValue, reqVO.getCodeValue())
                .likeIfPresent(StandardsDataElemCodeDO::getCodeName, reqVO.getCodeName())
                .eqIfPresent(StandardsDataElemCodeDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(StandardsDataElemCodeDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
