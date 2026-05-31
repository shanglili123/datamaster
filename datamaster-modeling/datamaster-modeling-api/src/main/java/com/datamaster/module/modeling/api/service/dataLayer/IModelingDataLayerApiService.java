package com.datamaster.module.modeling.api.service.dataLayer;

import com.datamaster.common.core.domain.TreeData;

import java.util.List;

/**
 * <P>
 * 用途:数仓分层ApiService接口
 * </p>
 *
 * @author: FXB
 * @create: 2026-04-27 16:53
 **/
public interface IModelingDataLayerApiService {
    /**
     * 获取树形数据
     * @param type 统计类型 1：统计资产数量
     * @return
     */
    List<TreeData> getTreeData(String type);
}
