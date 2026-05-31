

package com.datamaster.module.taxonomy.service.client;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientRespVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.client.TaxonomyClientDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 应用管理Service接口
 *
 * @author DATAMASTER
 * @date 2025-02-18
 */
public interface ITaxonomyClientService extends IService<TaxonomyClientDO> {

    /**
     * 获得应用管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 应用管理分页列表
     */
    PageResult<TaxonomyClientDO> getAttClientPage(TaxonomyClientPageReqVO pageReqVO);

    /**
     * 创建应用管理
     *
     * @param createReqVO 应用管理信息
     * @return 应用管理编号
     */
    Long createAttClient(TaxonomyClientSaveReqVO createReqVO);

    /**
     * 更新应用管理
     *
     * @param updateReqVO 应用管理信息
     */
    int updateAttClient(TaxonomyClientSaveReqVO updateReqVO);

    /**
     * 删除应用管理
     *
     * @param idList 应用管理编号
     */
    int removeAttClient(Collection<Long> idList);

    /**
     * 获得应用管理详情
     *
     * @param id 应用管理编号
     * @return 应用管理
     */
    TaxonomyClientDO getAttClientById(Long id);

    /**
     * 获得全部应用管理列表
     *
     * @return 应用管理列表
     */
    List<TaxonomyClientDO> getAttClientList();

    /**
     * 获得全部应用管理 Map
     *
     * @return 应用管理 Map
     */
    Map<Long, TaxonomyClientDO> getAttClientMap();


    /**
     * 导入应用管理数据
     *
     * @param importExcelList 应用管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAttClient(List<TaxonomyClientRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
