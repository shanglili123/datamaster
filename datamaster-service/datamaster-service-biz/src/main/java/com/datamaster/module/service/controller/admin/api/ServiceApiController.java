

package com.datamaster.module.service.controller.admin.api;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
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
import com.datamaster.common.utils.AesEncryptUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.service.controller.admin.api.vo.ServiceApiPageReqVO;
import com.datamaster.module.service.controller.admin.api.vo.ServiceApiReqVO;
import com.datamaster.module.service.controller.admin.api.vo.ServiceApiRespVO;
import com.datamaster.module.service.controller.admin.api.vo.SqlParseVo;
import com.datamaster.module.service.convert.api.ServiceApiConvert;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.dataobject.api.SqlParseDto;
import com.datamaster.module.service.service.api.IServiceApiService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * API服务Controller
 *
 * @author lhs
 * @date 2025-02-12
 */
@Tag(name = "API服务")
@RestController
@RequestMapping("/svc/api")
@Validated
public class ServiceApiController extends BaseController {
    @Resource
    private IServiceApiService ServiceApiService;

    @Operation(summary = "查询API服务列表")
    @PreAuthorize("@ss.hasPermi('ds:api:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ServiceApiRespVO>> list(ServiceApiPageReqVO ServiceApi) {
        PageResult<ServiceApiDO> page = ServiceApiService.getServiceApiPage(ServiceApi);
        return CommonResult.success(BeanUtils.toBean(page, ServiceApiRespVO.class));
    }

