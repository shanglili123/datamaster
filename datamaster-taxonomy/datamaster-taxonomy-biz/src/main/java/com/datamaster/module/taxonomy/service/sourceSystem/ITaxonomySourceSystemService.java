

package com.datamaster.module.taxonomy.service.sourceSystem;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemRespVO;
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.sourceSystem.TaxonomySourceSystemDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 来源系统Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
public interface ITaxonomySourceSystemService extends IService<TaxonomySourceSystemDO> {

    /**
     * 获得来源系统分页列表
     *
     * @param pageReqVO 分页请求
     * @return 来源系统分页列表
     */
    PageResult<TaxonomySourceSystemDO> getAttSourceSystemPage(TaxonomySourceSystemPageReqVO pageReqVO);

    /**
     * 创建来源系统
     *
     * @param createReqVO 来源系统信息
     * @return 来源系统编号
     */
    Long createAttSourceSystem(TaxonomySourceSystemSaveReqVO createReqVO);

    /**
     * 更新来源系统
     *
     * @param updateReqVO 来源系统信息
     */
    int updateAttSourceSystem(TaxonomySourceSystemSaveReqVO updateReqVO);

    /**
     * 删除来源系统
     *
     * @param idList 来源系统编号
     */
    int removeAttSourceSystem(Collection<Long> idList);

    /**
     * 获得来源系统详情
     *
     * @param id 来源系统编号
     * @return 来源系统
     */
    TaxonomySourceSystemDO getAttSourceSystemById(Long id);

    /**
     * 获得全部来源系统列表
     *
     * @return 来源系统列表
     */
    List<TaxonomySourceSystemDO> getAttSourceSystemList();

    /**
     * 获得全部来源系统列表(带状态)
     *
     * @return 来源系统列表
     */
    public List<TaxonomySourceSystemDO> getAttSourceSystemListByValidFlag(Boolean validFlag);

    /**
     * 获得全部来源系统 Map
     *
     * @return 来源系统 Map
     */
    Map<Long, TaxonomySourceSystemDO> getAttSourceSystemMap();


    /**
     * 导入来源系统数据
     *
     * @param importExcelList 来源系统数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAttSourceSystem(List<TaxonomySourceSystemRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
