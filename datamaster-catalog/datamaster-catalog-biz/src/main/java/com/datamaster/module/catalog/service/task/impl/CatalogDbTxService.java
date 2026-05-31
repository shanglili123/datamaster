package com.datamaster.module.catalog.service.task.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogDbSaveReqVO;
import com.datamaster.module.catalog.service.metadata.ICatalogDbService;

import javax.annotation.Resource;

@Service
public class CatalogDbTxService {

    @Resource
    @Lazy
    private ICatalogDbService CatalogDbApiService;

    /**
     * 库级元数据：独立事务，立即提交
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createDbAndCommit(CatalogDbSaveReqVO dbScope) {
        try {
            return CatalogDbApiService.createCatalogDb(dbScope);
        } catch (Exception e) {
            // 只影响当前库，不向外抛
            return null;
        }
    }
}
