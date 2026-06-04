

package com.datamaster.module.modeling.dal.mapper.dm;

import org.apache.commons.lang3.StringUtils;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataDomainDO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerSpecificationDO;

import java.util.Arrays;

import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 数仓分层-规范管理Mapper接口
 *
 * @author FXB
 * @date 2026-03-24
 */
public interface ModelingDataLayerSpecificationMapper extends BaseMapperX<ModelingDataLayerSpecificationDO> {

    default PageResult<ModelingDataLayerSpecificationDO> selectPage(ModelingDataLayerSpecificationPageReqVO reqVO) {
        MPJLambdaWrapperX<ModelingDataLayerSpecificationDO> lambdaWrapper = new MPJLambdaWrapperX<>();

        lambdaWrapper.selectAll(ModelingDataLayerSpecificationDO.class)
                .select("u.NICK_NAME AS ownerUserName","u.PHONENUMBER AS ownerUserPhoneNumber")
                .leftJoin("SYSTEM_USER u on t.OWNER_USER_ID = u.USER_ID AND u.DEL_FLAG = '0'");

        // 构造动态查询条件
        lambdaWrapper.eqIfPresent(ModelingDataLayerSpecificationDO::getDataLayerId, reqVO.getDataLayerId())
                .likeIfPresent(ModelingDataLayerSpecificationDO::getPrefixName, reqVO.getPrefixName())
                .likeIfPresent(ModelingDataLayerSpecificationDO::getBusinessEngName, reqVO.getBusinessEngName())
                .eqIfPresent(ModelingDataLayerSpecificationDO::getOwnerUserId, reqVO.getOwnerUserId())
                .eqIfPresent(ModelingDataLayerSpecificationDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ModelingDataLayerSpecificationDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelingDataLayerSpecificationDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(ModelingDataLayerSpecificationDO::getProjectId, reqVO.getProjectId())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelingDataLayerSpecificationDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(","))
                                .stream().map(e -> "t." +LambdaQueryWrapperX.camelToUnderline(e))
                                .collect(Collectors.toList()) : null);
        return selectJoinPage(reqVO, ModelingDataLayerSpecificationDO.class, lambdaWrapper);
    }
}
