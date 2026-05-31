

package com.datamaster.api.ds.service.etl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.datamaster.api.ds.api.etl.DsNodeGenCodeRespDTO;
import com.datamaster.api.ds.api.service.etl.IDsEtlNodeService;
import com.datamaster.common.httpClient.DsRequestUtils;
import com.datamaster.common.httpClient.constants.QianTongDCApiType;

import java.util.HashMap;
import java.util.Map;

/**
 * <P>
 * 用途:ds数据集成节点相关接口实现
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-18 17:01
 **/
@Slf4j
@Service
public class DsEtlNodeServiceImpl implements IDsEtlNodeService {
    @Override
    public DsNodeGenCodeRespDTO genCode(Long projectCode) {
        QianTongDCApiType apiType = QianTongDCApiType.GEN_TASK_DEFINITION_CODES;
        Map<String, Object> params = new HashMap<>();
        params.put("genNum", 1);
        return DsRequestUtils.request(apiType.getUrl(),
                apiType.getMethod(),
                null, params,
                DsNodeGenCodeRespDTO.class);
    }
}
