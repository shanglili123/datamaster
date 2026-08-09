package com.datamaster.metadata.storage;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.metadata.controller.quality.vo.CheckErrorDataReqDTO;
import com.datamaster.metadata.dal.dataobject.quality.CheckErrorData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface ErrorDataStorage {

    void saveErrorData(String reportId, Integer totalCount, Integer errorCount, List<JSONObject> errorList);

    Page<CheckErrorData> pageErrorData(PageRequest pageRequest, CheckErrorDataReqDTO dto);

    boolean updateErrorData(CheckErrorDataReqDTO dto);

    boolean isAvailable();
}
