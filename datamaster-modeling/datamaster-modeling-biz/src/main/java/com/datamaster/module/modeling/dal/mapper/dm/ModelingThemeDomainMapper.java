

package com.datamaster.module.modeling.dal.mapper.dm;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataDomainDO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingThemeDomainDO;

import java.util.Arrays;

import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 主题域管理Mapper接口
 *
 * @author FXB
 * @date 2026-03-24
 */
public interface ModelingThemeDomainMapper extends BaseMapperX<ModelingThemeDomainDO> {

    default PageResult<ModelingThemeDomainDO> selectPage(ModelingThemeDomainPageReqVO reqVO) {
        MPJLambdaWrapperX<ModelingThemeDomainDO> lambdaWrapper = new MPJLambdaWrapperX<>();

        lambdaWrapper.selectAll(ModelingThemeDomainDO.class)
                .select("u.NICK_NAME AS ownerUserName","u.PHONENUMBER AS ownerUserPhoneNumber")
                .leftJoin("SYSTEM_USER u on t.OWNER_USER_ID = u.USER_ID AND u.DEL_FLAG = '0'");

        lambdaWrapper.eqIfPresent(ModelingThemeDomainDO::getCode, reqVO.getCode())
                .likeIfPresent(ModelingThemeDomainDO::getName, reqVO.getName())
                .likeIfPresent(ModelingThemeDomainDO::getEngName, reqVO.getEngName())
                .eqIfPresent(ModelingThemeDomainDO::getParentId, reqVO.getParentId())
                .eqIfPresent(ModelingThemeDomainDO::getOwnerUserId, reqVO.getOwnerUserId())
                .eqIfPresent(ModelingThemeDomainDO::getDataLayerId, reqVO.getDataLayerId())
                .likeIfPresent(ModelingThemeDomainDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelingThemeDomainDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelingThemeDomainDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(","))
                                .stream().map(e -> "t." +LambdaQueryWrapperX.camelToUnderline(e))
                                .collect(Collectors.toList()) : null);

        return selectJoinPage(reqVO, ModelingThemeDomainDO.class, lambdaWrapper);
    }

    @Update(value = "update MDL_THEME_DOMAIN set VALID_FLAG=CASE WHEN #{validFlag} THEN '1' ELSE '0' END where code like concat(#{prefixCode}, '%')")
    int updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);
}
