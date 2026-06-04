

package com.datamaster.module.modeling.dal.mapper.businessCategory;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelPageReqVO;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessDomainRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 业务分类数据域关联关系Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
public interface ModelingBusinessDomainRelMapper extends BaseMapperX<ModelingBusinessDomainRelDO> {

    default PageResult<ModelingBusinessDomainRelDO> selectPage(ModelingBusinessDomainRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ModelingBusinessDomainRelDO>()
                .eqIfPresent(ModelingBusinessDomainRelDO::getBusinessCategoryId, reqVO.getBusinessCategoryId())
                .eqIfPresent(ModelingBusinessDomainRelDO::getDataDomainId, reqVO.getDataDomainId())
                .likeIfPresent(ModelingBusinessDomainRelDO::getBusinessCategoryName, reqVO.getBusinessCategoryName())
                .likeIfPresent(ModelingBusinessDomainRelDO::getDataDomainName, reqVO.getDataDomainName())
                .eqIfPresent(ModelingBusinessDomainRelDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(ModelingBusinessDomainRelDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelingBusinessDomainRelDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(ModelingBusinessDomainRelDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(ModelingBusinessDomainRelDO::getProjectId, reqVO.getProjectId())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelingBusinessDomainRelDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
