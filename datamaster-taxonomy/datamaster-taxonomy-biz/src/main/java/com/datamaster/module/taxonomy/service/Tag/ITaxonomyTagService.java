

package com.datamaster.module.taxonomy.service.Tag;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagRespVO;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.Tag.TaxonomyTagDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 标签管理Service接口
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
public interface ITaxonomyTagService extends IService<TaxonomyTagDO> {

    /**
     * 获得标签管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 标签管理分页列表
     */
    PageResult<TaxonomyTagDO> getAttTagPage(TaxonomyTagPageReqVO pageReqVO);

    /**
     * 创建标签管理
     *
     * @param createReqVO 标签管理信息
     * @return 标签管理编号
     */
    Long createAttTag(TaxonomyTagSaveReqVO createReqVO);

    /**
     * 更新标签管理
     *
     * @param updateReqVO 标签管理信息
     */
    int updateAttTag(TaxonomyTagSaveReqVO updateReqVO);

    /**
     * 删除标签管理
     *
     * @param idList 标签管理编号
     */
    int removeAttTag(Collection<Long> idList);

    /**
     * 获得标签管理详情
     *
     * @param id 标签管理编号
     * @return 标签管理
     */
    TaxonomyTagRespVO getAttTagById(Long id);

    /**
     * 获得全部标签管理列表
     *
     * @return 标签管理列表
     */
    List<TaxonomyTagDO> getAttTagList();

    /**
     * 获得全部标签管理 Map
     *
     * @return 标签管理 Map
     */
    Map<Long, TaxonomyTagDO> getAttTagMap();


    /**
     * 导入标签管理数据
     *
     * @param importExcelList 标签管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAttTag(List<TaxonomyTagRespVO> importExcelList, boolean isUpdateSupport, String operName);

    Long getCountByCatCode(String code);


    /**
     * 将老的 CAT_CODE 批量更新成新的 CAT_CODE
     *
     * @param oldCatCode 旧分类编码
     * @param newCatCode 新分类编码
     * @return 受影响行数
     */
    int updateCatCode(String oldCatCode, String newCatCode);
}
