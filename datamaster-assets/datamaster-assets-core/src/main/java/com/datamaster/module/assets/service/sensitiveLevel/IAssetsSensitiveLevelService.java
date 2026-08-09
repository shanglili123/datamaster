package com.datamaster.module.assets.service.sensitiveLevel;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelPageReqVO;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelRespVO;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.sensitiveLevel.AssetsSensitiveLevelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IAssetsSensitiveLevelService extends IService<AssetsSensitiveLevelDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsSensitiveLevelDO> getDaSensitiveLevelPage(AssetsSensitiveLevelPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaSensitiveLevel(AssetsSensitiveLevelSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaSensitiveLevel(AssetsSensitiveLevelSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaSensitiveLevel(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsSensitiveLevelDO getDaSensitiveLevelById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsSensitiveLevelDO> getDaSensitiveLevelList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsSensitiveLevelDO> getDaSensitiveLevelMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaSensitiveLevel(List<AssetsSensitiveLevelRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     *
     * @param id
     * @param status
     * @return
     */
    Boolean updateStatus(Long id, Long status);
}
