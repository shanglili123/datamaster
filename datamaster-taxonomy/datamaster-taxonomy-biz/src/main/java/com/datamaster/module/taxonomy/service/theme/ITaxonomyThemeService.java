

package com.datamaster.module.taxonomy.service.theme;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemeRespVO;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemeSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.theme.TaxonomyThemeDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 主题Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface ITaxonomyThemeService extends IService<TaxonomyThemeDO> {

    /**
     * 获得主题分页列表
     *
     * @param pageReqVO 分页请求
     * @return 主题分页列表
     */
    PageResult<TaxonomyThemeDO> getAttThemePage(TaxonomyThemePageReqVO pageReqVO);

    /**
     * 创建主题
     *
     * @param createReqVO 主题信息
     * @return 主题编号
     */
    Long createAttTheme(TaxonomyThemeSaveReqVO createReqVO);

    /**
     * 更新主题
     *
     * @param updateReqVO 主题信息
     */
    int updateAttTheme(TaxonomyThemeSaveReqVO updateReqVO);

    /**
     * 删除主题
     *
     * @param idList 主题编号
     */
    int removeAttTheme(Collection<Long> idList);

    /**
     * 获得主题详情
     *
     * @param id 主题编号
     * @return 主题
     */
    TaxonomyThemeDO getAttThemeById(Long id);

    /**
     * 获得全部主题列表
     *
     * @return 主题列表
     */
    List<TaxonomyThemeDO> getAttThemeList();

    /**
     * 获得全部主题 Map
     *
     * @return 主题 Map
     */
    Map<Long, TaxonomyThemeDO> getAttThemeMap();


    /**
     * 导入主题数据
     *
     * @param importExcelList 主题数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAttTheme(List<TaxonomyThemeRespVO> importExcelList, boolean isUpdateSupport, String operName);

    List<TaxonomyThemeDO> getAttThemeListByReqVO(TaxonomyThemePageReqVO TaxonomyTheme);
}
