

package com.datamaster.module.collector.convert.qa;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjPageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskObjDO;

import java.util.List;

/**
 * 数据质量任务-稽查对象 Convert
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Mapper
public interface CollectorQualityTaskObjConvert {
    CollectorQualityTaskObjConvert INSTANCE = Mappers.getMapper(CollectorQualityTaskObjConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param CollectorQualityTaskObjPageReqVO 请求参数
     * @return CollectorQualityTaskObjDO
     */
     CollectorQualityTaskObjDO convertToDO(CollectorQualityTaskObjPageReqVO CollectorQualityTaskObjPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param CollectorQualityTaskObjSaveReqVO 保存请求参数
     * @return CollectorQualityTaskObjDO
     */
     CollectorQualityTaskObjDO convertToDO(CollectorQualityTaskObjSaveReqVO CollectorQualityTaskObjSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param CollectorQualityTaskObjDO 实体对象
     * @return CollectorQualityTaskObjRespVO
     */
     CollectorQualityTaskObjRespVO convertToRespVO(CollectorQualityTaskObjDO CollectorQualityTaskObjDO);

    /**
     * DOList 转换为 RespVOList
     * @param CollectorQualityTaskObjDOList 实体对象列表
     * @return List<CollectorQualityTaskObjRespVO>
     */
     List<CollectorQualityTaskObjRespVO> convertToRespVOList(List<CollectorQualityTaskObjDO> CollectorQualityTaskObjDOList);
}
