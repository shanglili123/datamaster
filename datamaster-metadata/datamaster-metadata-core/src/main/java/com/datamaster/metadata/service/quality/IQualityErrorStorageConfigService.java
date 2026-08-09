package com.datamaster.metadata.service.quality;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.metadata.dal.dataobject.qa.QualityErrorStorageConfigDO;

public interface IQualityErrorStorageConfigService extends IService<QualityErrorStorageConfigDO> {

    QualityErrorStorageConfigDO getEnabledConfig();

    boolean saveOrUpdateConfig(Long datasourceId, String tableName);
}
