package com.datamaster.module.catalog.dal.mapper.domain;

import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.catalog.controller.admin.domain.vo.CatalogDomainPageReqVO;
import com.datamaster.module.catalog.dal.dataobject.domain.CatalogDomainDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 业务域Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-02-12
 */
public interface CatalogDomainMapper extends BaseMapperX<CatalogDomainDO> {

    default PageResult<CatalogDomainDO> selectPage(CatalogDomainPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<CatalogDomainDO>()
                .likeIfPresent(CatalogDomainDO::getName, reqVO.getName())
                .eqIfPresent(CatalogDomainDO::getParentId, reqVO.getParentId())
                .eqIfPresent(CatalogDomainDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(CatalogDomainDO::getCode, reqVO.getCode())
                .eqIfPresent(CatalogDomainDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(CatalogDomainDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(CatalogDomainDO::getRemark, reqVO.getRemark())
                .eqIfPresent(CatalogDomainDO::getDescription, reqVO.getDescription())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(CatalogDomainDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    default List<CatalogDomainDO> selectList(CatalogDomainPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time", "sort_order"));
        if (StringUtils.isBlank(reqVO.getOrderByColumn())) {
            reqVO.setOrderByColumn("sort_order");
            reqVO.setIsAsc("asc");
        }
        LambdaQueryWrapperX<CatalogDomainDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(CatalogDomainDO::getName, reqVO.getName())
                .eqIfPresent(CatalogDomainDO::getParentId, reqVO.getParentId())
                .eqIfPresent(CatalogDomainDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(CatalogDomainDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(CatalogDomainDO::getDescription, reqVO.getDescription())
                .likeRightIfPresent(CatalogDomainDO::getCode, reqVO.getCode())
                .eqIfPresent(CatalogDomainDO::getCreateTime, reqVO.getCreateTime())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns);
        // FIXME 迁移时候存在问题
        //queryWrapperX.orderBy(!reqVO.getOrderByColumn().contains("create_time") && !reqVO.getOrderByColumn().contains("createTime"),
        //        false, BaseEntity::getCreateTime);
        return selectList(queryWrapperX);
    }

    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);

}
