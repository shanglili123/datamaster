package com.datamaster.module.standards.dal.mapper.dataElem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelPageReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemRuleRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.*;

/**
 * 数据元数据规则关联信息Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface StandardsDataElemRuleRelMapper extends BaseMapperX<StandardsDataElemRuleRelDO> {

    default PageResult<StandardsDataElemRuleRelDO> selectPage(StandardsDataElemRuleRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<StandardsDataElemRuleRelDO> lambdaWrapper = new MPJLambdaWrapper<>();
        lambdaWrapper.selectAll(StandardsDataElemRuleRelDO.class);
        lambdaWrapper.eq(StringUtils.isNotBlank(reqVO.getType()), StandardsDataElemRuleRelDO::getType, reqVO.getType())
                .eq(reqVO.getDataElemId() != null, StandardsDataElemRuleRelDO::getDataElemId, reqVO.getDataElemId())
                .eq(reqVO.getRuleId() != null, StandardsDataElemRuleRelDO::getRuleId, reqVO.getRuleId())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return selectJoinPage(reqVO, StandardsDataElemRuleRelDO.class, lambdaWrapper);
    }

    default List<StandardsDataElemRuleRelDO> listByDataElemIdList(Collection<Long> dataElemIdList, String type) {
        LambdaQueryWrapper<StandardsDataElemRuleRelDO> queryWrapper = Wrappers.<StandardsDataElemRuleRelDO>lambdaQuery().in(StandardsDataElemRuleRelDO::getDataElemId, dataElemIdList)
                .eq(StandardsDataElemRuleRelDO::getType, type);
        return selectList(queryWrapper);
    }

}
