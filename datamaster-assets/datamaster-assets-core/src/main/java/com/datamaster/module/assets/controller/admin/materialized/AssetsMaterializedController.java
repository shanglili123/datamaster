package com.datamaster.module.assets.controller.admin.materialized;

import cn.hutool.core.date.DateUtil;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.assets.controller.admin.materialized.vo.AssetsMaterializedReqVO;
import com.datamaster.module.assets.service.materialized.IAssetsMaterializedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 资产物化Controller
 *
 * @author DATAMASTER
 * @date 2026-08-01
 */
@Tag(name = "资产物化")
@RestController
@RequestMapping("/assets/materialized")
@Validated
public class AssetsMaterializedController extends BaseController {

    @Resource
    private IAssetsMaterializedService assetsMaterializedService;

    @Operation(summary = "物化建表")
    @PostMapping("/createMaterializedTable")
    public CommonResult<Long> createMaterializedTable(@Valid @RequestBody AssetsMaterializedReqVO req) {
        req.setCreatorId(getUserId());
        req.setCreateBy(getNickName());
        req.setCreateTime(DateUtil.date());
        return CommonResult.success(assetsMaterializedService.createMaterializedTable(req));
    }

}
