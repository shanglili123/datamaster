

package com.datamaster.api.ds.service.etl;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DSExecuteDTO;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.api.ds.api.service.etl.IDsEtlExecutorService;
import com.datamaster.common.httpClient.DsRequestUtils;
import com.datamaster.common.httpClient.constants.DataMasterDSApiType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public List<ProcessInstance> listProcessInstances(String projectCode, String processDefinitionCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.GET_PROCESS_INSTANCE_LIST;
        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", 1);
        params.put("pageSize", 100);
        params.put("processDefinitionCode", processDefinitionCode);

        JSONObject response = DsRequestUtils.request(DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode),
                apiType.getMethod(), null, params, JSONObject.class);
        if (response == null || !Boolean.TRUE.equals(response.getBoolean("success"))) {
            log.warn("查询DS流程实例失败，projectCode={}，processDefinitionCode={}，msg={}",
                    projectCode, processDefinitionCode, response == null ? null : response.getString("msg"));
            return Collections.emptyList();
        }

        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            return Collections.emptyList();
        }
        List<ProcessInstance> instances = data.getList("totalList", ProcessInstance.class);
        return instances == null ? Collections.emptyList() : instances;
    }
}
