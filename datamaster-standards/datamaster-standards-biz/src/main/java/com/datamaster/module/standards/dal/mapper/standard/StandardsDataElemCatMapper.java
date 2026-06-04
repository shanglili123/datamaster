package com.datamaster.module.standards.dal.mapper.standard;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemCatPageReqVO;
import com.datamaster.module.standards.dal.dataobject.standard.StandardsDataElemCatDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据元类目管理Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface StandardsDataElemCatMapper extends BaseMapperX<StandardsDataElemCatDO> {

    default List<StandardsDataElemCatDO> selectList(StandardsDataElemCatPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time", "sort_order"));
        if (StringUtils.isBlank(reqVO.getOrderByColumn())) {
            reqVO.setOrderByColumn("sort_order");
            reqVO.setIsAsc("asc");
        }
        LambdaQueryWrapperX<StandardsDataElemCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(StandardsDataElemCatDO::getName, reqVO.getName())
                .eqIfPresent(StandardsDataElemCatDO::getParentId, reqVO.getParentId())
                .eq(reqVO.getValidFlag() != null, "valid_flag", Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .eqIfPresent(StandardsDataElemCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(StandardsDataElemCatDO::getDescription, reqVO.getDescription())
                .likeRightIfPresent(StandardsDataElemCatDO::getCode, reqVO.getCode())
                .eqIfPresent(StandardsDataElemCatDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getProjectId() != null, StandardsDataElemCatDO::getProjectId, reqVO.getProjectId())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns)
                .orderBy(!reqVO.getOrderByColumn().contains("create_time") && !reqVO.getOrderByColumn().contains("createTime"),
                        false, BaseEntity::getCreateTime);
        return selectList(queryWrapperX);
    }

    @Update("update STD_DATA_ELEM_CAT set VALID_FLAG=CASE WHEN #{validFlag} THEN '1' ELSE '0' END where code like concat(#{prefixCode}, '%')")
    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);

}
