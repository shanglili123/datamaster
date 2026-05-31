package com.datamaster.module.catalog.convert.task;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceLogDO;

import java.util.List;

/**
 * 采集任务实例-日志 Convert
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Mapper
public interface CatalogTaskInstanceLogConvert {
    CatalogTaskInstanceLogConvert INSTANCE = Mappers.getMapper(CatalogTaskInstanceLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogTaskInstanceLogPageReqVO 请求参数
     * @return CatalogTaskInstanceLogDO
     */
     CatalogTaskInstanceLogDO convertToDO(CatalogTaskInstanceLogPageReqVO CatalogTaskInstanceLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogTaskInstanceLogSaveReqVO 保存请求参数
     * @return CatalogTaskInstanceLogDO
     */
     CatalogTaskInstanceLogDO convertToDO(CatalogTaskInstanceLogSaveReqVO CatalogTaskInstanceLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogTaskInstanceLogDO 实体对象
     * @return CatalogTaskInstanceLogRespVO
     */
     CatalogTaskInstanceLogRespVO convertToRespVO(CatalogTaskInstanceLogDO CatalogTaskInstanceLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogTaskInstanceLogDOList 实体对象列表
     * @return List<CatalogTaskInstanceLogRespVO>
     */
     List<CatalogTaskInstanceLogRespVO> convertToRespVOList(List<CatalogTaskInstanceLogDO> CatalogTaskInstanceLogDOList);
}