    @Operation(summary = "导出API服务列表")
    @PreAuthorize("@ss.hasPermi('ds:api:export')")
    @Log(title = "API服务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ServiceApiPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ServiceApiDO> list = (List<ServiceApiDO>) ServiceApiService.getServiceApiPage(exportReqVO).getRows();
        ExcelUtil<ServiceApiRespVO> util = new ExcelUtil<>(ServiceApiRespVO.class);
        util.exportExcel(response, ServiceApiConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入API服务列表")
    @PreAuthorize("@ss.hasPermi('ds:api:import')")
    @Log(title = "API服务", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<ServiceApiRespVO> util = new ExcelUtil<>(ServiceApiRespVO.class);
        List<ServiceApiRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ServiceApiService.importServiceApi(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取API服务详细信息")
    @PreAuthorize("@ss.hasPermi('ds:api:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<ServiceApiRespVO> getInfo(@PathVariable("ID") Long ID) {
        ServiceApiDO ServiceApiDO = ServiceApiService.getServiceApiById(ID);
        return CommonResult.success(BeanUtils.toBean(ServiceApiDO, ServiceApiRespVO.class));
    }

    @Operation(summary = "根据名称，版本号，路径进行判断是否重复")
    @PreAuthorize("@ss.hasPermi('ds:api:query')")
    @PostMapping(value = "/repeatFlag")
    public AjaxResult repeatFlag(@RequestBody JSONObject jsonObject) {
        if (StringUtils.isBlank(jsonObject.getString("name"))) {
            return AjaxResult.error("请携带API名称");
        }
        if (StringUtils.isBlank(jsonObject.getString("apiVersion"))) {
            return AjaxResult.error("请携带API版本号");
        }
        if (StringUtils.isBlank(jsonObject.getString("apiUrl"))) {
            return AjaxResult.error("请携带API路径");
        }
        ServiceApiDO ServiceApiDO = ServiceApiService.repeatFlag(jsonObject);
        if (ServiceApiDO != null) {
            return AjaxResult.error("名称、版本号、路径以存在");
        }
        return AjaxResult.success(ServiceApiDO);
    }


    @Operation(summary = "删除API服务")
    @PreAuthorize("@ss.hasPermi('ds:api:remove')")
    @Log(title = "API服务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public CommonResult<Integer> remove(@PathVariable(name = "id") Long[] id) {
        return CommonResult.toAjax(ServiceApiService.removeServiceApi(Arrays.asList(id)));
    }

    /**
     * SQL解析
     *
     * @param sqlParseDto
     * @return
     */
    @Operation(summary = "SQL解析")
    @PreAuthorize("@ss.hasPermi('da:asset:list')")
    @PostMapping("/sqlParse")
    public AjaxResult sqlParse(@RequestBody @Validated SqlParseDto sqlParseDto) {
        /*try {
            sqlParseDto.setSqlText(AesEncryptUtil.desEncrypt(sqlParseDto.getSqlText()).trim());
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        SqlParseVo sqlParseVo = ServiceApiService.sqlParse(sqlParseDto);
        return AjaxResult.success(sqlParseVo);
    }

    @Operation(summary = "接口调试")
    @PreAuthorize("@ss.hasPermi('da:asset:edit')")
    @PostMapping("/serviceTesting")
    public AjaxResult serviceTesting(@RequestBody ServiceApiDO dataApi) {
        if (dataApi.getExecuteConfig() != null && StringUtils.isNotBlank(dataApi.getExecuteConfig().getSqlText())) {
            try {
                dataApi.getExecuteConfig().setSqlText(AesEncryptUtil.desEncrypt(dataApi.getExecuteConfig().getSqlText()).trim());
            } catch (Exception e) {
                logger.error("失败", e);
            }
        } else {
            if (dataApi.getExecuteConfig() != null) {
                dataApi.getExecuteConfig().setSqlText("");
            }
        }
        Object value = ServiceApiService.serviceTesting(dataApi);
        return AjaxResult.success(value);
    }


    @Operation(summary = "接口调试")
    @PreAuthorize("@ss.hasPermi('ds:api:query')")
    @PostMapping("/queryServiceForwarding")
    public void queryServiceForwarding(HttpServletResponse response, @Valid @RequestBody ServiceApiReqVO ServiceApiReqVO) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        ServiceApiService.queryServiceForwarding(response, ServiceApiReqVO);
    }


    /**
     * 添加
     *
     * @param dataApi
     * @return
     */
    @Operation(summary = "保存Api信息")
    @PreAuthorize("@ss.hasPermi('da:asset:edit')")
    @PostMapping()
    public AjaxResult saveDataApi(@RequestBody ServiceApiDO dataApi) {
//        if (dataApi.getExecuteConfig() != null && StringUtils.isNotBlank(dataApi.getExecuteConfig().getSqlText())) {
//            try {
//                dataApi.getExecuteConfig().setSqlText(AesEncryptUtil.desEncrypt(dataApi.getExecuteConfig().getSqlText()).trim());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        dataApi.setCreateBy(getUsername());
        dataApi.setCreatorId(getUserId());
        dataApi.setCreateTime(DateUtil.date());
        return ServiceApiService.saveDataApi(dataApi);
    }


    /**
     * 修改
     *
     * @param dataApi
     * @return
     */
    @Operation(summary = "修改Api信息")
    @PutMapping
    public AjaxResult updateDataApi(@RequestBody ServiceApiDO dataApi) {
//        if (dataApi.getExecuteConfig() != null && StringUtils.isNotBlank(dataApi.getExecuteConfig().getSqlText())) {
//            try {
//                dataApi.getExecuteConfig().setSqlText(AesEncryptUtil.desEncrypt(dataApi.getExecuteConfig().getSqlText()).trim());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        dataApi.setUpdatorId(getUserId());
        dataApi.setUpdateBy(getUsername());
        dataApi.setUpdateTime(DateUtil.date());
        return ServiceApiService.updateDataApi(dataApi);
    }


    /**
     * 发布接口
     *
     * @param id
     * @return
     */

    @GetMapping(value = "/release/{id}")
    @PreAuthorize("@ss.hasPermi('da:asset:edit')")
    public AjaxResult releaseDataApi(@PathVariable String id) {
        ServiceApiService.releaseDataApi(id, getUserId(), getUsername());
        return AjaxResult.success();
    }

    /**
     * 注销接口
     *
     * @param id
     * @return
     */
    @PreAuthorize("@ss.hasPermi('da:asset:edit')")
    @GetMapping(value = "/cancel/{id}")
    public AjaxResult cancelDataApi(@PathVariable String id) {
        ServiceApiService.cancelDataApi(id, getUserId(), getUsername());
        return AjaxResult.success();
    }

    @Operation(summary = "查询API下拉选服务列表")
    @GetMapping("/selectList")
    public CommonResult<List<ServiceApiRespVO>> selectList(String name) {
        IPage<ServiceApiDO> page = ServiceApiService.page(new Page(1, 20), Wrappers.lambdaQuery(ServiceApiDO.class)
                .like(StringUtils.isNotBlank(name),ServiceApiDO::getName, name)
                .select(ServiceApiDO::getId, ServiceApiDO::getName));
        return CommonResult.success(BeanUtils.toBean(page.getRecords(), ServiceApiRespVO.class));
    }


}
