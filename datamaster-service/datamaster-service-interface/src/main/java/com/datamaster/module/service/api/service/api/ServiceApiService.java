

package com.datamaster.module.service.api.service.api;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.module.service.api.api.dto.ServiceApiReqDTO;
import com.datamaster.module.service.api.api.dto.ServiceApiRespDTO;
import com.datamaster.module.service.api.apiLog.dto.ServiceApiLogReqDTO;
import com.datamaster.module.service.api.apiLog.dto.ServiceApiLogRespDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ServiceApiService {

    Long getCountByCatCode(String catCode);

    /**
     * 查询当前空间可供 Agent 使用的数据服务。实现方必须在查询层再次校验空间，
     * 不能只依赖调用方传入的 ID。
     */
    List<ServiceApiRespDTO> listVisibleServiceApis(Long spaceId, String spaceCode,
                                                    String name, String status, int limit);

    /** 查询当前空间内的单个数据服务。 */
    ServiceApiRespDTO getVisibleServiceApi(Long id, Long spaceId, String spaceCode);

    /**
     * 调用当前空间内已发布的数据服务。参数只作为已发布服务声明的动态查询参数，
     * 不允许调用方替换数据源、SQL 或执行配置。
     */
    Object invokeVisibleServiceApi(Long id, Long spaceId, String spaceCode,
                                   Map<String, Object> params);

    /**
     * 在指定空间创建只读表单查询服务草稿；实现方负责补齐服务默认值和执行 SQL。
     */
    Long createDraftDataService(ServiceApiReqDTO request);

    /** 发布当前空间内的数据服务草稿。 */
    void publishDataService(Long id, Long spaceId, String spaceCode,
                            Long operatorId, String operatorName);

}
