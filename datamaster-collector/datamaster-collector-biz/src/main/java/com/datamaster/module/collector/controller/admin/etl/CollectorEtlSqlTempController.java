

package com.datamaster.module.collector.controller.admin.etl;

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempSaveReqVO;
import com.datamaster.module.collector.convert.etl.CollectorEtlSqlTempConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSqlTempDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlSqlTempService;

/**
 * 数据集成SQL模版Controller
 *
 * @author FXB
 * @date 2025-06-25
 */
@Tag(name = "数据集成SQL模版")
@RestController
@RequestMapping("/dpp/etlSqlTemp")
@Validated
public class CollectorEtlSqlTempController extends BaseController {
    @Resource
    private ICollectorEtlSqlTempService CollectorEtlSqlTempService;

    @Operation(summary = "查询数据集成SQL模版列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlSqlTempRespVO>> list(CollectorEtlSqlTempPageReqVO CollectorEtlSqlTemp) {
        PageResult<CollectorEtlSqlTempDO> page = CollectorEtlSqlTempService.getCollectorEtlSqlTempPage(CollectorEtlSqlTemp);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlSqlTempRespVO.class));
    }


    @Operation(summary = "新增数据集成SQL模版")
    @Log(title = "数据集成SQL模版", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEtlSqlTempSaveReqVO CollectorEtlSqlTemp) {
        CollectorEtlSqlTemp.setCreatorId(getUserId());
        CollectorEtlSqlTemp.setCreateBy(getNickName());
        CollectorEtlSqlTemp.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlSqlTempService.createCollectorEtlSqlTemp(CollectorEtlSqlTemp));
    }

    @Operation(summary = "修改数据集成SQL模版")
    @Log(title = "数据集成SQL模版", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEtlSqlTempSaveReqVO CollectorEtlSqlTemp) {
        CollectorEtlSqlTemp.setUpdatorId(getUserId());
        CollectorEtlSqlTemp.setUpdateBy(getNickName());
        CollectorEtlSqlTemp.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlSqlTempService.updateCollectorEtlSqlTemp(CollectorEtlSqlTemp));
    }

    @Operation(summary = "删除数据集成SQL模版")
    @Log(title = "数据集成SQL模版", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlSqlTempService.removeCollectorEtlSqlTemp(Arrays.asList(ids)));
    }

}
