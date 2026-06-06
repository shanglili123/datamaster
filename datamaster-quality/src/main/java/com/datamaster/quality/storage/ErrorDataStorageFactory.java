package com.datamaster.quality.storage;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.quality.dal.dataobject.datasource.DatasourceDO;
import com.datamaster.quality.dal.dataobject.quality.QualityErrorStorageConfigDO;
import com.datamaster.quality.service.datasource.IDatasourceQualityService;
import com.datamaster.quality.service.quality.IQualityErrorStorageConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ErrorDataStorageFactory {

    private static final Logger log = LoggerFactory.getLogger(ErrorDataStorageFactory.class);

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Autowired
    private IDatasourceQualityService datasourceQualityService;

    @Autowired
    private IQualityErrorStorageConfigService storageConfigService;

    private volatile ErrorDataStorage storage;

    @PostConstruct
    public void init() {
        initStorage();
    }

    public synchronized void initStorage() {
        QualityErrorStorageConfigDO config = storageConfigService.getEnabledConfig();
        if (config != null && config.getDatasourceId() != null) {
            DatasourceDO datasource = datasourceQualityService.getDatasourceDOById(config.getDatasourceId());
            if (datasource != null) {
                try {
                    DbQueryProperty dbQueryProperty = new DbQueryProperty(
                            datasource.getDatasourceType(),
                            datasource.getIp(),
                            datasource.getPort(),
                            datasource.getDatasourceConfig()
                    );
                    JdbcErrorDataStorage jdbcStorage = new JdbcErrorDataStorage(
                            dataSourceFactory, dbQueryProperty, config.getTableName());
                    jdbcStorage.initTable();
                    storage = jdbcStorage;
                    log.info("错误明细存储已切换为JDBC存储，数据源: {}, 表名: {}",
                            datasource.getDatasourceName(), config.getTableName());
                    return;
                } catch (Exception e) {
                    log.error("初始化JDBC错误明细存储失败: {}", e.getMessage(), e);
                }
            }
        }

        log.warn("未配置错误明细存储，跳过写入");
        storage = new NoopErrorDataStorage();
    }

    public ErrorDataStorage getStorage() {
        if (storage == null) {
            synchronized (this) {
                if (storage == null) {
                    initStorage();
                }
            }
        }
        return storage;
    }

    public void refreshStorage() {
        storage = null;
        initStorage();
    }
}
