package com.datamaster.module.assets.service.assetchild.operate;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author qdata
 * @date 2025-05-09
 */
public interface IAssetsAssetOperateLogService extends IService<AssetsAssetOperateLogDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetOperateLogDO> getDaAssetOperateLogPage(AssetsAssetOperateLogPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaAssetOperateLog(AssetsAssetOperateLogSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaAssetOperateLog(AssetsAssetOperateLogSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaAssetOperateLog(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetOperateLogDO getDaAssetOperateLogById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetOperateLogDO> getDaAssetOperateLogList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetOperateLogDO> getDaAssetOperateLogMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetOperateLog(List<AssetsAssetOperateLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

    void rollBack(Long id);

    PageResult<AssetsAssetOperateLogDO> queryDaAssetOperateLogPage(AssetsAssetOperateLogPageReqVO AssetsAssetOperateLog);
}
