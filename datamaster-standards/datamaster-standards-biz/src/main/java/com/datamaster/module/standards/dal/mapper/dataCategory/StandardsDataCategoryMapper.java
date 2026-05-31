

package com.datamaster.module.standards.dal.mapper.dataCategory;

import org.apache.commons.lang3.StringUtils;
import com.datamaster.module.standards.dal.dataobject.dataCategory.StandardsDataCategoryDO;

import java.util.Arrays;

import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;

import java.util.HashSet;
import java.util.Set;

import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategoryPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 数据分类Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
public interface StandardsDataCategoryMapper extends BaseMapperX<StandardsDataCategoryDO> {

    default PageResult<StandardsDataCategoryDO> selectPage(StandardsDataCategoryPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapperX<StandardsDataCategoryDO> lambdaWrapper = new MPJLambdaWrapperX<>();

        String subQuery = "(CASE WHEN r.id IS NOT NULL THEN '1' ELSE '0' END) AS desensitizationRulesFlag";
        lambdaWrapper.selectAll(StandardsDataCategoryDO.class)
                .select("t2.NAME AS catName", "t3.SHORT_NAME AS dataLevelShortName","r.id AS desensitizationRulesId", subQuery)
                .leftJoin("STD_DATA_CATEGORY_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .leftJoin("STD_DATA_LEVEL t3 on t.DATA_LEVEL_ID = t3.ID AND t3.DEL_FLAG = '0'")
                .leftJoin("STD_DESENSITIZE_RULE r on t.id = r.DATA_CATEGORY_ID AND r.DEL_FLAG = '0'")
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), StandardsDataCategoryDO::getCatCode, reqVO.getCatCode());

        lambdaWrapper
                .likeIfPresent(StandardsDataCategoryDO::getName, reqVO.getName())
                .likeIfPresent(StandardsDataCategoryDO::getDescription, reqVO.getDescription())
                .eqIfPresent(StandardsDataCategoryDO::getDataLevelId, reqVO.getDataLevelId())
                .eqIfPresent(StandardsDataCategoryDO::getPriority, reqVO.getPriority())
                .eqIfPresent(StandardsDataCategoryDO::getValidFlag, reqVO.getValidFlag())
                // 按照 createTime 字段降序排序
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        // 构造动态查询条件
        return selectJoinPage(reqVO, StandardsDataCategoryDO.class, lambdaWrapper);
    }
}
