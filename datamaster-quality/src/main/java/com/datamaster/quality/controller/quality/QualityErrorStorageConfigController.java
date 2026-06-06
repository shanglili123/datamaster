package com.datamaster.quality.controller.quality;

import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.quality.dal.dataobject.datasource.DatasourceDO;
import com.datamaster.quality.dal.dataobject.quality.QualityErrorStorageConfigDO;
import com.datamaster.quality.service.datasource.IDatasourceQualityService;
import com.datamaster.quality.service.quality.IQualityErrorStorageConfigService;
import com.datamaster.quality.storage.ErrorDataStorageFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "错误明细存储配置")
@RestController
@RequestMapping("/quality/errorStorageConfig")
public class QualityErrorStorageConfigController extends BaseController {

    @Autowired
    private IQualityErrorStorageConfigService storageConfigService;

    @Autowired
    private IDatasourceQualityService datasourceQualityService;

    @Autowired
    private ErrorDataStorageFactory errorDataStorageFactory;

    @GetMapping("/getConfig")
    public AjaxResult getConfig() {
        QualityErrorStorageConfigDO config = storageConfigService.getEnabledConfig();
        if (config == null) {
            return AjaxResult.success();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("config", config);
        DatasourceDO datasource = datasourceQualityService.getDatasourceDOById(config.getDatasourceId());
        result.put("datasourceName", datasource != null ? datasource.getDatasourceName() : null);
        return AjaxResult.success(result);
    }

    @PostMapping("/setConfig")
    public AjaxResult setConfig(@RequestParam Long datasourceId,
                                @RequestParam(defaultValue = "quality_error_data") String tableName) {
        DatasourceDO datasource = datasourceQualityService.getDatasourceDOById(datasourceId);
        if (datasource == null) {
            return AjaxResult.error("数据源不存在");
        }
        storageConfigService.saveOrUpdateConfig(datasourceId, tableName);
        errorDataStorageFactory.refreshStorage();
        return AjaxResult.success("配置成功");
    }

    @PostMapping("/initTable")
    public AjaxResult initTable() {
        errorDataStorageFactory.refreshStorage();
        boolean available = errorDataStorageFactory.getStorage().isAvailable();
        return AjaxResult.success(available ? "初始化成功" : "初始化失败，请检查配置");
    }
}
