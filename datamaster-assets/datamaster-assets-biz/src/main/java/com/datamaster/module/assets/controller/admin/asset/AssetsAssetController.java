package com.datamaster.module.assets.controller.admin.asset;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
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
import com.datamaster.common.core.domain.TreeData;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.text.Convert;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.api.Rel.dto.TaxonomyTagAssetRelReqDTO;
import com.datamaster.module.taxonomy.api.Rel.dto.TaxonomyTagAssetRelRespDTO;
import com.datamaster.module.taxonomy.api.service.cat.tag.ITaxonomyTagApiService;
import com.datamaster.module.taxonomy.api.service.cat.tagRel.ITaxonomyTagAssetRelApiService;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetPageReqVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetRespVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnRelRuleVO;
import com.datamaster.module.assets.convert.asset.AssetsAssetConvert;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.neo4j.dto.LineageDTO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据资产Controller * * @author lhs * @date 2025-01-21
 */

@Tag(name = "")
@RestController
@RequestMapping("/da/asset")
@Validated
public class AssetsAssetController extends BaseController {
    @Resource
    private IAssetsAssetService AssetsAssetService;
    @Resource
    private ITaxonomyTagAssetRelApiService attTagAssetRelApiService;
    @Resource
    private ITaxonomyTagApiService attTagApiService;

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetRespVO>> list(AssetsAssetPageReqVO AssetsAsset) {
        PageResult<AssetsAssetDO> page = AssetsAssetService.getDaAssetPage(AssetsAsset, "1");
        PageResult<AssetsAssetRespVO> result = BeanUtils.toBean(page, AssetsAssetRespVO.class);
        if (result.getRows().size() > 0) {
            for (AssetsAssetRespVO item : result.getRows()) {
                if (StringUtils.isNotBlank(item.getTags())) {
                    JSONArray tags = JSONArray.parse(item.getTags());
                    item.setTagIds(tags.stream().map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("tagId")).collect(Collectors.toList()));
                    item.setTagNames(tags.stream().map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("tagName")).collect(Collectors.toList()));
                }
            }
        }
        return CommonResult.success(result);
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:list')")
    @GetMapping("/listAll")
    public CommonResult<List<AssetsAssetRespVO>> listAll(AssetsAssetPageReqVO AssetsAsset) {
        List<AssetsAssetDO> page = AssetsAssetService.getDaAssetListAll(AssetsAsset, "1");
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetRespVO.class));
    }

    /**
     * *     * @param bean PageResult<AssetsAssetRespVO>     * @return PageResult<AssetsAssetRespVO>
     */

    @Deprecated
    @SuppressWarnings("unchecked")
    public PageResult<AssetsAssetRespVO> fillAssetTags(PageResult<AssetsAssetRespVO> bean) {
        List<AssetsAssetRespVO> rows = (List<AssetsAssetRespVO>) bean.getRows();
        if (CollectionUtils.isEmpty(rows)) {
            return bean;
        }
        List<TaxonomyTagAssetRelRespDTO> apiList = attTagAssetRelApiService.getApiList(new TaxonomyTagAssetRelReqDTO());
        Map<Long, String> collect1 = attTagApiService.getApiList().stream().collect(Collectors.toMap(s -> s.getId(), s -> s.getName()));
        Map<String, List<TaxonomyTagAssetRelRespDTO>> collect = apiList.stream().collect(Collectors.groupingBy(s -> s.getAssetId()));
        for (AssetsAssetRespVO row : rows) {
            List<TaxonomyTagAssetRelRespDTO> attTagAssetRelRespDTOS = collect.get(Convert.toStr(row.getId()));
            row.setTagIds(new ArrayList<>());
            if (attTagAssetRelRespDTOS != null) {
                List<String> list = new ArrayList<>();
                List<String> listName = new ArrayList<>();
                for (TaxonomyTagAssetRelRespDTO attTagAssetRelRespDTO : attTagAssetRelRespDTOS) {
                    String name = collect1.get(Convert.toLong(attTagAssetRelRespDTO.getTagId()));
                    if (StringUtils.isNotEmpty(name)) {
                        listName.add(name);
                    }
                    list.add(attTagAssetRelRespDTO.getTagId());
                }
                row.setTagIds(list);
            }
        }
        bean.setRows(rows);
        return bean;
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('att:tag:query')")
    @GetMapping("/listAssetTag")
    public CommonResult<PageResult<AssetsAssetRespVO>> listAssetTag(AssetsAssetPageReqVO AssetsAsset) {
        if (CollectionUtils.isEmpty(AssetsAsset.getTagIdList())) {
            return CommonResult.success(null);
        }
        PageResult<AssetsAssetDO> page = AssetsAssetService.getDaAssetPage(AssetsAsset, "1");
        PageResult<AssetsAssetRespVO> bean = BeanUtils.toBean(page, AssetsAssetRespVO.class);
        return CommonResult.success(bean);
    }

    @Deprecated
    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:list')")
    @GetMapping("/dpp/list")
    public CommonResult<PageResult<AssetsAssetRespVO>> dppList(AssetsAssetPageReqVO AssetsAsset) {
        PageResult<AssetsAssetDO> page = AssetsAssetService.getDppAssetPage(AssetsAsset);
        return CommonResult.success(this.fillAssetTags(BeanUtils.toBean(page, AssetsAssetRespVO.class)));
    }

    /**
     * id     *     * @param ids     * @return
     */

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:list')")
    @GetMapping("/listByIds/{ids}")
    public CommonResult<PageResult<AssetsAssetRespVO>> list(@PathVariable("ids") List<Long> ids) {
        PageResult<AssetsAssetDO> page = AssetsAssetService.getDaAssetByIds(ids);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetRespVO.class));
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:list')")
    @GetMapping("/dpp/noPage/list")
    public AjaxResult dppNoPageList(AssetsAssetPageReqVO AssetsAsset) {
        List<AssetsAssetDO> AssetsAssetDOList = AssetsAssetService.getDppAssetNoPageList(AssetsAsset);
        return AjaxResult.success(AssetsAssetDOList);
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:list')")
    @GetMapping("/getTablesByDataSourceId")
    public AjaxResult getTablesByDataSourceId(AssetsAssetPageReqVO AssetsAsset) {
        List<AssetsAssetDO> tablesByDataSourceId = AssetsAssetService.getTablesByDataSourceId(AssetsAsset);
        return AjaxResult.success(tablesByDataSourceId);
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:list')")
    @GetMapping("/getDaAssetRespList")
    public CommonResult<List<AssetsAssetRespVO>> getDaAssetRespList(AssetsAssetPageReqVO AssetsAsset) {
        List<AssetsAssetDO> tablesByDataSourceId = AssetsAssetService.getDaAssetList(AssetsAsset);
        return CommonResult.success(BeanUtils.toBean(tablesByDataSourceId, AssetsAssetRespVO.class));
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:export')")
    @Log(title = "", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetDO> list = (List<AssetsAssetDO>) AssetsAssetService.getDaAssetPage(exportReqVO, "1").getRows();
        ExcelUtil<AssetsAssetRespVO> util = new ExcelUtil<>(AssetsAssetRespVO.class);
        util.exportExcel(response, AssetsAssetConvert.INSTANCE.convertToRespVOList(list), "");
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:import')")
    @Log(title = "", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetRespVO> util = new ExcelUtil<>(AssetsAssetRespVO.class);
        List<AssetsAssetRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetService.importDaAsset(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetRespVO> getInfo(@PathVariable("id") Long id) {
        return CommonResult.success(AssetsAssetService.getDaAssetById(id));
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:query')")
    @PostMapping(value = "/preview")
    public AjaxResult getPreview(@RequestBody JSONObject jsonObject) {
        if (StringUtils.isEmpty(jsonObject.getStr("id"))) {
            return error("id");
        }
        Map<String, Object> columnData = AssetsAssetService.getColumnData(jsonObject);
        if (columnData == null) {
            return error("!");
        }
        SysUser sysUser = SecurityUtils.getLoginUser().getUser();

// List<Map<String, Object>> dataMaskingList = AssetsAssetService.dataMasking(Long.valueOf(jsonObject.getStr("id")), (List<Map<String, Object>>) columnData.get("tableData"));

//1.数据资产  2.数据查询
        List<Map<String, Object>> dataMaskingList = AssetsAssetService.dataMaskings(Long.valueOf(jsonObject.getStr("id")), (List<Map<String, Object>>) columnData.get("tableData"), sysUser.getUserId(), "1");
        if (dataMaskingList == null) {
            return error("请检查资产字段与数据表字段是否一致");
        }
        columnData.put("tableData", dataMaskingList);
        return success(columnData);
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:edit')")
    @Log(title = "", businessType = BusinessType.INSERT)
    @PostMapping("/bindResources")
    public CommonResult<Long> bindResources(@Valid
                                            @RequestBody AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAsset.setUpdatorId(getUserId());
        AssetsAsset.setUpdateBy(getNickName());
        AssetsAsset.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetService.createDaAssetBindResources(AssetsAsset));
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:add')")
    @Log(title = "", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid
                                  @RequestBody AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAsset.setCreatorId(getUserId());
        AssetsAsset.setCreateBy(getNickName());
        AssetsAsset.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetService.createDaAssetNew(AssetsAsset));
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:add')")
// 也可以单独配 da:asset:batchAdd
    @Log(title = "", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public CommonResult<List<Long>> batchAdd(@Valid
                                             @RequestBody List<AssetsAssetSaveReqVO> AssetsAssetList) {
        Long userId = getUserId();
        String nickName = getNickName();
        Date now = DateUtil.date();
        for (AssetsAssetSaveReqVO AssetsAsset : AssetsAssetList) {
            AssetsAsset.setCreatorId(userId);
            AssetsAsset.setCreateBy(nickName);
            AssetsAsset.setCreateTime(now);
        }
        return CommonResult.success(AssetsAssetService.createDaAssetBatchNew(AssetsAssetList));
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:edit')")
    @Log(title = "", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid
                                      @RequestBody AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAsset.setUpdatorId(getUserId());
        AssetsAsset.setUpdateBy(getNickName());
        AssetsAsset.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetService.updateDaAssetNew(AssetsAsset));
    }

    @Operation(summary = "")
    @PreAuthorize("@ss.hasPermi('da:asset:remove')")
    @Log(title = "", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ID}")
    public CommonResult<Integer> remove(@PathVariable Long ID) {
        return CommonResult.toAjax(AssetsAssetService.removeDaAsset(ID));
    }

    @Log(title = "", businessType = BusinessType.UPDATE)
    @PutMapping("/startDaDiscoveryTask")
    public AjaxResult startDaDiscoveryTask(@Valid
                                           @RequestBody AssetsAssetSaveReqVO AssetsAsset) {
        return AssetsAssetService.startDaAssetDatasourceTask(AssetsAsset.getId());
    }

    @GetMapping("/listRelRule")
    public CommonResult<List<AssetsAssetColumnRelRuleVO>> listRelRule(@RequestParam Long id, @RequestParam String type) {
        List<AssetsAssetColumnRelRuleVO> vos = AssetsAssetService.listRelRule(id, type);
        return CommonResult.success(vos);
    }

    @GetMapping("/listRelRule/v2")
    public CommonResult<List<AssetsAssetColumnRelRuleVO>> listRelRule(@RequestParam Long datasourceId, @RequestParam String tableName, @RequestParam String type) {
        List<AssetsAssetColumnRelRuleVO> vos = AssetsAssetService.listRelRule(datasourceId, tableName, type);
        return CommonResult.success(vos);
    }

    @GetMapping("/dataLineage/{id}")
    public CommonResult<LineageDTO> dataLineage(@PathVariable Long id) {
        return CommonResult.success(AssetsAssetService.dataLineage(id));
    }

    @Operation(summary = "")
    @GetMapping("/getTreeData")
    public CommonResult<List<TreeData>> getTree() {
        return CommonResult.success(AssetsAssetService.getTreeData());
    }
}