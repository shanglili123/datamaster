package com.datamaster.module.assets.service.asset;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.TreeData;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetPageReqVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetRespVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnRelRuleVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.neo4j.dto.LineageDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface IAssetsAssetService extends IService<AssetsAssetDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetDO> getDaAssetPage(AssetsAssetPageReqVO pageReqVO, String AssetsAssetQueryType);

    List<AssetsAssetDO> getDaAssetList(AssetsAssetPageReqVO AssetsAsset);

    List<AssetsAssetDO> getTablesByDataSourceId(AssetsAssetPageReqVO pageReqVO);

    AssetsAssetDO getDaAssetByDaAssetPageReqVO(AssetsAssetPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaAsset(AssetsAssetSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaAsset(AssetsAssetSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaAsset(Collection<Long> idList);

    int removeDaAsset(Long id);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetRespVO getDaAssetById(Long id);

    AssetsAssetRespVO getDaAssetByIdSimple(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetDO> getDaAssetList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetDO> getDaAssetMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAsset(List<AssetsAssetRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     *
     *
     * @param jsonObject id
     * @return
     */
    Map<String, Object> getColumnData(JSONObject jsonObject);

    /**
     *
     *
     * @param id   id
     * @param Data
     * @return
     */
    List<Map<String, Object>> dataMasking(Long id, List<Map<String, Object>> Data);

    void insertAssetByDiscoveryInfo(AssetsAssetPageReqVO AssetsAssetPageReqVO, List<AssetsAssetColumnSaveReqVO> columnSaveReqVOList);

    void updateAssetByDiscoveryInfo(AssetsAssetPageReqVO AssetsAssetPageReqVO);

    PageResult<AssetsAssetDO> getDppAssetPage(AssetsAssetPageReqVO AssetsAsset);

    List<AssetsAssetDO> getDppAssetNoPageList(AssetsAssetPageReqVO AssetsAsset);

    Long createDaAssetNew(AssetsAssetSaveReqVO AssetsAsset);

    /**
     *
     */
    Long createDaAssetBindResources(AssetsAssetSaveReqVO AssetsAsset);

    int updateDaAssetNew(AssetsAssetSaveReqVO AssetsAsset);

    AjaxResult startDaAssetDatasourceTask(Long id);

    void startDaAssetDatasourceTaskNull();

    PageResult<AssetsAssetDO> getDaAssetByIds(List<Long> ids);

    List<AssetsAssetColumnRelRuleVO> listRelRule(Long id, String type);

    List<AssetsAssetColumnRelRuleVO> listRelRule(Long datasourceId, String tableName, String type);

    /**
     * id
     *
     * @param id
     * @return
     */
    LineageDTO dataLineage(Long id);

    List<AssetsAssetDO> getDaAssetListAll(AssetsAssetPageReqVO AssetsAsset, String number);

    /**
     *
     *
     * @return
     */
    List<TreeData> getTreeData();

    /**
     *
     *
     * @param AssetsAssetList
     * @return
     */
    List<Long> createDaAssetBatchNew(List<AssetsAssetSaveReqVO> AssetsAssetList);

    List<Map<String, Object>> dataMaskings(Long id, List<Map<String, Object>> tableData, Long userId, String scene);

    List<AssetsAssetDO> getDaAssetByDataSourceId(Long DataSourceId, String tableName);
}
