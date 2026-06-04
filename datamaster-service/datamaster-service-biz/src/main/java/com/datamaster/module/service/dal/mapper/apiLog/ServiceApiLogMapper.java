

package com.datamaster.module.service.dal.mapper.apiLog;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogPageReqVO;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.dataobject.apiLog.ServiceApiLogDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;

/**
 * API服务调用日志Mapper接口
 *
 * @author lhs
 * @date 2025-02-12
 */
public interface ServiceApiLogMapper extends BaseMapperX<ServiceApiLogDO> {

    default PageResult<ServiceApiLogDO> selectPage(ServiceApiLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        MPJLambdaWrapper<ServiceApiLogDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(ServiceApiLogDO.class)
                .select("t2.NAME AS apiName,t2.REQ_METHOD as reqMethod,t3.NAME as catName")
                .leftJoin("Service_API t2 on t.API_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .leftJoin("TAX_API_CAT t3 on t.CAT_CODE = t3.CODE AND t3.DEL_FLAG = '0'")
                .like(StringUtils.isNotEmpty(reqVO.getApiName()), "t2.NAME", reqVO.getApiName())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), ServiceApiLogDO::getCatCode, reqVO.getCatCode())
                .eq(reqVO.getApiId() != null, ServiceApiLogDO::getApiId, reqVO.getApiId())
                .eq(reqVO.getCallerId() != null, ServiceApiLogDO::getCallerId, reqVO.getCallerId())
                .eq(reqVO.getStatus() != null, ServiceApiLogDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getProjectId() != null, ServiceApiLogDO::getProjectId, reqVO.getProjectId())
                .between(com.datamaster.common.utils.StringUtils.isNotNull(reqVO.getParamByKey("beginCreateTime"))
                        &&com.datamaster.common.utils.StringUtils.isNotNull(reqVO.getParamByKey("endCreateTime")),
                        ServiceApiDO::getCreateTime, reqVO.getParamByKey("beginCreateTime"), reqVO.getParamByKey("endCreateTime"))
                .eq(reqVO.getCreateTime() != null, ServiceApiLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);

        // 构造动态查询条件
        return selectJoinPage(reqVO, ServiceApiLogDO.class, wrapper);
    }

    public ServiceApiLogDO selectServiceApiLogByID(Long id);
}
