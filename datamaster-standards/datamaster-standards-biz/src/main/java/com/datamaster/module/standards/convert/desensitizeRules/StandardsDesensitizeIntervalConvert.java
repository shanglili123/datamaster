

package com.datamaster.module.standards.convert.desensitizeRules;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalPageReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;

/**
 * 脱敏区间 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Mapper
public interface StandardsDesensitizeIntervalConvert {
    StandardsDesensitizeIntervalConvert INSTANCE = Mappers.getMapper(StandardsDesensitizeIntervalConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDesensitizeIntervalPageReqVO 请求参数
     * @return StandardsDesensitizeIntervalDO
     */
     StandardsDesensitizeIntervalDO convertToDO(StandardsDesensitizeIntervalPageReqVO StandardsDesensitizeIntervalPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDesensitizeIntervalSaveReqVO 保存请求参数
     * @return StandardsDesensitizeIntervalDO
     */
     StandardsDesensitizeIntervalDO convertToDO(StandardsDesensitizeIntervalSaveReqVO StandardsDesensitizeIntervalSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDesensitizeIntervalDO 实体对象
     * @return StandardsDesensitizeIntervalRespVO
     */
     StandardsDesensitizeIntervalRespVO convertToRespVO(StandardsDesensitizeIntervalDO StandardsDesensitizeIntervalDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDesensitizeIntervalDOList 实体对象列表
     * @return List<StandardsDesensitizeIntervalRespVO>
     */
     List<StandardsDesensitizeIntervalRespVO> convertToRespVOList(List<StandardsDesensitizeIntervalDO> StandardsDesensitizeIntervalDOList);
}
