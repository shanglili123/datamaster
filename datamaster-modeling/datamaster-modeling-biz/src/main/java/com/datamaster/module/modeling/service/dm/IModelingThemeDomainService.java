

package com.datamaster.module.modeling.service.dm;

import java.util.List;
import java.util.Map;
import java.util.Collection;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainSaveReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainPageReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingThemeDomainDO;

/**
 * 主题域管理Service接口
 *
 * @author FXB
 * @date 2026-03-24
 */
public interface IModelingThemeDomainService extends IService<ModelingThemeDomainDO> {

    /**
     * 获得主题域管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 主题域管理分页列表
     */
    PageResult<ModelingThemeDomainDO> getModelingThemeDomainPage(ModelingThemeDomainPageReqVO pageReqVO);

    /**
     * 创建主题域管理
     *
     * @param createReqVO 主题域管理信息
     * @return 主题域管理编号
     */
    Long createModelingThemeDomain(ModelingThemeDomainSaveReqVO createReqVO);

    /**
     * 更新主题域管理
     *
     * @param updateReqVO 主题域管理信息
     */
    int updateModelingThemeDomain(ModelingThemeDomainSaveReqVO updateReqVO);

    /**
     * 删除主题域管理
     *
     * @param idList 主题域管理编号
     */
    int removeModelingThemeDomain(Collection<Long> idList);

    /**
     * 获得主题域管理详情
     *
     * @param id 主题域管理编号
     * @return 主题域管理
     */
    ModelingThemeDomainDO getModelingThemeDomainById(Long id);

    /**
     * 获得全部主题域管理列表
     *
     * @return 主题域管理列表
     */
    List<ModelingThemeDomainDO> getModelingThemeDomainList();

    /**
     * 获得全部主题域管理 Map
     *
     * @return 主题域管理 Map
     */
    Map<Long, ModelingThemeDomainDO> getModelingThemeDomainMap();


    /**
     * 导入主题域管理数据
     *
     * @param importExcelList 主题域管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importModelingThemeDomain(List<ModelingThemeDomainRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 获取主题域管理列表
     *
     * @param reqVO
     * @return
     */
    List<ModelingThemeDomainDO> getModelingThemeDomainList(ModelingThemeDomainPageReqVO reqVO);

    /**
     * 生成code
     *
     * @param parentId
     * @param parentCode
     * @return
     */
    String createCode(Long parentId, String parentCode);

    /**
     * 更改指定pid下的所有code
     *
     * @param pid
     */
    void changeCodeByPid(Long pid, String parentCode);
}
