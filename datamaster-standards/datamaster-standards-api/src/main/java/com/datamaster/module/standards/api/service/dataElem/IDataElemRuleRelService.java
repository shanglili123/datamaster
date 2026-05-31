

package com.datamaster.module.standards.api.service.dataElem;

import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemRuleRelRespDTO;

import java.util.Collection;
import java.util.List;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-03 18:06
 **/
public interface IDataElemRuleRelService {
    /**
     * 通过数据元id列表查询数据元规则关联信息
     *
     * @param dataElemIdList
     * @return
     */
    List<StandardsDataElemRuleRelRespDTO> listByDataElemIdList(Collection<Long> dataElemIdList, String type);
}
