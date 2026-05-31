package com.datamaster.module.modeling.api.service.businessCategory;

import com.datamaster.common.core.domain.TreeData;

import java.util.List;

/**
 * <P>
 * 用途:业务分类ApiService接口
 * </p>
 *
 * @author: FXB
 * @create: 2026-04-26 11:01
 **/
public interface IModelingBusinessCategoryApiService {
    /**
     * 获取树形数据
     * @param type 统计类型 1：统计资产数量
     * @return
     */
    List<TreeData> getTreeData(String type);
}
