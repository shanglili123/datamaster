package com.datamaster.metadata.controller.admin.lineage;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.metadata.api.lineage.dto.LineageGraphDTO;
import com.datamaster.metadata.api.lineage.dto.LineageQueryReqDTO;
import com.datamaster.metadata.api.service.lineage.LineageApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

@Tag(name = "元数据血缘 v2")
@RestController
@RequestMapping("/cat/table/lineage/v2")
public class LineageController {
    @Resource private LineageApiService lineage;

    @Operation(summary = "查询配置推导血缘（非运行时证明）")
    @PreAuthorize("@ss.hasPermi('cat:table:query')")
    @GetMapping("/{id}")
    public CommonResult<LineageGraphDTO> get(@PathVariable("id") Long id, LineageQueryReqDTO request) {
        // Service repeats permission + member + connection + data-scope checks for internal callers.
        return CommonResult.success(lineage.queryByMetadata(id, request));
    }
}
