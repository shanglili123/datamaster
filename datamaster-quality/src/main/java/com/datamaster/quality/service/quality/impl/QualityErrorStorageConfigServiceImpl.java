package com.datamaster.quality.service.quality.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.quality.dal.dataobject.quality.QualityErrorStorageConfigDO;
import com.datamaster.quality.dal.mapper.quality.QualityErrorStorageConfigMapper;
import com.datamaster.quality.service.quality.IQualityErrorStorageConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QualityErrorStorageConfigServiceImpl
        extends ServiceImpl<QualityErrorStorageConfigMapper, QualityErrorStorageConfigDO>
        implements IQualityErrorStorageConfigService {

    @Override
    public QualityErrorStorageConfigDO getEnabledConfig() {
        List<QualityErrorStorageConfigDO> list = lambdaQuery()
                .orderByDesc(QualityErrorStorageConfigDO::getCreateTime)
                .last("LIMIT 1")
                .list();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public boolean saveOrUpdateConfig(Long datasourceId, String tableName) {
        List<QualityErrorStorageConfigDO> list = lambdaQuery().list();
        QualityErrorStorageConfigDO config;
        if (list.isEmpty()) {
            config = QualityErrorStorageConfigDO.builder()
                    .datasourceId(datasourceId)
                    .tableName(tableName)
                    .enabled("1")
                    .build();
            return save(config);
        }
        config = list.get(0);
        config.setDatasourceId(datasourceId);
        config.setTableName(tableName);
        config.setEnabled("1");
        return updateById(config);
    }
}
