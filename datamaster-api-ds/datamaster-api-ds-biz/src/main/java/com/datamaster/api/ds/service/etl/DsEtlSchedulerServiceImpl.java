

package com.datamaster.api.ds.service.etl;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DsSchedulerRespDTO;
import com.datamaster.api.ds.api.etl.DsSchedulerSaveReqDTO;
import com.datamaster.api.ds.api.etl.DsSchedulerUpdateReqDTO;
import com.datamaster.api.ds.api.etl.ds.Schedule;
import com.datamaster.api.ds.api.service.etl.IDsEtlSchedulerService;
import com.datamaster.common.httpClient.DsRequestUtils;
import com.datamaster.common.httpClient.constants.DataMasterDSApiType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        JSONObject params = JSONObject.parseObject(JSONObject.toJSONString(dsSchedulerSaveReqDTO));
        params.put("workflowDefinitionCode", dsSchedulerSaveReqDTO.getProcessDefinitionCode());
        params.remove("processDefinitionCode");
        return DsRequestUtils.requestForm(DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode),
                apiType.getMethod(),
                params,
                DsSchedulerRespDTO.class);
    }

    @Override
    public DsSchedulerRespDTO updateScheduler(DsSchedulerUpdateReqDTO dsSchedulerUpdateReqDTO, String projectCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.UPDATE_SCHEDULE;
        JSONObject params = JSONObject.parseObject(JSONObject.toJSONString(dsSchedulerUpdateReqDTO));
        params.put("workflowDefinitionCode", dsSchedulerUpdateReqDTO.getProcessDefinitionCode());
        params.remove("processDefinitionCode");
        return DsRequestUtils.requestForm(DsRequestUtils.replaceProjectCodeAndId(apiType.getUrl(), projectCode, dsSchedulerUpdateReqDTO.getId()),
                apiType.getMethod(),
                params,
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
        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", 1);
        params.put("pageSize", 10);
        params.put("workflowDefinitionCode", taskCode);
        JSONObject response = DsRequestUtils.request(DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode),
                apiType.getMethod(),
                null, params,
                JSONObject.class);
        DsSchedulerRespDTO result = new DsSchedulerRespDTO();
        if (response == null) {
            result.setSuccess(false);
            result.setMsg("DolphinScheduler调度查询无响应");
            return result;
        }
        result.setCode(response.getInteger("code"));
        result.setMsg(response.getString("msg"));
        result.setSuccess(response.getBoolean("success"));
        result.setFailed(response.getBoolean("failed"));
        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            return result;
        }
        List<Schedule> schedules = data.getList("totalList", Schedule.class);
        if (schedules != null && !schedules.isEmpty()) {
            result.setData(schedules.get(0));
        }
        return result;
    }
}
