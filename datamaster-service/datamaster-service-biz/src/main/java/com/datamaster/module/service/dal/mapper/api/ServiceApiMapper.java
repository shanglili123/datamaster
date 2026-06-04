

package com.datamaster.module.service.dal.mapper.api;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.api.vo.ServiceApiPageReqVO;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;

/**
 * API服务Mapper接口
 *
 * @author lhs
 * @date 2025-02-12
 */
public interface ServiceApiMapper extends BaseMapperX<ServiceApiDO> {

    default PageResult<ServiceApiDO> selectPage(ServiceApiPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        MPJLambdaWrapper<ServiceApiDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(ServiceApiDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_API_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .between(reqVO.getParamByKey("beginCreateTime") != null && reqVO.getParamByKey("endCreateTime") != null,
                        ServiceApiDO::getCreateTime,
                        reqVO.getParamByKey("beginCreateTime"),
                        reqVO.getParamByKey("endCreateTime"))
                .like(StringUtils.isNotBlank(reqVO.getName()),ServiceApiDO::getName, reqVO.getName())
                .in(reqVO.getApiIdList() != null && !reqVO.getApiIdList().isEmpty(),ServiceApiDO::getId,reqVO.getApiIdList())
                .in(reqVO.getCatIds() != null && !reqVO.getCatIds().isEmpty(),ServiceApiDO::getCatId,reqVO.getCatIds())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), ServiceApiDO::getCatCode, reqVO.getCatCode())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()),ServiceApiDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getProjectId() != null, ServiceApiDO::getProjectId, reqVO.getProjectId())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return selectJoinPage(reqVO, ServiceApiDO.class, lambdaWrapper);
    }
}
