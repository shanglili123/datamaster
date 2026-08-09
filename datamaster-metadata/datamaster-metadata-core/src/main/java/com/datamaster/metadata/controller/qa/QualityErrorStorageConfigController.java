package com.datamaster.metadata.controller.qa;

import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.httpClient.HeaderEntity;
import com.datamaster.common.httpClient.HttpUtils;
import com.datamaster.metadata.dal.dataobject.qa.QualityErrorStorageConfigDO;
import com.datamaster.metadata.dal.mapper.qa.QualityErrorStorageConfigMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "错误明细存储配置")
@RestController
@RequestMapping("/metadata/errorStorageConfig")
public class QualityErrorStorageConfigController extends BaseController {

    @Value("${path.quality_url}")
    private String qualityUrl;

    @Resource
    private QualityErrorStorageConfigMapper errorStorageConfigMapper;

    @GetMapping("/getConfig")
    public AjaxResult getConfig() {
        List<QualityErrorStorageConfigDO> list = errorStorageConfigMapper.selectList(null);
        QualityErrorStorageConfigDO config = list.isEmpty() ? null : list.get(0);
        if (config == null) {
            return AjaxResult.success();
        }
        return AjaxResult.success(config);
    }

    @PostMapping("/setConfig")
    public AjaxResult setConfig(@RequestParam Long datasourceId,
                                @RequestParam(defaultValue = "quality_error_data") String tableName) {
        List<QualityErrorStorageConfigDO> list = errorStorageConfigMapper.selectList(null);
        QualityErrorStorageConfigDO config;
        if (list.isEmpty()) {
            config = QualityErrorStorageConfigDO.builder()
                    .datasourceId(datasourceId)
                    .tableName(tableName)
                    .build();
            errorStorageConfigMapper.insert(config);
        } else {
            config = list.get(0);
            config.setDatasourceId(datasourceId);
            config.setTableName(tableName);
            errorStorageConfigMapper.updateById(config);
        }

        try {
            List<HeaderEntity> headers = new ArrayList<>();
            HeaderEntity header = new HeaderEntity();
            header.setKey("Content-Type");
            header.setValue("application/json");
            headers.add(header);
            HttpUtils.ResponseObject resp = HttpUtils.sendPost(qualityUrl + "/refreshErrorStorage", null, headers);
            if (resp.getStatus() != 200) {
                return AjaxResult.error("配置已保存，但通知 quality 服务失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("配置已保存，但通知 quality 服务失败：" + e.getMessage());
        }

        return AjaxResult.success("配置成功");
    }
}
