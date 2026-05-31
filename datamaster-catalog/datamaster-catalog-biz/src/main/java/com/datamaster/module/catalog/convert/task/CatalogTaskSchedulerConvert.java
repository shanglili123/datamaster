package com.datamaster.module.catalog.convert.task;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskSchedulerDO;

import java.util.List;

/**
 * 数据集成调度信息 Convert
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Mapper
public interface CatalogTaskSchedulerConvert {
    CatalogTaskSchedulerConvert INSTANCE = Mappers.getMapper(CatalogTaskSchedulerConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogTaskSchedulerPageReqVO 请求参数
     * @return CatalogTaskSchedulerDO
     */
     CatalogTaskSchedulerDO convertToDO(CatalogTaskSchedulerPageReqVO CatalogTaskSchedulerPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogTaskSchedulerSaveReqVO 保存请求参数
     * @return CatalogTaskSchedulerDO
     */
     CatalogTaskSchedulerDO convertToDO(CatalogTaskSchedulerSaveReqVO CatalogTaskSchedulerSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogTaskSchedulerDO 实体对象
     * @return CatalogTaskSchedulerRespVO
     */
     CatalogTaskSchedulerRespVO convertToRespVO(CatalogTaskSchedulerDO CatalogTaskSchedulerDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogTaskSchedulerDOList 实体对象列表
     * @return List<CatalogTaskSchedulerRespVO>
     */
     List<CatalogTaskSchedulerRespVO> convertToRespVOList(List<CatalogTaskSchedulerDO> CatalogTaskSchedulerDOList);
}
