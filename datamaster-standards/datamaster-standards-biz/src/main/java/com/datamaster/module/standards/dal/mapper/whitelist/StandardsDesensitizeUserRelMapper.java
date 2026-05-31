

package com.datamaster.module.standards.dal.mapper.whitelist;

import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeUserRelDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 脱敏白名单与用户关联关系Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
public interface StandardsDesensitizeUserRelMapper extends BaseMapperX<StandardsDesensitizeUserRelDO> {

    default PageResult<StandardsDesensitizeUserRelDO> selectPage(StandardsDesensitizeUserRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<StandardsDesensitizeUserRelDO>()
                .eqIfPresent(StandardsDesensitizeUserRelDO::getDesensitizeId, reqVO.getDesensitizeId())
                .eqIfPresent(StandardsDesensitizeUserRelDO::getUserId, reqVO.getUserId())
                .likeIfPresent(StandardsDesensitizeUserRelDO::getDesensitizeName, reqVO.getDesensitizeName())
                .likeIfPresent(StandardsDesensitizeUserRelDO::getUserName, reqVO.getUserName())
                .eqIfPresent(StandardsDesensitizeUserRelDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(StandardsDesensitizeUserRelDO::getEffectiveCategory, reqVO.getEffectiveCategory())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(StandardsDesensitizeUserRelDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
