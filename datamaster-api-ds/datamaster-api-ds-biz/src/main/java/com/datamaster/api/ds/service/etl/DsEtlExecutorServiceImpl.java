

package com.datamaster.api.ds.service.etl;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DSExecuteDTO;
import com.datamaster.api.ds.api.service.etl.IDsEtlExecutorService;
import com.datamaster.common.httpClient.DsRequestUtils;
import com.datamaster.common.httpClient.constants.DataMasterDSApiType;

/**
 * <P>
 * 用途:执行相关相关接口实现类
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-27 14:37
 **/
@Slf4j
@Service
public class DsEtlExecutorServiceImpl implements IDsEtlExecutorService {
    @Override
    public DsStatusRespDTO execute(DSExecuteDTO dsExecuteDTO, String projectCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.POST_EXECUTORS_EXECUTE;
        return DsRequestUtils.requestForm(DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode),
                apiType.getMethod(), JSONObject.parseObject(JSONObject.toJSONString(dsExecuteDTO)),
                DsStatusRespDTO.class);
    }
}
