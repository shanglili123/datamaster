package com.datamaster.quality.service.quality;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.quality.dal.dataobject.quality.QualityErrorStorageConfigDO;

public interface IQualityErrorStorageConfigService extends IService<QualityErrorStorageConfigDO> {

    QualityErrorStorageConfigDO getEnabledConfig();

    boolean saveOrUpdateConfig(Long datasourceId, String tableName);
}
