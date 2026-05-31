

package com.datamaster.module.standards.api.service.model;

import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemAssetRelReqDTO;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemAssetRelRespDTO;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemRespDTO;
import com.datamaster.module.standards.api.model.dto.StandardsModelColumnRespDTO;
import com.datamaster.module.standards.api.model.dto.StandardsModelRespDTO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 逻辑模型Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IStandardsModelApiService {

    /**
     * 根据逻辑模型ID获取逻辑模型信息
     *
     * @param id
     * @return
     */
    StandardsModelRespDTO getDpModelByIdApi(Long id);

    /**
     * 根据逻辑模型ID获取逻辑模型列信息
     *
     * @param modelId 逻辑模型ID
     * @return 逻辑模型列信息
     */
    List<StandardsModelColumnRespDTO> getDpModelColumnListByModelIdApi(Long modelId);

    /**
     * 根据数据元id查询数据元信息
     *
     * @param ids
     * @return
     */
    List<StandardsDataElemRespDTO> getDpDataElemListByIdsApi(Set<Long> ids);

    /**
     * 根据资产id获取数据元id集合
     *
     * @param assetId
     * @return
     */
    Set<Long> getDpDataElemListByAssetIdApi(Long assetId);

    List<StandardsDataElemAssetRelRespDTO> getDpDataElemListByColumnIdInApi(Collection<Long> columnIds);

    /**
     * 根据资产id及字段id获取数据元id集合
     *
     * @param assetId
     * @return
     */
    Set<Long> getDpDataElemListByAssetIdAndColumnId(Long assetId, Long columnId);

    /**
     * 插入数据元和资产关系数据
     *
     * @param StandardsDataElemAssetRel
     * @return
     */
    boolean insertElementAssetRelation(List<StandardsDataElemAssetRelReqDTO> StandardsDataElemAssetRel);

    /**
     * 根据类目编码查询数量
     *
     * @return
     */
    Long getCountByCatCode(String catCode);

    /**
     * 更新数据元和资产关系数据
     *
     * @param StandardsDataElemAssetRel
     * @return
     */
    boolean updateElementAssetRelation(StandardsDataElemAssetRelReqDTO StandardsDataElemAssetRel);

    /**
     * 根据资产id和代码表id查询数据元信息
     *
     * @param assetId
     * @param codeId
     * @return
     */
    List<StandardsDataElemRespDTO> getDpDataElemListByAssetId(Long assetId, Set<Long> codeId);

    /**
     * 更具模型id查询模型下的字段集合
     *
     * @param modelId 模型id
     */
    List<StandardsModelColumnRespDTO> getModelIdColumnList(Long modelId);

    /**
     * 将老的 CAT_CODE 批量更新成新的 CAT_CODE
     *
     * @param oldCatCode 旧分类编码
     * @param newCatCode 新分类编码
     * @return 受影响行数
     */
    int updateCatCode(String oldCatCode, String newCatCode);
}
