package com.datamaster.module.assets.controller.admin.assetchild.video;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;

import cn.hutool.core.date.DateUtil;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoSaveReqVO;
import com.datamaster.module.assets.convert.assetchild.video.AssetsAssetVideoConvert;
import com.datamaster.module.assets.dal.dataobject.assetchild.video.AssetsAssetVideoDO;
import com.datamaster.module.assets.service.assetchild.video.IAssetsAssetVideoService;

/**
 * 数据资产-视频数据Controller * * @author DATAMASTER * @date 2025-04-14
 */
@Tag(name = "数据资产-视频数据")
@RestController
@RequestMapping("/ast/assetVideo")
@Validated
public class AssetsAssetVideoController extends BaseController {
    @Resource
    private IAssetsAssetVideoService AssetsAssetVideoService;

    @Operation(summary = "查询数据资产-视频数据列表")
    @PreAuthorize("@ss.hasPermi('da:assetVideo:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetVideoRespVO>> list(AssetsAssetVideoPageReqVO AssetsAssetVideo) {
        PageResult<AssetsAssetVideoDO> page = AssetsAssetVideoService.getAssetVideoPage(AssetsAssetVideo);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetVideoRespVO.class));
    }

    @Operation(summary = "导出数据资产-视频数据列表")
    @PreAuthorize("@ss.hasPermi('da:assetVideo:export')")
    @Log(title = "数据资产-视频数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetVideoPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetVideoDO> list = (List<AssetsAssetVideoDO>) AssetsAssetVideoService.getAssetVideoPage(exportReqVO).getRows();
        ExcelUtil<AssetsAssetVideoRespVO> util = new ExcelUtil<>(AssetsAssetVideoRespVO.class);
        util.exportExcel(response, AssetsAssetVideoConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据资产-视频数据列表")
    @PreAuthorize("@ss.hasPermi('da:assetVideo:import')")
    @Log(title = "数据资产-视频数据", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetVideoRespVO> util = new ExcelUtil<>(AssetsAssetVideoRespVO.class);
        List<AssetsAssetVideoRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetVideoService.importAssetVideo(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据资产-视频数据详细信息")
    @PreAuthorize("@ss.hasPermi('da:assetVideo:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetVideoRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetVideoDO AssetsAssetVideoDO = AssetsAssetVideoService.getAssetVideoById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsAssetVideoDO, AssetsAssetVideoRespVO.class));
    }

    @Operation(summary = "新增数据资产-视频数据")
    @PreAuthorize("@ss.hasPermi('da:assetVideo:add')")
    @Log(title = "数据资产-视频数据", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetVideoSaveReqVO AssetsAssetVideo) {
        AssetsAssetVideo.setCreatorId(getUserId());
        AssetsAssetVideo.setCreateBy(getNickName());
        AssetsAssetVideo.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetVideoService.createAssetVideo(AssetsAssetVideo));
    }

    @Operation(summary = "修改数据资产-视频数据")
    @PreAuthorize("@ss.hasPermi('da:assetVideo:edit')")
    @Log(title = "数据资产-视频数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetVideoSaveReqVO AssetsAssetVideo) {
        AssetsAssetVideo.setUpdatorId(getUserId());
        AssetsAssetVideo.setUpdateBy(getNickName());
        AssetsAssetVideo.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetVideoService.updateAssetVideo(AssetsAssetVideo));
    }

    @Operation(summary = "删除数据资产-视频数据")
    @PreAuthorize("@ss.hasPermi('da:assetVideo:remove')")
    @Log(title = "数据资产-视频数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsAssetVideoService.removeAssetVideo(Arrays.asList(ids)));
    }

    @Operation(summary = "删除数据资产-视频")
    @PreAuthorize("@ss.hasPermi('da:asset:edit')")
    @PostMapping("/queryServiceForwarding")
    public void queryServiceForwarding(HttpServletResponse response, @Valid @RequestBody AssetsAssetVideoReqVO AssetsAssetVideoReqVO) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        AssetsAssetVideoService.queryServiceForwarding(response, AssetsAssetVideoReqVO);
    }
}