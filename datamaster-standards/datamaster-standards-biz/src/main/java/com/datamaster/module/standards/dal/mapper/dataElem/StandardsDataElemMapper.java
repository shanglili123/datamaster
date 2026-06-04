package com.datamaster.module.standards.dal.mapper.dataElem;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemDO;

import java.util.*;

import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;

import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 数据元Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface StandardsDataElemMapper extends BaseMapperX<StandardsDataElemDO> {

    default PageResult<StandardsDataElemDO> selectPage(StandardsDataElemPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<StandardsDataElemDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(StandardsDataElemDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_DATA_ELEM_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getName()), StandardsDataElemDO::getName, reqVO.getName())
                .like(StringUtils.isNotBlank(reqVO.getEngName()), StandardsDataElemDO::getEngName, reqVO.getEngName())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), StandardsDataElemDO::getCatCode, reqVO.getCatCode())
                .eq(StringUtils.isNotBlank(reqVO.getType()), StandardsDataElemDO::getType, reqVO.getType())
                .eq(StringUtils.isNotBlank(reqVO.getColumnType()), StandardsDataElemDO::getColumnType, reqVO.getColumnType())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), StandardsDataElemDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getDocumentId()!= null, StandardsDataElemDO::getDocumentId, reqVO.getDocumentId())
                .eq(reqVO.getProjectId() != null, StandardsDataElemDO::getProjectId, reqVO.getProjectId())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return selectJoinPage(reqVO, StandardsDataElemDO.class, lambdaWrapper);
    }

    /**
     * 判断当前元数据是否被模型及资产使用
     *
     * @param idList
     * @return
     */
    Long checkHasRel(@Param("idList") List<Long> idList);
}
