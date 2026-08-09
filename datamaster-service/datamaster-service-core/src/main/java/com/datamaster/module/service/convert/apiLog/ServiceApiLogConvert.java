

package com.datamaster.module.service.convert.apiLog;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogPageReqVO;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogRespVO;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogSaveReqVO;
import com.datamaster.module.service.dal.dataobject.apiLog.ServiceApiLogDO;

import java.util.List;

/**
 * API服务调用日志 Convert
 *
 * @author lhs
 * @date 2025-02-12
 */
@Mapper
public interface ServiceApiLogConvert {
    ServiceApiLogConvert INSTANCE = Mappers.getMapper(ServiceApiLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ServiceApiLogPageReqVO 请求参数
     * @return ServiceApiLogDO
     */
     ServiceApiLogDO convertToDO(ServiceApiLogPageReqVO ServiceApiLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ServiceApiLogSaveReqVO 保存请求参数
     * @return ServiceApiLogDO
     */
     ServiceApiLogDO convertToDO(ServiceApiLogSaveReqVO ServiceApiLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ServiceApiLogDO 实体对象
     * @return ServiceApiLogRespVO
     */
     ServiceApiLogRespVO convertToRespVO(ServiceApiLogDO ServiceApiLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param ServiceApiLogDOList 实体对象列表
     * @return List<ServiceApiLogRespVO>
     */
     List<ServiceApiLogRespVO> convertToRespVOList(List<ServiceApiLogDO> ServiceApiLogDOList);
}
