

package com.datamaster.module.modeling.dal.mapper.dm;

import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainPageReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataDomainDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 数据域管理Mapper接口
 *
 * @author FXB
 * @date 2026-03-24
 */
public interface ModelingDataDomainMapper extends BaseMapperX<ModelingDataDomainDO> {

    default PageResult<ModelingDataDomainDO> selectPage(ModelingDataDomainPageReqVO reqVO) {
        MPJLambdaWrapperX<ModelingDataDomainDO> lambdaWrapper = new MPJLambdaWrapperX<>();

        lambdaWrapper.selectAll(ModelingDataDomainDO.class)
                .select("u.NICK_NAME AS ownerUserName","u.PHONENUMBER AS ownerUserPhoneNumber")
                .leftJoin("SYSTEM_USER u on t.OWNER_USER_ID = u.USER_ID AND u.DEL_FLAG = '0'");

        lambdaWrapper.likeIfPresent(ModelingDataDomainDO::getName, reqVO.getName())
                .likeIfPresent(ModelingDataDomainDO::getEngName, reqVO.getEngName())
                .eqIfPresent(ModelingDataDomainDO::getOwnerUserId, reqVO.getOwnerUserId())
                .eqIfPresent(ModelingDataDomainDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelingDataDomainDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelingDataDomainDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(","))
                                .stream().map(e -> "t." + LambdaQueryWrapperX.camelToUnderline(e))
                                .collect(Collectors.toList()) : null);
        return selectJoinPage(reqVO, ModelingDataDomainDO.class, lambdaWrapper);
    }

    default PageResult<ModelingDataDomainDO> selectlistBybusinessDomainId(ModelingDataDomainPageReqVO reqVO) {
        MPJLambdaWrapperX<ModelingDataDomainDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper.selectAll(ModelingDataDomainDO.class)
                .select("u.NICK_NAME AS ownerUserName", "u.PHONENUMBER AS ownerUserPhoneNumber")
                .leftJoin("MDL_BUSINESS_DOMAIN_REL t3 ON t3.DATA_DOMAIN_ID=t.ID  AND t3.DEL_FLAG = '0'")
                .leftJoin("MDL_BUSINESS_CATEGORY t4 ON t4.ID=t3.BUSINESS_CATEGORY_ID AND t4.DEL_FLAG = '0'")
                .leftJoin("SYSTEM_USER u on t.OWNER_USER_ID = u.USER_ID AND u.DEL_FLAG = '0'")
                .eq(reqVO.getBusinessDomainId() != null, "t4.ID", reqVO.getBusinessDomainId());
        return selectJoinPage(reqVO, ModelingDataDomainDO.class, lambdaWrapper);
    }
}
