

package com.datamaster.metadata.controller.qa;

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
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjPageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskObjSaveReqVO;
import com.datamaster.metadata.convert.qa.QualityTaskObjConvert;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskObjDO;
import com.datamaster.metadata.service.qa.IQualityTaskObjService;

/**
 * 质量探查任务-稽查对象Controller
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Tag(name = "质量探查任务-稽查对象")
@RestController
@RequestMapping("/metadata/qualityTaskObj")
@Validated
public class QualityTaskObjController extends BaseController {
    @Resource
    private IQualityTaskObjService QualityTaskObjService;

    @Operation(summary = "查询质量探查任务-稽查对象列表")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityTaskObjRespVO>> list(QualityTaskObjPageReqVO QualityTaskObj) {
        PageResult<QualityTaskObjDO> page = QualityTaskObjService.getQualityTaskObjPage(QualityTaskObj);
        return CommonResult.success(BeanUtils.toBean(page, QualityTaskObjRespVO.class));
    }

    @Operation(summary = "导出质量探查任务-稽查对象列表")
    @Log(title = "质量探查任务-稽查对象", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityTaskObjPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityTaskObjDO> list = (List<QualityTaskObjDO>) QualityTaskObjService.getQualityTaskObjPage(exportReqVO).getRows();
        ExcelUtil<QualityTaskObjRespVO> util = new ExcelUtil<>(QualityTaskObjRespVO.class);
        util.exportExcel(response, QualityTaskObjConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入质量探查任务-稽查对象列表")
    @Log(title = "质量探查任务-稽查对象", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<QualityTaskObjRespVO> util = new ExcelUtil<>(QualityTaskObjRespVO.class);
        List<QualityTaskObjRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = QualityTaskObjService.importQualityTaskObj(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取质量探查任务-稽查对象详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityTaskObjRespVO> getInfo(@PathVariable("id") Long id) {
        QualityTaskObjDO QualityTaskObjDO = QualityTaskObjService.getQualityTaskObjById(id);
        return CommonResult.success(BeanUtils.toBean(QualityTaskObjDO, QualityTaskObjRespVO.class));
    }

    @Operation(summary = "新增质量探查任务-稽查对象")
    @Log(title = "质量探查任务-稽查对象", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody QualityTaskObjSaveReqVO QualityTaskObj) {
        QualityTaskObj.setCreatorId(getUserId());
        QualityTaskObj.setCreateBy(getNickName());
        QualityTaskObj.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(QualityTaskObjService.createQualityTaskObj(QualityTaskObj));
    }

    @Operation(summary = "修改质量探查任务-稽查对象")
    @Log(title = "质量探查任务-稽查对象", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody QualityTaskObjSaveReqVO QualityTaskObj) {
        QualityTaskObj.setUpdatorId(getUserId());
        QualityTaskObj.setUpdateBy(getNickName());
        QualityTaskObj.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(QualityTaskObjService.updateQualityTaskObj(QualityTaskObj));
    }

    @Operation(summary = "删除质量探查任务-稽查对象")
    @Log(title = "质量探查任务-稽查对象", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(QualityTaskObjService.removeQualityTaskObj(Arrays.asList(ids)));
    }

}
