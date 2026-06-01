

package com.datamaster.module.taxonomy.controller.admin.client;

import cn.hutool.core.util.IdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientRespVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientSaveReqVO;
import com.datamaster.module.taxonomy.convert.client.TaxonomyClientConvert;
import com.datamaster.module.taxonomy.dal.dataobject.client.TaxonomyClientDO;
import com.datamaster.module.taxonomy.service.client.ITaxonomyClientService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 应用管理Controller
 *
 * @author DATAMASTER
 * @date 2025-02-18
 */
@Tag(name = "应用管理")
@RestController
@RequestMapping("/tax/client")
@Validated
public class TaxonomyClientController extends BaseController {
    @Resource
    private ITaxonomyClientService TaxonomyClientService;

    @Operation(summary = "查询应用管理列表")
    @PreAuthorize("@ss.hasPermi('att:client:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomyClientRespVO>> list(TaxonomyClientPageReqVO TaxonomyClient) {
        PageResult<TaxonomyClientDO> page = TaxonomyClientService.getAttClientPage(TaxonomyClient);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyClientRespVO.class));
    }

    @Operation(summary = "导出应用管理列表")
    @PreAuthorize("@ss.hasPermi('att:client:export')")
    @Log(title = "应用管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyClientPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyClientDO> list = (List<TaxonomyClientDO>) TaxonomyClientService.getAttClientPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyClientRespVO> util = new ExcelUtil<>(TaxonomyClientRespVO.class);
        util.exportExcel(response, TaxonomyClientConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入应用管理列表")
    @PreAuthorize("@ss.hasPermi('att:client:import')")
    @Log(title = "应用管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyClientRespVO> util = new ExcelUtil<>(TaxonomyClientRespVO.class);
        List<TaxonomyClientRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyClientService.importAttClient(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取应用管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:client:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyClientRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyClientDO TaxonomyClientDO = TaxonomyClientService.getAttClientById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomyClientDO, TaxonomyClientRespVO.class));
    }

    @Operation(summary = "新增应用管理")
    @PreAuthorize("@ss.hasPermi('att:client:add')")
    @Log(title = "应用管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyClientSaveReqVO TaxonomyClient) {
        return CommonResult.toAjax(TaxonomyClientService.createAttClient(TaxonomyClient));
    }

    @Operation(summary = "修改应用管理")
    @PreAuthorize("@ss.hasPermi('att:client:edit')")
    @Log(title = "应用管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyClientSaveReqVO TaxonomyClient) {
        return CommonResult.toAjax(TaxonomyClientService.updateAttClient(TaxonomyClient));
    }

    @Operation(summary = "删除应用管理")
    @PreAuthorize("@ss.hasPermi('att:client:remove')")
    @Log(title = "应用管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyClientService.removeAttClient(Arrays.asList(ids)));
    }

    @Operation(summary = "重置应用秘钥")
    @PreAuthorize("@ss.hasPermi('att:client:edit')")
    @Log(title = "重置应用秘钥", businessType = BusinessType.UPDATE)
    @PostMapping("/reset/secret")
    public CommonResult<String> resetSecret(Long id) {
        TaxonomyClientDO client = TaxonomyClientService.getAttClientById(id);
        client.setSecret(IdUtil.fastSimpleUUID());
        TaxonomyClientService.updateById(client);
        return CommonResult.success(client.getSecret());
    }
}
