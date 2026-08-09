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
 * @author lili.shang
 * @date 2025-05-09
 */
public interface IAssetsAssetOperateLogService extends IService<AssetsAssetOperateLogDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetOperateLogDO> getAssetOperateLogPage(AssetsAssetOperateLogPageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createAssetOperateLog(AssetsAssetOperateLogSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateAssetOperateLog(AssetsAssetOperateLogSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeAssetOperateLog(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetOperateLogDO getAssetOperateLogById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetOperateLogDO> getAssetOperateLogList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetOperateLogDO> getAssetOperateLogMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetOperateLog(List<AssetsAssetOperateLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

    void rollBack(Long id);

    PageResult<AssetsAssetOperateLogDO> queryAssetOperateLogPage(AssetsAssetOperateLogPageReqVO AssetsAssetOperateLog);
}
