

package com.datamaster.module.taxonomy.service.cat;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyAssetCatDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据资产类目管理Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface ITaxonomyAssetCatService extends IService<TaxonomyAssetCatDO> {

    /**
     * 获得数据资产类目管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据资产类目管理分页列表
     */
    PageResult<TaxonomyAssetCatDO> getAttAssetCatPage(TaxonomyAssetCatPageReqVO pageReqVO);

    /**
     * 创建数据资产类目管理
     *
     * @param createReqVO 数据资产类目管理信息
     * @return 数据资产类目管理编号
     */
    Long createAttAssetCat(TaxonomyAssetCatSaveReqVO createReqVO);

    /**
     * 更新数据资产类目管理
     *
     * @param updateReqVO 数据资产类目管理信息
     */
    int updateAttAssetCat(TaxonomyAssetCatSaveReqVO updateReqVO);

    /**
     * 删除数据资产类目管理
     *
     * @param idList 数据资产类目管理编号
     */
    int removeAttAssetCat(Collection<Long> idList);


    /**
     * 获得数据资产类目管理详情
     *
     * @param id 数据资产类目管理编号
     * @return 数据资产类目管理
     */
    TaxonomyAssetCatDO getAttAssetCatById(Long id);

    /**
     * 获得全部数据资产类目管理列表
     *
     * @return 数据资产类目管理列表
     */
    List<TaxonomyAssetCatDO> getAttAssetCatList();

    /**
     * 获得全部数据资产类目管理列表
     *
     * @return 数据资产类目管理列表
     */
    List<TaxonomyAssetCatDO> getAttAssetCatList(TaxonomyAssetCatPageReqVO reqVO);

    /**
     * 获得全部数据资产类目管理 Map
     *
     * @return 数据资产类目管理 Map
     */
    Map<Long, TaxonomyAssetCatDO> getAttAssetCatMap();


    /**
     * 导入数据资产类目管理数据
     *
     * @param importExcelList 数据资产类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importAttAssetCat(List<TaxonomyAssetCatRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 生成code
     *
     * @param parentId
     * @param parentCode
     * @return
     */
    String createCode(Long parentId, String parentCode);
}
