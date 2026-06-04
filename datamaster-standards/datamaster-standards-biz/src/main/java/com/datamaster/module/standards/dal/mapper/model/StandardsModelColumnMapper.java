package com.datamaster.module.standards.dal.mapper.model;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnPageReqVO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelColumnDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 逻辑模型属性信息Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface StandardsModelColumnMapper extends BaseMapperX<StandardsModelColumnDO> {

    default PageResult<StandardsModelColumnDO> selectPage(StandardsModelColumnPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<StandardsModelColumnDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(StandardsModelColumnDO.class)
                .select("t2.NAME AS dataElemName")
                .leftJoin("STD_DATA_ELEM t2 on t.DATA_ELEM_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .eq(reqVO.getModelId() != null, StandardsModelColumnDO::getModelId, reqVO.getModelId())
                .like(com.datamaster.common.utils.StringUtils.isNotBlank(reqVO.getEngName()), StandardsModelColumnDO::getEngName, reqVO.getEngName())
                .like(com.datamaster.common.utils.StringUtils.isNotBlank(reqVO.getCnName()), StandardsModelColumnDO::getCnName, reqVO.getCnName())
                .eq(com.datamaster.common.utils.StringUtils.isNotBlank(reqVO.getColumnType()), StandardsModelColumnDO::getColumnType, reqVO.getColumnType())
                .eq(reqVO.getColumnLength() != null, StandardsModelColumnDO::getColumnLength, reqVO.getColumnLength())
                .eq(reqVO.getColumnScale() != null, StandardsModelColumnDO::getColumnScale, reqVO.getColumnScale())
                .eq(com.datamaster.common.utils.StringUtils.isNotBlank(reqVO.getDefaultValue()), StandardsModelColumnDO::getDefaultValue, reqVO.getDefaultValue())
                .eq(reqVO.getPkFlag() != null, StandardsModelColumnDO::getPkFlag, reqVO.getPkFlag())
                .eq(reqVO.getNullableFlag() != null, StandardsModelColumnDO::getNullableFlag, reqVO.getNullableFlag())
                .eq(reqVO.getSortOrder() != null, StandardsModelColumnDO::getSortOrder, reqVO.getSortOrder())
                .eq(com.datamaster.common.utils.StringUtils.isNotBlank(reqVO.getAuthorityDept()), StandardsModelColumnDO::getAuthorityDept, reqVO.getAuthorityDept())
                .eq(reqVO.getDataElemId() != null, StandardsModelColumnDO::getDataElemId, reqVO.getDataElemId())
                .eq(reqVO.getCreateTime() != null, StandardsModelColumnDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getProjectId() != null, StandardsModelColumnDO::getProjectId, reqVO.getProjectId())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()),
                        StringUtils.isNotBlank(reqVO.getOrderByColumn())
                                ? Arrays.asList(reqVO.getOrderByColumn().split(","))
                                : null);
        return selectJoinPage(reqVO, StandardsModelColumnDO.class, lambdaWrapper);
    }
}
