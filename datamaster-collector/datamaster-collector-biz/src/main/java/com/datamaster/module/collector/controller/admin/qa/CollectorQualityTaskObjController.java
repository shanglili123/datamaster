

package com.datamaster.module.collector.controller.admin.qa;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import cn.hutool.core.date.DateUtil;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjPageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjSaveReqVO;
import com.datamaster.module.collector.convert.qa.CollectorQualityTaskObjConvert;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskObjDO;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskObjService;

/**
 * 数据质量任务-稽查对象Controller
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Tag(name = "数据质量任务-稽查对象")
@RestController
@RequestMapping("/dpp/qualityTaskObj")
@Validated
public class CollectorQualityTaskObjController extends BaseController {
    @Resource
    private ICollectorQualityTaskObjService CollectorQualityTaskObjService;

    @Operation(summary = "查询数据质量任务-稽查对象列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorQualityTaskObjRespVO>> list(CollectorQualityTaskObjPageReqVO CollectorQualityTaskObj) {
        PageResult<CollectorQualityTaskObjDO> page = CollectorQualityTaskObjService.getCollectorQualityTaskObjPage(CollectorQualityTaskObj);
        return CommonResult.success(BeanUtils.toBean(page, CollectorQualityTaskObjRespVO.class));
    }

    @Operation(summary = "导出数据质量任务-稽查对象列表")
    @Log(title = "数据质量任务-稽查对象", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorQualityTaskObjPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorQualityTaskObjDO> list = (List<CollectorQualityTaskObjDO>) CollectorQualityTaskObjService.getCollectorQualityTaskObjPage(exportReqVO).getRows();
        ExcelUtil<CollectorQualityTaskObjRespVO> util = new ExcelUtil<>(CollectorQualityTaskObjRespVO.class);
        util.exportExcel(response, CollectorQualityTaskObjConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据质量任务-稽查对象列表")
    @Log(title = "数据质量任务-稽查对象", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorQualityTaskObjRespVO> util = new ExcelUtil<>(CollectorQualityTaskObjRespVO.class);
        List<CollectorQualityTaskObjRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorQualityTaskObjService.importCollectorQualityTaskObj(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据质量任务-稽查对象详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorQualityTaskObjRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorQualityTaskObjDO CollectorQualityTaskObjDO = CollectorQualityTaskObjService.getCollectorQualityTaskObjById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorQualityTaskObjDO, CollectorQualityTaskObjRespVO.class));
    }

    @Operation(summary = "新增数据质量任务-稽查对象")
    @Log(title = "数据质量任务-稽查对象", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorQualityTaskObjSaveReqVO CollectorQualityTaskObj) {
        CollectorQualityTaskObj.setCreatorId(getUserId());
        CollectorQualityTaskObj.setCreateBy(getNickName());
        CollectorQualityTaskObj.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorQualityTaskObjService.createCollectorQualityTaskObj(CollectorQualityTaskObj));
    }

    @Operation(summary = "修改数据质量任务-稽查对象")
    @Log(title = "数据质量任务-稽查对象", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorQualityTaskObjSaveReqVO CollectorQualityTaskObj) {
        CollectorQualityTaskObj.setUpdatorId(getUserId());
        CollectorQualityTaskObj.setUpdateBy(getNickName());
        CollectorQualityTaskObj.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorQualityTaskObjService.updateCollectorQualityTaskObj(CollectorQualityTaskObj));
    }

    @Operation(summary = "删除数据质量任务-稽查对象")
    @Log(title = "数据质量任务-稽查对象", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorQualityTaskObjService.removeCollectorQualityTaskObj(Arrays.asList(ids)));
    }

}
