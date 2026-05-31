

package com.datamaster.module.standards.dal.mapper.desensitizeList;

import org.apache.commons.lang3.StringUtils;
import com.datamaster.module.standards.dal.dataobject.dataCategory.StandardsDataCategoryDO;
import com.datamaster.module.standards.dal.dataobject.desensitizeList.StandardsDesensitizeAssetcolumnDO;

import java.util.Arrays;

import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;

import java.util.HashSet;
import java.util.Set;

import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnPageReqVO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeWhitelistDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 脱敏清单关联关系Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
public interface StandardsDesensitizeAssetcolumnMapper extends BaseMapperX<StandardsDesensitizeAssetcolumnDO> {

    default PageResult<StandardsDesensitizeAssetcolumnDO> selectPage(StandardsDesensitizeAssetcolumnPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        MPJLambdaWrapperX<StandardsDesensitizeAssetcolumnDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper.selectAll(StandardsDesensitizeAssetcolumnDO.class)
                .select("t2.NAME AS assetName",
                        "t2.DESCRIPTION as assetDescription",
                        "t2.TABLE_NAME AS assetTableName",
                        "t2.TABLE_COMMENT AS assetTableComment",
                        "t3.COLUMN_NAME AS  assetcolumnName",
                        "t3.COLUMN_COMMENT  AS assetcolumnComment",
                        "t4.NAME AS dataCategoryName",
                        "t5.SHORT_NAME AS dataLevelName",
                        "t6.NAME AS desensitizeRuleName")
                .innerJoin("AST_ASSET t2 ON t.ASSET_ID=t2.ID  AND t2.DEL_FLAG = '0'")
                .innerJoin("AST_ASSET_COLUMN t3 ON t.ASSETCOLUMN_ID=t3.ID  AND t3.DEL_FLAG = '0'")
                .innerJoin("STD_DATA_CATEGORY  t4 ON t.DATA_CATEGORY_ID =t4.ID AND t4.DEL_FLAG = '0'")
                .leftJoin("STD_DATA_LEVEL t5 ON t4.DATA_LEVEL_ID =t5.ID AND t5.DEL_FLAG = '0'")
                .leftJoin("STD_DESENSITIZE_RULE t6 ON t6.DATA_CATEGORY_ID =t4.ID  AND t6.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getAssetName()),"t2.NAME", reqVO.getAssetName());

        lambdaWrapper
                //根据ValidFlag查询
                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getValidFlag, reqVO.getValidFlag())
                // 按照 createTime 字段降序排序
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        // 构造动态查询条件
        return selectJoinPage(reqVO, StandardsDesensitizeAssetcolumnDO.class, lambdaWrapper);
    }

    default PageResult<StandardsDesensitizeAssetcolumnDO> selectPagebyRuleId(StandardsDesensitizeAssetcolumnPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        MPJLambdaWrapperX<StandardsDesensitizeAssetcolumnDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper.selectAll(StandardsDesensitizeAssetcolumnDO.class)
                .select("t2.NAME AS assetName",
                        "t2.DESCRIPTION as assetDescription",
                        "t2.TABLE_NAME AS assetTableName",
                        "t2.TABLE_COMMENT AS assetTableComment",
                        "t3.COLUMN_NAME AS  assetcolumnName",
                        "t3.COLUMN_COMMENT  AS assetcolumnComment",
                        "t4.NAME AS dataCategoryName",
                        "t5.SHORT_NAME AS dataLevelName",
                        "t6.NAME AS desensitizeRuleName")
                .innerJoin("AST_ASSET t2 ON t.ASSET_ID=t2.ID  AND t2.DEL_FLAG = '0'")
                .innerJoin("AST_ASSET_COLUMN t3 ON t.ASSETCOLUMN_ID=t3.ID  AND t3.DEL_FLAG = '0'")
                .innerJoin("STD_DATA_CATEGORY  t4 ON t.DATA_CATEGORY_ID =t4.ID AND t4.DEL_FLAG = '0'")
                .leftJoin("STD_DATA_LEVEL t5 ON t4.DATA_LEVEL_ID =t5.ID AND t5.DEL_FLAG = '0'")
                .leftJoin("STD_DESENSITIZE_RULE t6 ON t6.DATA_CATEGORY_ID =t4.ID  AND t6.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getAssetName()), StandardsDesensitizeAssetcolumnDO::getAssetName, reqVO.getAssetName())
                .eq(reqVO.getRuleId() != null, "t6.ID", reqVO.getRuleId())
                // 按照 createTime 字段降序排序
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        // 构造动态查询条件
        return selectJoinPage(reqVO, StandardsDesensitizeAssetcolumnDO.class, lambdaWrapper);
    }

    default StandardsDesensitizeAssetcolumnDO selectDesensitizeAssetcolumnById(Long id) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        MPJLambdaWrapperX<StandardsDesensitizeAssetcolumnDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper.selectAll(StandardsDesensitizeAssetcolumnDO.class)
                .select("t2.NAME AS assetName",
                        "t2.DESCRIPTION as assetDescription",
                        "t2.TABLE_NAME AS assetTableName",
                        "t2.TABLE_COMMENT AS assetTableComment",
                        "t3.COLUMN_NAME AS  assetcolumnName",
                        "t3.COLUMN_COMMENT  AS assetcolumnComment",
                        "t4.NAME AS dataCategoryName",
                        "t5.SHORT_NAME AS dataLevelName",
                        "t6.NAME AS desensitizeRuleName")
                .innerJoin("AST_ASSET t2 ON t.ASSET_ID=t2.ID  AND t2.DEL_FLAG = '0'")
                .innerJoin("AST_ASSET_COLUMN t3 ON t.ASSETCOLUMN_ID=t3.ID  AND t3.DEL_FLAG = '0'")
                .innerJoin("STD_DATA_CATEGORY  t4 ON t.DATA_CATEGORY_ID =t4.ID AND t4.DEL_FLAG = '0'")
                .leftJoin("STD_DATA_LEVEL t5 ON t4.DATA_LEVEL_ID =t5.ID AND t5.DEL_FLAG = '0'")
                .leftJoin("STD_DESENSITIZE_RULE t6 ON t6.DATA_CATEGORY_ID =t4.ID  AND t6.DEL_FLAG = '0'")
                .eq(id != null, "t.ID", id);
        // 构造动态查询条件
        return selectOne(lambdaWrapper);
    }


//    default PageResult<StandardsDesensitizeAssetcolumnDO> selectPage(StandardsDesensitizeAssetcolumnPageReqVO reqVO) {
//        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
//        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
//
//        // 构造动态查询条件
//        return selectPage(reqVO, new LambdaQueryWrapperX<StandardsDesensitizeAssetcolumnDO>()
//                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getAssetId, reqVO.getAssetId())
//                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getAssetcolumnId, reqVO.getAssetcolumnId())
//                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getDataCategoryId, reqVO.getDataCategoryId())
//                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getSortOrder, reqVO.getSortOrder())
//                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getDescription, reqVO.getDescription())
//                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getValidFlag, reqVO.getValidFlag())
//                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getCreateBy, reqVO.getCreateBy())
//                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getCreatorId, reqVO.getCreatorId())
//                .eqIfPresent(StandardsDesensitizeAssetcolumnDO::getCreateTime, reqVO.getCreateTime())
//                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
//                // .likeIfPresent(StandardsDesensitizeAssetcolumnDO::getName, reqVO.getName())
//                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
//    }


}
