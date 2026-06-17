
package com.datamaster.api.ds.service.datasource;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.api.ds.api.base.DsResultDTO;
import com.datamaster.api.ds.api.datasource.DsDatasourceCreateReqDTO;
import com.datamaster.api.ds.api.service.datasource.IDsDatasourceService;
import com.datamaster.common.httpClient.DsRequestUtils;
import com.datamaster.common.httpClient.constants.DataMasterDSApiType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DsDatasourceServiceImpl implements IDsDatasourceService {

    @Override
    public DsResultDTO createDatasource(DsDatasourceCreateReqDTO reqDTO) {
        DataMasterDSApiType apiType = DataMasterDSApiType.CREATE_DATASOURCE;
        log.info("createDatasource request: type={}, name={}, host={}, port={}",
                reqDTO.getType(), reqDTO.getName(), reqDTO.getHost(), reqDTO.getPort());
        DsResultDTO result = DsRequestUtils.request(apiType.getUrl(), apiType.getMethod(),
                reqDTO, null, DsResultDTO.class);
        log.info("createDatasource response: code={}, msg={}, success={}", result.getCode(), result.getMsg(), result.getSuccess());
        return result;
    }

    @Override
    public DsResultDTO testConnect(DsDatasourceCreateReqDTO reqDTO) {
        DataMasterDSApiType apiType = DataMasterDSApiType.TEST_CONNECT_DATASOURCE;
        log.info("testConnect request: type={}, name={}, host={}, port={}",
                reqDTO.getType(), reqDTO.getName(), reqDTO.getHost(), reqDTO.getPort());
        DsResultDTO result = DsRequestUtils.request(apiType.getUrl(), apiType.getMethod(),
                reqDTO, null, DsResultDTO.class);
        log.info("testConnect response: code={}, msg={}, success={}", result.getCode(), result.getMsg(), result.getSuccess());
        return result;
    }

    @Override
    public boolean existsByName(String name) {
        DataMasterDSApiType apiType = DataMasterDSApiType.QUERY_DATASOURCE_LIST;
        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", 1);
        params.put("pageSize", 100);
        params.put("searchVal", name);
        JSONObject response = DsRequestUtils.request(apiType.getUrl(), apiType.getMethod(),
                null, params, JSONObject.class);
        if (response == null || !Boolean.TRUE.equals(response.getBoolean("success"))) {
            log.warn("existsByName query failed or DS unavailable, skipping duplicate check");
            return false;
        }
        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            return false;
        }
        JSONArray totalList = data.getJSONArray("totalList");
        if (totalList == null || totalList.isEmpty()) {
            return false;
        }
        for (int i = 0; i < totalList.size(); i++) {
            String dsName = totalList.getJSONObject(i).getString("name");
            if (name.equals(dsName)) {
                log.info("existsByName: datasource [{}] already exists in DS", name);
                return true;
            }
        }
        return false;
    }

}
