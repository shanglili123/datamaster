package com.datamaster.module.standards.dal.mapper.standard;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.module.standards.dal.dataobject.standard.StandardsDataMetaDO;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据元Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface StandardsDataMetaMapper extends BaseMapperX<StandardsDataMetaDO> {

    default PageResult<StandardsDataMetaDO> selectPage(StandardsDataElemPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapperX<StandardsDataMetaDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper.selectAll(StandardsDataMetaDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("STD_DATA_ELEM_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getName()), StandardsDataMetaDO::getName, reqVO.getName())
                .like(StringUtils.isNotBlank(reqVO.getEngName()), StandardsDataMetaDO::getEngName, reqVO.getEngName())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), StandardsDataMetaDO::getCatCode, reqVO.getCatCode())
                .eq(StringUtils.isNotBlank(reqVO.getType()), StandardsDataMetaDO::getType, reqVO.getType())
                .eq(StringUtils.isNotBlank(reqVO.getColumnType()), StandardsDataMetaDO::getColumnType, reqVO.getColumnType())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), StandardsDataMetaDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getDocumentId() != null, StandardsDataMetaDO::getDocumentId, reqVO.getDocumentId());
        lambdaWrapper.orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns);
        return selectJoinPage(reqVO, StandardsDataMetaDO.class, lambdaWrapper);
    }

    default List<StandardsDataMetaDO> selectList(StandardsDataElemPageReqVO reqVO) {
        LambdaQueryWrapperX<StandardsDataMetaDO> queryWrapper = new LambdaQueryWrapperX<>();
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        queryWrapper.likeIfPresent(StandardsDataMetaDO::getName, reqVO.getName())
                .likeIfPresent(StandardsDataMetaDO::getEngName, reqVO.getEngName())
                .eqIfPresent(StandardsDataMetaDO::getCatCode, reqVO.getCatCode())
                .eqIfPresent(StandardsDataMetaDO::getType, reqVO.getType())
                .eqIfPresent(StandardsDataMetaDO::getPersonCharge, reqVO.getPersonCharge())
                .eqIfPresent(StandardsDataMetaDO::getContactNumber, reqVO.getContactNumber())
                .eqIfPresent(StandardsDataMetaDO::getColumnType, reqVO.getColumnType())
                .eqIfPresent(StandardsDataMetaDO::getStatus, reqVO.getStatus())
                .eqIfPresent(StandardsDataMetaDO::getDocumentId, reqVO.getDocumentId())
                .eqIfPresent(StandardsDataMetaDO::getDescription, reqVO.getDescription())
                .eqIfPresent(StandardsDataMetaDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(StandardsDataElemDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns);
        return selectList(queryWrapper);
    }

    /**
     * 判断当前元数据是否被模型及资产使用
     *
     * @param idList
     * @return
     */
    Long checkHasRel(@Param("idList") List<Long> idList);

    default boolean existsByCatCode(String catCode) {
        return exists(Wrappers.lambdaQuery(StandardsDataMetaDO.class)
                .likeRight(StandardsDataMetaDO::getCatCode, catCode));
    }

    /**
     * 将老的 CAT_CODE 批量更新成新的 CAT_CODE
     *
     * @param oldCatCode 旧分类编码
     * @param newCatCode 新分类编码
     * @return 受影响行数
     */
    default int updateCatCode(String oldCatCode, String newCatCode) {
        return this.update(
                null,
                Wrappers.<StandardsDataMetaDO>lambdaUpdate()
                        .set(StandardsDataMetaDO::getCatCode, newCatCode)
                        .eq(StandardsDataMetaDO::getDelFlag, "0")
                        .eq(StandardsDataMetaDO::getCatCode, oldCatCode)
        );
    }

}
