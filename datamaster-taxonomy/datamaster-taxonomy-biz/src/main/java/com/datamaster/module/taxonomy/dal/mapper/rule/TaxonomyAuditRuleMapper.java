

package com.datamaster.module.taxonomy.dal.mapper.rule;

import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRulePageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyAuditRuleDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 稽查规则Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface TaxonomyAuditRuleMapper extends BaseMapperX<TaxonomyAuditRuleDO> {

    default PageResult<TaxonomyAuditRuleDO> selectPage(TaxonomyAuditRulePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyAuditRuleDO>()
                .likeIfPresent(TaxonomyAuditRuleDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyAuditRuleDO::getQualityDim, reqVO.getQualityDim())
                .eqIfPresent(TaxonomyAuditRuleDO::getType, reqVO.getType())
                .eqIfPresent(TaxonomyAuditRuleDO::getLevel, reqVO.getLevel())
                .eqIfPresent(TaxonomyAuditRuleDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(TaxonomyAuditRuleDO::getCode, reqVO.getCode())
                .likeIfPresent(TaxonomyAuditRuleDO::getUseCase, reqVO.getUseCase())
                .likeIfPresent(TaxonomyAuditRuleDO::getExample, reqVO.getExample())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyAuditRuleDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
//                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
                .eq(reqVO.getProjectId() != null, TaxonomyAuditRuleDO::getProjectId, reqVO.getProjectId())
         .orderByDesc(TaxonomyAuditRuleDO::getCreateTime));

    }

    List<TaxonomyAuditRuleDO> selectAttAuditRuleList(@Param("dataElemId") Long dataElemId);
}
