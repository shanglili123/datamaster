

package com.datamaster.api.ds.service.etl;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DsSchedulerRespDTO;
import com.datamaster.api.ds.api.etl.DsSchedulerSaveReqDTO;
import com.datamaster.api.ds.api.etl.DsSchedulerUpdateReqDTO;
import com.datamaster.api.ds.api.service.etl.IDsEtlSchedulerService;
import com.datamaster.common.httpClient.DsRequestUtils;
import com.datamaster.common.httpClient.constants.DataMasterDSApiType;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-21 10:30
 **/
@Slf4j
@Service
public class DsEtlSchedulerServiceImpl implements IDsEtlSchedulerService {
    @Override
    public DsSchedulerRespDTO saveScheduler(DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO, String projectCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.CREATE_SCHEDULE;
        return DsRequestUtils.requestForm(DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode),
                apiType.getMethod(),
                JSONObject.parseObject(JSONObject.toJSONString(dsSchedulerSaveReqDTO)),
                DsSchedulerRespDTO.class);
    }

    @Override
    public DsSchedulerRespDTO updateScheduler(DsSchedulerUpdateReqDTO dsSchedulerUpdateReqDTO, String projectCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.UPDATE_SCHEDULE;
        return DsRequestUtils.requestForm(DsRequestUtils.replaceProjectCodeAndId(apiType.getUrl(), projectCode, dsSchedulerUpdateReqDTO.getId()),
                apiType.getMethod(),
                JSONObject.parseObject(JSONObject.toJSONString(dsSchedulerUpdateReqDTO)),
                DsSchedulerRespDTO.class);
    }

    @Override
    public DsStatusRespDTO onlineScheduler(String projectCode, Long id) {
        DataMasterDSApiType apiType = DataMasterDSApiType.SCHEDULE_ONLINE;
        return DsRequestUtils.request(DsRequestUtils.replaceProjectCodeAndId(apiType.getUrl(), projectCode, id),
                apiType.getMethod(),
                null, null,
                DsStatusRespDTO.class);
    }

    @Override
    public DsStatusRespDTO offlineScheduler(String projectCode, Long id) {
        DataMasterDSApiType apiType = DataMasterDSApiType.SCHEDULE_OFFLINE;
        return DsRequestUtils.request(DsRequestUtils.replaceProjectCodeAndId(apiType.getUrl(), projectCode, id),
                apiType.getMethod(),
                null, null,
                DsStatusRespDTO.class);
    }

    @Override
    public DsSchedulerRespDTO getByTaskCode(String projectCode, String taskCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.GET_SCHEDULE_BY_PROCESS_CODE;
        return DsRequestUtils.request(DsRequestUtils.replaceProjectCodeAndCode(apiType.getUrl(), projectCode, taskCode),
                apiType.getMethod(),
                null, null,
                DsSchedulerRespDTO.class);
    }
}
