

package com.datamaster.api.ds.service.project;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.datamaster.api.ds.api.project.DsProjectCreateReqDTO;
import com.datamaster.api.ds.api.project.DsProjectDeleteRespDTO;
import com.datamaster.api.ds.api.project.DsProjectRespDTO;
import com.datamaster.api.ds.api.project.DsProjectUpdateReqDTO;
import com.datamaster.api.ds.api.project.DsWorkerGroupRespDTO;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.service.project.IDsProjectService;
import com.datamaster.common.httpClient.DsRequestUtils;
import com.datamaster.common.httpClient.constants.DataMasterDSApiType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <P>
 * 用途:ds项目service实现
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-18 14:27
 **/
@Slf4j
@Service
public class DsProjectServiceImpl implements IDsProjectService {

    @Override
    public DsProjectRespDTO saveProject(DsProjectCreateReqDTO dsProjectCreateReqDTO) {
        DataMasterDSApiType apiType = DataMasterDSApiType.CREATE_PROJECT;
        return DsRequestUtils.request(apiType.getUrl(),
                apiType.getMethod(),
                dsProjectCreateReqDTO, null,
                DsProjectRespDTO.class);
    }

    @Override
    public DsProjectRespDTO updateProject(DsProjectUpdateReqDTO dsProjectUpdateReqDTO) {
        DataMasterDSApiType apiType = DataMasterDSApiType.UPDATE_PROJECT;
        return DsRequestUtils.request(StringUtils.replace(apiType.getUrl(), "{code}", String.valueOf(dsProjectUpdateReqDTO.getProjectCode())),
                apiType.getMethod(),
                dsProjectUpdateReqDTO, null,
                DsProjectRespDTO.class);
    }

    @Override
    public DsProjectDeleteRespDTO deleteProject(Long projectCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.DELETE_PROJECT;
        return DsRequestUtils.request(StringUtils.replace(apiType.getUrl(), "{code}", String.valueOf(projectCode)),
                apiType.getMethod(),
                null, null,
                DsProjectDeleteRespDTO.class);
    }

    @Override
    public DsWorkerGroupRespDTO saveWorkerGroup(String workerGroupName) {
        DataMasterDSApiType apiType = DataMasterDSApiType.CREATE_WORKER_GROUP;
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", 0);
        params.put("name", workerGroupName);
        params.put("addrList", "");
        params.put("description", "DataMaster project worker group");
        return DsRequestUtils.requestForm(apiType.getUrl(), apiType.getMethod(), params, DsWorkerGroupRespDTO.class);
    }

    @Override
    public DsStatusRespDTO assignWorkerGroup(Long projectCode, String workerGroupName) {
        DataMasterDSApiType apiType = DataMasterDSApiType.ASSIGN_WORKER_GROUP_TO_PROJECT;
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("workerGroups", workerGroupName);
        return DsRequestUtils.requestForm(DsRequestUtils.replaceProjectCode(apiType.getUrl(), String.valueOf(projectCode)),
                apiType.getMethod(), params, DsStatusRespDTO.class);
    }

    @Override
    public DsStatusRespDTO deleteWorkerGroup(Integer workerGroupId) {
        DataMasterDSApiType apiType = DataMasterDSApiType.DELETE_WORKER_GROUP;
        return DsRequestUtils.request(StringUtils.replace(apiType.getUrl(), "{id}", String.valueOf(workerGroupId)),
                apiType.getMethod(), null, null, DsStatusRespDTO.class);
    }
}
