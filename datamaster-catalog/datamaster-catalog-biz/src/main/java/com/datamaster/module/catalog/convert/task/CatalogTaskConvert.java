package com.datamaster.module.catalog.convert.task;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskDO;

import java.util.List;

/**
 * 采集任务 Convert
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Mapper
public interface CatalogTaskConvert {
    CatalogTaskConvert INSTANCE = Mappers.getMapper(CatalogTaskConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogTaskPageReqVO 请求参数
     * @return CatalogTaskDO
     */
     CatalogTaskDO convertToDO(CatalogTaskPageReqVO CatalogTaskPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogTaskSaveReqVO 保存请求参数
     * @return CatalogTaskDO
     */
     CatalogTaskDO convertToDO(CatalogTaskSaveReqVO CatalogTaskSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogTaskDO 实体对象
     * @return CatalogTaskRespVO
     */
     CatalogTaskRespVO convertToRespVO(CatalogTaskDO CatalogTaskDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogTaskDOList 实体对象列表
     * @return List<CatalogTaskRespVO>
     */
     List<CatalogTaskRespVO> convertToRespVOList(List<CatalogTaskDO> CatalogTaskDOList);
}
