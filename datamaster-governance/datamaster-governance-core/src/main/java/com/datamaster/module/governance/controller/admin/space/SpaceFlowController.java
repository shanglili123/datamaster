package com.datamaster.module.governance.controller.admin.space;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi.*;
import com.datamaster.module.governance.service.flow.SpaceFlowService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/tax/space-flow")
public class SpaceFlowController {
    @Resource private SpaceFlowService service;
    @GetMapping
    @PreAuthorize("@ss.hasPermi('tax:space:query')")
    public CommonResult<Map<String,Object>> get(@RequestParam Long spaceId, @RequestParam(required=false) String spaceCode) {
        return CommonResult.success(service.get(spaceId, spaceCode));
    }
    @PutMapping
    @PreAuthorize("@ss.hasPermi('tax:space:edit')")
    public CommonResult<Map<String,Object>> save(@RequestBody SaveRequest request) { return CommonResult.success(service.save(request)); }
    @PostMapping("/validate")
    @PreAuthorize("@ss.hasPermi('tax:space:edit')")
    public CommonResult<Boolean> validate(@RequestBody SaveRequest request) {
        service.validateDraft(request); return CommonResult.success(true);
    }
    @PostMapping("/publish-preview")
    @PreAuthorize("@ss.hasPermi('tax:space:edit')")
    public CommonResult<Map<String,Object>> preview(@RequestBody SaveRequest request) {
        return CommonResult.success(service.preview(request));
    }
    @GetMapping("/consumer-impacts")
    @PreAuthorize("@ss.hasPermi('tax:space:edit')")
    public CommonResult<Map<String,Object>> impacts(@RequestParam Long spaceId,
            @RequestParam(required=false) String spaceCode, @RequestParam Integer revision,
            @RequestParam String type, @RequestParam(defaultValue="1") int pageNum,
            @RequestParam(defaultValue="10") int pageSize) {
        SaveRequest request = new SaveRequest(); request.setSpaceId(spaceId); request.setSpaceCode(spaceCode); request.setRevision(revision);
        return CommonResult.success(service.impacts(request, type, pageNum, pageSize));
    }
    @GetMapping("/versions")
    @PreAuthorize("@ss.hasPermi('tax:space:query')")
    public CommonResult<Map<String,Object>> versions(@RequestParam Long spaceId,
            @RequestParam(required=false) String spaceCode, @RequestParam(defaultValue="1") int pageNum,
            @RequestParam(defaultValue="10") int pageSize) {
        return CommonResult.success(service.versions(spaceId, spaceCode, pageNum, pageSize));
    }
    @PostMapping("/publish")
    @PreAuthorize("@ss.hasPermi('tax:space:edit')")
    public CommonResult<Map<String,Object>> publish(@RequestBody SaveRequest request) { return CommonResult.success(service.publish(request)); }
    @GetMapping("/quality-confirmations")
    @PreAuthorize("@ss.hasPermi('tax:space:query')")
    public CommonResult<Map<String,Object>> qualityHistory(@RequestParam Long spaceId,
            @RequestParam(required=false) String spaceCode, @RequestParam Long datasourceId,
            @RequestParam String tableName, @RequestParam(defaultValue="1") int pageNum,
            @RequestParam(defaultValue="10") int pageSize) {
        return CommonResult.success(service.qualityHistory(spaceId, spaceCode, datasourceId, tableName, pageNum, pageSize));
    }
    @PostMapping("/quality-confirmation")
    @PreAuthorize("@ss.hasPermi('tax:space:edit')")
    public CommonResult<Boolean> quality(@RequestBody QualityRequest request) { service.confirmQuality(request); return CommonResult.success(true); }
}
