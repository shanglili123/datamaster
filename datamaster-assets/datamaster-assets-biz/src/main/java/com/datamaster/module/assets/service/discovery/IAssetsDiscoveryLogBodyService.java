package com.datamaster.module.assets.service.discovery;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryLogBodyDO;

/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-10-15
 */
public interface IAssetsDiscoveryLogBodyService extends IService<AssetsDiscoveryLogBodyDO> {

    /**  TM  */
    String getLatestLog(Long taskId);

    /**  */
    String getLog(Long taskId);

    /**  (taskId, tm)  */
    int saveOrUpdateByPk(AssetsDiscoveryLogBodyDO entity);
    void taskLogAppend(Long taskId, String logStr);

    /**
     *
     *
     * @param taskId ID
     * @return
     */
    boolean deleteByPk(Long taskId);
}
