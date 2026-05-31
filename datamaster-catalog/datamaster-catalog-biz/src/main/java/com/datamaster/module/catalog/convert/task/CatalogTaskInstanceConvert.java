package com.datamaster.module.catalog.convert.task;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstancePageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceDO;

import java.util.List;

/**
 * 采集任务实例 Convert
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Mapper
public interface CatalogTaskInstanceConvert {
    CatalogTaskInstanceConvert INSTANCE = Mappers.getMapper(CatalogTaskInstanceConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CatalogTaskInstancePageReqVO 请求参数
     * @return CatalogTaskInstanceDO
     */
     CatalogTaskInstanceDO convertToDO(CatalogTaskInstancePageReqVO CatalogTaskInstancePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CatalogTaskInstanceSaveReqVO 保存请求参数
     * @return CatalogTaskInstanceDO
     */
     CatalogTaskInstanceDO convertToDO(CatalogTaskInstanceSaveReqVO CatalogTaskInstanceSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CatalogTaskInstanceDO 实体对象
     * @return CatalogTaskInstanceRespVO
     */
     CatalogTaskInstanceRespVO convertToRespVO(CatalogTaskInstanceDO CatalogTaskInstanceDO);

    /**
     * DOList 转换为 RespVOList
     * @param CatalogTaskInstanceDOList 实体对象列表
     * @return List<CatalogTaskInstanceRespVO>
     */
     List<CatalogTaskInstanceRespVO> convertToRespVOList(List<CatalogTaskInstanceDO> CatalogTaskInstanceDOList);
}
