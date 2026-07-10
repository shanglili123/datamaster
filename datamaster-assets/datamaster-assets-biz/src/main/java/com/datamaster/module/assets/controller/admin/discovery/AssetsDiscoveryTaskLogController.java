package com.datamaster.module.assets.controller.admin.discovery;import cn.hutool.core.date.DateUtil;import io.swagger.v3.oas.annotations.Operation;import io.swagger.v3.oas.annotations.tags.Tag;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.validation.annotation.Validated;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;import com.datamaster.common.annotation.Log;import com.datamaster.common.core.controller.BaseController;import com.datamaster.common.core.domain.AjaxResult;import com.datamaster.common.core.domain.CommonResult;import com.datamaster.common.core.domain.ReturnT;import com.datamaster.common.core.page.PageParam;import com.datamaster.common.core.page.PageResult;import com.datamaster.common.enums.BusinessType;import com.datamaster.common.utils.object.BeanUtils;import com.datamaster.common.utils.poi.ExcelUtil;import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogPageReqVO;import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogRespVO;import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogSaveReqVO;import com.datamaster.module.assets.controller.admin.discovery.vo.LogResult;import com.datamaster.module.assets.convert.discovery.AssetsDiscoveryTaskLogConvert;import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskLogDO;import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryTaskLogService;import javax.annotation.Resource;import javax.servlet.http.HttpServletResponse;import javax.validation.Valid;import java.io.*;import java.util.Arrays;import java.util.List;/** * 数据发现任务日志Controller * * @author DATAMASTER * @date 2025-02-17 */

@Tag(name = "")
@RestController
@RequestMapping("/ast/discoveryTaskLog")
@Validated
public class AssetsDiscoveryTaskLogController extends BaseController {
    @Resource
    private IAssetsDiscoveryTaskLogService AssetsDiscoveryTaskLogService;

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:discoveryTaskLog:list')")
    @GetMapping("/list")    public CommonResult<PageResult<AssetsDiscoveryTaskLogRespVO>> list(AssetsDiscoveryTaskLogPageReqVO AssetsDiscoveryTaskLog) {        PageResult<AssetsDiscoveryTaskLogDO> page = AssetsDiscoveryTaskLogService.getDaDiscoveryTaskLogPage(AssetsDiscoveryTaskLog);        return CommonResult.success(BeanUtils.toBean(page, AssetsDiscoveryTaskLogRespVO.class));    }
@Operation(summary = "")
@PreAuthorize("@ss.hasPermi('da:discoveryTaskLog:export')")
@Log(title = "", businessType = BusinessType.EXPORT)
@PostMapping("/export")    public void export(HttpServletResponse response, AssetsDiscoveryTaskLogPageReqVO exportReqVO) {        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);        List<AssetsDiscoveryTaskLogDO> list = (List<AssetsDiscoveryTaskLogDO>) AssetsDiscoveryTaskLogService                .getDaDiscoveryTaskLogPage(exportReqVO).getRows();        ExcelUtil<AssetsDiscoveryTaskLogRespVO> util = new ExcelUtil<>(AssetsDiscoveryTaskLogRespVO.class);        util.exportExcel(response, AssetsDiscoveryTaskLogConvert.INSTANCE.convertToRespVOList(list), "");    }
@Operation(summary = "")
@PreAuthorize("@ss.hasPermi('da:discoveryTaskLog:import')")
@Log(title = "", businessType = BusinessType.IMPORT)
@PostMapping("/importData")    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {        ExcelUtil<AssetsDiscoveryTaskLogRespVO> util = new ExcelUtil<>(AssetsDiscoveryTaskLogRespVO.class);        List<AssetsDiscoveryTaskLogRespVO> importExcelList = util.importExcel(file.getInputStream());        String operName = getUsername();        String message = AssetsDiscoveryTaskLogService.importDaDiscoveryTaskLog(importExcelList, updateSupport, operName);        return success(message);    }
@Operation(summary = "")
@PreAuthorize("@ss.hasPermi('da:discoveryTaskLog:query')")
@GetMapping(value = "/{id}")    public CommonResult<AssetsDiscoveryTaskLogRespVO> getInfo(@PathVariable("id") Long id) {        AssetsDiscoveryTaskLogDO AssetsDiscoveryTaskLogDO = AssetsDiscoveryTaskLogService.getDaDiscoveryTaskLogById(id);        return CommonResult.success(BeanUtils.toBean(AssetsDiscoveryTaskLogDO, AssetsDiscoveryTaskLogRespVO.class));    }
@Operation(summary = "")
@PreAuthorize("@ss.hasPermi('da:discoveryTaskLog:add')")
@Log(title = "", businessType = BusinessType.INSERT)
@PostMapping    public CommonResult<Long> add(@Valid
    @RequestBody AssetsDiscoveryTaskLogSaveReqVO AssetsDiscoveryTaskLog) {        AssetsDiscoveryTaskLog.setCreatorId(getUserId());        AssetsDiscoveryTaskLog.setCreateBy(getNickName());        AssetsDiscoveryTaskLog.setCreateTime(DateUtil.date());        return CommonResult.toAjax(AssetsDiscoveryTaskLogService.createDaDiscoveryTaskLog(AssetsDiscoveryTaskLog));    }
