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
    PageResult<AssetsAssetDO> getAssetPage(AssetsAssetPageReqVO pageReqVO, String AssetsAssetQueryType);

    List<AssetsAssetDO> getAssetList(AssetsAssetPageReqVO AssetsAsset);

    List<AssetsAssetDO> getTablesByDataSourceId(AssetsAssetPageReqVO pageReqVO);

    AssetsAssetDO getAssetByAssetPageReqVO(AssetsAssetPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createAsset(AssetsAssetSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateAsset(AssetsAssetSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeAsset(Collection<Long> idList);

    int removeAsset(Long id);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetRespVO getAssetById(Long id);

    AssetsAssetRespVO getAssetByIdSimple(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetDO> getAssetList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetDO> getAssetMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAsset(List<AssetsAssetRespVO> importExcelList, boolean isUpdateSupport, String operName);

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

    PageResult<AssetsAssetDO> getCollectorAssetPage(AssetsAssetPageReqVO AssetsAsset);

    List<AssetsAssetDO> getCollectorAssetNoPageList(AssetsAssetPageReqVO AssetsAsset);

    Long createAssetNew(AssetsAssetSaveReqVO AssetsAsset);

    /**
     *
     */
    Long createAssetBindResources(AssetsAssetSaveReqVO AssetsAsset);

    int updateAssetNew(AssetsAssetSaveReqVO AssetsAsset);

    AjaxResult startAssetDatasourceTask(Long id);

    void startAssetDatasourceTaskNull();

    PageResult<AssetsAssetDO> getAssetByIds(List<Long> ids);

    List<AssetsAssetColumnRelRuleVO> listRelRule(Long id, String type);

    List<AssetsAssetColumnRelRuleVO> listRelRule(Long datasourceId, String tableName, String type);

    /**
     * id
     *
     * @param id
     * @return
     */
    LineageDTO dataLineage(Long id);

    List<AssetsAssetDO> getAssetListAll(AssetsAssetPageReqVO AssetsAsset, String number);

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
    List<Long> createAssetBatchNew(List<AssetsAssetSaveReqVO> AssetsAssetList);

    List<Map<String, Object>> dataMaskings(Long id, List<Map<String, Object>> tableData, Long userId, String scene);

    List<AssetsAssetDO> getAssetByDataSourceId(Long DataSourceId, String tableName);
}
