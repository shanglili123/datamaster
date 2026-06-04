

package com.datamaster.module.standards.dal.mapper.desensitizeRules;

import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 脱敏区间Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
public interface StandardsDesensitizeIntervalMapper extends BaseMapperX<StandardsDesensitizeIntervalDO> {

    default PageResult<StandardsDesensitizeIntervalDO> selectPage(StandardsDesensitizeIntervalPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<StandardsDesensitizeIntervalDO>()
                .eqIfPresent(StandardsDesensitizeIntervalDO::getDesensitizeRuleId, reqVO.getDesensitizeRuleId())
                .eqIfPresent(StandardsDesensitizeIntervalDO::getIntervalNo, reqVO.getIntervalNo())
                .eqIfPresent(StandardsDesensitizeIntervalDO::getStartNum, reqVO.getStartNum())
                .eqIfPresent(StandardsDesensitizeIntervalDO::getEndNum, reqVO.getEndNum())
                .eqIfPresent(StandardsDesensitizeIntervalDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(StandardsDesensitizeIntervalDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getProjectId() != null, StandardsDesensitizeIntervalDO::getProjectId, reqVO.getProjectId())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(StandardsDesensitizeIntervalDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
