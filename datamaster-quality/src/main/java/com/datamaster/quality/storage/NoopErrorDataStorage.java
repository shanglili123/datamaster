package com.datamaster.quality.storage;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.quality.controller.quality.vo.CheckErrorDataReqDTO;
import com.datamaster.quality.dal.dataobject.quality.CheckErrorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;

public class NoopErrorDataStorage implements ErrorDataStorage {

    private static final Logger log = LoggerFactory.getLogger(NoopErrorDataStorage.class);

    @Override
    public void saveErrorData(String reportId, Integer totalCount, Integer errorCount, List<JSONObject> errorList) {
        log.info("错误明细存储未配置，跳过 {} 条错误数据的写入", errorList.size());
    }

    @Override
    public Page<CheckErrorData> pageErrorData(PageRequest pageRequest, CheckErrorDataReqDTO dto) {
        return new PageImpl<>(Collections.emptyList(), pageRequest, 0);
    }

    @Override
    public boolean updateErrorData(CheckErrorDataReqDTO dto) {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
