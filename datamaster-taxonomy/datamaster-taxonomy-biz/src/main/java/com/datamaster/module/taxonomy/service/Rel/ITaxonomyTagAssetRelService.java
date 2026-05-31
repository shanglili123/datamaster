

package com.datamaster.module.taxonomy.service.Rel;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.Rel.TaxonomyTagAssetRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 标签与资产关联关系Service接口
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
public interface ITaxonomyTagAssetRelService extends IService<TaxonomyTagAssetRelDO> {

    /**
     * 获得标签与资产关联关系分页列表
     *
     * @param pageReqVO 分页请求
     * @return 标签与资产关联关系分页列表
     */
    PageResult<TaxonomyTagAssetRelDO> getAttTagAssetRelPage(TaxonomyTagAssetRelPageReqVO pageReqVO);

    /**
     * 创建标签与资产关联关系
     *
     * @param createReqVO 标签与资产关联关系信息
     * @return 标签与资产关联关系编号
     */
    Long createAttTagAssetRel(TaxonomyTagAssetRelSaveReqVO createReqVO);

    /**
     * 更新标签与资产关联关系
     *
     * @param updateReqVO 标签与资产关联关系信息
     */
    int updateAttTagAssetRel(TaxonomyTagAssetRelSaveReqVO updateReqVO);

    /**
     * 删除标签与资产关联关系
     *
     * @param idList 标签与资产关联关系编号
     */
    int removeAttTagAssetRel(Collection<Long> idList);



    /**
     * 获得标签与资产关联关系详情
     *
     * @param id 标签与资产关联关系编号
     * @return 标签与资产关联关系
     */
    TaxonomyTagAssetRelDO getAttTagAssetRelById(Long id);

    /**
     * 获得全部标签与资产关联关系列表
     *
     * @return 标签与资产关联关系列表
     */
    List<TaxonomyTagAssetRelDO> getAttTagAssetRelList();

    /**
     * 获得全部标签与资产关联关系 Map
     *
     * @return 标签与资产关联关系 Map
     */
    Map<Long, TaxonomyTagAssetRelDO> getAttTagAssetRelMap();


    /**
     * 导入标签与资产关联关系数据
     *
     * @param importExcelList 标签与资产关联关系数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAttTagAssetRel(List<TaxonomyTagAssetRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

    int removeAttTagAssetRel(Long id, TaxonomyTagAssetRelPageReqVO TaxonomyTagAssetRel);
}
