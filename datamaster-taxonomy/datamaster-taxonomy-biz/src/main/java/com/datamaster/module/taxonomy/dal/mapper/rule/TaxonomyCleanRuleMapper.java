

package com.datamaster.module.taxonomy.dal.mapper.rule;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRulePageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyCleanRuleDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 清洗规则Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface TaxonomyCleanRuleMapper extends BaseMapperX<TaxonomyCleanRuleDO> {

    default PageResult<TaxonomyCleanRuleDO> selectPage(TaxonomyCleanRulePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<TaxonomyCleanRuleDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(TaxonomyCleanRuleDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_CLEAN_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), TaxonomyCleanRuleDO::getCatCode, reqVO.getCatCode())
                .like(StringUtils.isNotBlank(reqVO.getName()), TaxonomyCleanRuleDO::getName, reqVO.getName())
                .eq(StringUtils.isNotBlank(reqVO.getType()), TaxonomyCleanRuleDO::getType, reqVO.getType())
                .eq(StringUtils.isNotBlank(reqVO.getCode()), TaxonomyCleanRuleDO::getCode, reqVO.getCode())
                .eq(StringUtils.isNotBlank(reqVO.getLevel()), TaxonomyCleanRuleDO::getLevel, reqVO.getLevel())
                .eq(reqVO.getProjectId() != null, TaxonomyCleanRuleDO::getProjectId, reqVO.getProjectId())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);

        return selectJoinPage(reqVO, TaxonomyCleanRuleDO.class, lambdaWrapper);
    }

    List<TaxonomyCleanRuleDO> selectAttCleanRuleList(@Param("dataElemId") Long dataElemId);

    List<TaxonomyCleanRuleDO> getCleaningRuleTreeIds(Long[] dataElemId);
}
