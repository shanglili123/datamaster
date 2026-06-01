package com.datamaster.module.standards.controller.admin.document;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.standards.controller.admin.document.vo.*;
import com.datamaster.module.standards.convert.document.StandardsDocumentConvert;
import com.datamaster.module.standards.dal.dataobject.document.StandardsDocumentDO;
import com.datamaster.module.standards.service.document.IStandardsDocumentService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 标准信息登记Controller
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
@Tag(name = "标准信息登记")
@RestController
@RequestMapping("/std/document")
@Validated
public class StandardsDocumentController extends BaseController {
    @Resource
    private IStandardsDocumentService StandardsDocumentService;

    @Operation(summary = "查询标准信息登记列表")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDocumentRespVO>> list(StandardsDocumentPageReqVO StandardsDocument) {
        PageResult<StandardsDocumentDO> page = StandardsDocumentService.getDpDocumentPage(StandardsDocument);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDocumentRespVO.class));
    }


    @Operation(summary = "查询标准信息登记列表")
    @GetMapping("/getDpDocumentList")
    public CommonResult<List<StandardsDocumentRespVO>> getDpDocumentList(StandardsDocumentPageReqVO StandardsDocument) {
        List<StandardsDocumentDO> list = StandardsDocumentService.getDpDocumentList(StandardsDocument);
        return CommonResult.success(BeanUtils.toBean(list, StandardsDocumentRespVO.class));
    }

    @Operation(summary = "导出标准信息登记列表")
    @Log(title = "标准信息登记", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDocumentPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDocumentDO> list = (List<StandardsDocumentDO>) StandardsDocumentService.getDpDocumentPage(exportReqVO).getRows();
        ExcelUtil<StandardsDocumentRespVO> util = new ExcelUtil<>(StandardsDocumentRespVO.class);
        util.exportExcel(response, StandardsDocumentConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入标准信息登记列表")
    @Log(title = "标准信息登记", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDocumentRespVO> util = new ExcelUtil<>(StandardsDocumentRespVO.class);
        List<StandardsDocumentRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDocumentService.importDpDocument(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取标准信息登记详细信息")
    @GetMapping(value = "/{ID}")
    public CommonResult<StandardsDocumentRespVO> getInfo(@PathVariable("ID") Long ID) {
        StandardsDocumentDO StandardsDocumentDO = StandardsDocumentService.getDpDocumentById(ID);
        return CommonResult.success(BeanUtils.toBean(StandardsDocumentDO, StandardsDocumentRespVO.class));
    }

    @Operation(summary = "新增标准信息登记")
    @Log(title = "标准信息登记", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDocumentSaveReqVO StandardsDocument) {
        StandardsDocument.setCreatorId(getUserId());
        StandardsDocument.setCreateBy(getNickName());
        StandardsDocument.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDocumentService.createDpDocument(StandardsDocument));
    }

    @Operation(summary = "修改标准信息登记")
    @Log(title = "标准信息登记", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDocumentSaveReqVO StandardsDocument) {
        StandardsDocument.setUpdatorId(getUserId());
        StandardsDocument.setUpdateBy(getNickName());
        StandardsDocument.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDocumentService.updateDpDocument(StandardsDocument));
    }
//
//    @Operation(summary = "删除标准信息登记")
//    @PreAuthorize("@ss.hasPermi('dp:document:document:remove')")
//    @Log(title = "标准信息登记", businessType = BusinessType.DELETE)
//    @DeleteMapping("/{IDs}")
//    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
//        return CommonResult.toAjax(StandardsDocumentService.removeDpDocument(Arrays.asList(ids)));
//    }

    @Operation(summary = "删除标准信息登记")
    @Log(title = "标准信息登记", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public CommonResult<Integer> remove(@PathVariable Long id) {
        return CommonResult.toAjax(StandardsDocumentService.removeDpDocument(Arrays.asList(id)));
    }

    @Operation(summary = "查询标准检索列表")
    @GetMapping("/search")
    public CommonResult<PageResult<StandardsDocumentSearchRespVO>> search(StandardsDocumentSearchReqVO StandardsDocument) {
        return CommonResult.success(StandardsDocumentService.getDpDocumentSearchPage(StandardsDocument));
    }
}
