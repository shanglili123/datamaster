

package com.datamaster.module.service.convert.api;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.service.controller.admin.api.vo.ServiceApiPageReqVO;
import com.datamaster.module.service.controller.admin.api.vo.ServiceApiRespVO;
import com.datamaster.module.service.controller.admin.api.vo.ServiceApiSaveReqVO;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;

import java.util.List;

/**
 * API服务 Convert
 *
 * @author lhs
 * @date 2025-02-12
 */
@Mapper
public interface ServiceApiConvert {
    ServiceApiConvert INSTANCE = Mappers.getMapper(ServiceApiConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ServiceApiPageReqVO 请求参数
     * @return ServiceApiDO
     */
     ServiceApiDO convertToDO(ServiceApiPageReqVO ServiceApiPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ServiceApiSaveReqVO 保存请求参数
     * @return ServiceApiDO
     */
     ServiceApiDO convertToDO(ServiceApiSaveReqVO ServiceApiSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ServiceApiDO 实体对象
     * @return ServiceApiRespVO
     */
     ServiceApiRespVO convertToRespVO(ServiceApiDO ServiceApiDO);

    /**
     * DOList 转换为 RespVOList
     * @param ServiceApiDOList 实体对象列表
     * @return List<ServiceApiRespVO>
     */
     List<ServiceApiRespVO> convertToRespVOList(List<ServiceApiDO> ServiceApiDOList);
}