@Operation(summary = "")
@PreAuthorize("@ss.hasPermi('da:discoveryTaskLog:edit')")
@Log(title = "", businessType = BusinessType.UPDATE)
@PutMapping    public CommonResult<Integer> edit(@Valid
    @RequestBody AssetsDiscoveryTaskLogSaveReqVO AssetsDiscoveryTaskLog) {        AssetsDiscoveryTaskLog.setUpdatorId(getUserId());        AssetsDiscoveryTaskLog.setUpdateBy(getNickName());        AssetsDiscoveryTaskLog.setUpdateTime(DateUtil.date());        return CommonResult.toAjax(AssetsDiscoveryTaskLogService.updateDaDiscoveryTaskLog(AssetsDiscoveryTaskLog));    }
@Operation(summary = "")
@PreAuthorize("@ss.hasPermi('da:discoveryTaskLog:remove')")
@Log(title = "", businessType = BusinessType.DELETE)
@DeleteMapping("/{ids}")    public CommonResult<Integer> remove(@PathVariable Long[] ids) {        return CommonResult.toAjax(AssetsDiscoveryTaskLogService.removeDaDiscoveryTaskLog(Arrays.asList(ids)));
    }
@PreAuthorize("@ss.hasPermi('da:discoveryTaskLog:list')")
@RequestMapping(value = "/logDetailCat", method = RequestMethod.GET)
@Operation(summary = "")    public ReturnT<LogResult> logDetailCat(Long id, Long dsTaskInstanceId, String handleMsg) {
// 添加日志审计功能
    try {
    String logContent = AssetsDiscoveryTaskLogService.getLogInfo(id, dsTaskInstanceId, handleMsg);
// @TODO 查看日志
    ReturnT<LogResult> returnT = new ReturnT<>(ReturnT.SUCCESS_CODE, "查询日志成功");
    LogResult logResult = new LogResult(0, 0, logContent, true);
    returnT.setContent(logResult);
    return returnT;
    } catch (Exception e) {
    logger.error(e.getMessage(), e);
    return new ReturnT<>(ReturnT.FAIL_CODE, "暂未找到日志文件信息");
        }    }
@PreAuthorize("@ss.hasPermi('da:discoveryTaskLog:list')")
@RequestMapping(value = "/downloadLog", method = {RequestMethod.GET, RequestMethod.POST})
@Operation(summary = "")    public void downloadLog(HttpServletResponse response, Long id, Long dsTaskInstanceId, String handleMsg) {
// 添加日志审计功能
    try {
// 设置响应的内容类型为文件下载
    response.setContentType("application/octet-stream");

// 设置下载文件名
    String fileName = "metadata-discovery.log";
    response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

// 创建文件输入流
    String logContent = AssetsDiscoveryTaskLogService.downloadLog(id, dsTaskInstanceId, handleMsg);
    try (InputStream in = new ByteArrayInputStream((logContent == null ? "" : logContent).getBytes("UTF-8"));
    OutputStream out = response.getOutputStream()) {
    byte[] buffer = new byte[1024];
    int length;

// 将文件内容写入输出流
    while ((length = in.read(buffer)) != -1) {
    out.write(buffer, 0, length);
                    }                }
    } catch (Exception e) {
    logger.error(e.getMessage(), e);
    try {
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    response.getWriter().write("文件下载失败：" + e.getMessage());
    } catch (IOException ioException) {
    logger.error("写入错误信息失败", ioException);
            }        }    }
@Operation(summary = "")
//
    @PreAuthorize("@ss.hasPermi('dpp:etlNodeInstance:query')")
@GetMapping(value = "/log/{id}")    public AjaxResult getLogInfo(@PathVariable("id") Long id) {        return AjaxResult.success(AssetsDiscoveryTaskLogService.getLogInfo(id));
    }}
