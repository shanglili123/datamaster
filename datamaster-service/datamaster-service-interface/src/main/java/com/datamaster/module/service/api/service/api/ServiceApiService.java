

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

}
