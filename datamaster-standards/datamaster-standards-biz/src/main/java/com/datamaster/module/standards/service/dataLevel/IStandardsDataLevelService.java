

package com.datamaster.module.standards.service.dataLevel;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelRespVO;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelSaveReqVO;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelPageReqVO;
import com.datamaster.module.standards.dal.dataobject.dataLevel.StandardsDataLevelDO;
/**
 * 数据分级Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
public interface IStandardsDataLevelService extends IService<StandardsDataLevelDO> {

    /**
     * 获得数据分级分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据分级分页列表
     */
    PageResult<StandardsDataLevelDO> getDgDataLevelPage(StandardsDataLevelPageReqVO pageReqVO);

    /**
     * 创建数据分级
     *
     * @param createReqVO 数据分级信息
     * @return 数据分级编号
     */
    Long createDgDataLevel(StandardsDataLevelSaveReqVO createReqVO);

    /**
     * 更新数据分级
     *
     * @param updateReqVO 数据分级信息
     */
    int updateDgDataLevel(StandardsDataLevelSaveReqVO updateReqVO);

    /**
     * 删除数据分级
     *
     * @param idList 数据分级编号
     */
    int removeDgDataLevel(Collection<Long> idList);

    /**
     * 获得数据分级详情
     *
     * @param id 数据分级编号
     * @return 数据分级
     */
    StandardsDataLevelDO getDgDataLevelById(Long id);

    /**
     * 获得全部数据分级列表
     *
     * @return 数据分级列表
     */
    List<StandardsDataLevelDO> getDgDataLevelList();

    /**
     * 获得全部数据分级 Map
     *
     * @return 数据分级 Map
     */
    Map<Long, StandardsDataLevelDO> getDgDataLevelMap();


    /**
     * 导入数据分级数据
     *
     * @param importExcelList 数据分级数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDgDataLevel(List<StandardsDataLevelRespVO> importExcelList, boolean isUpdateSupport, String operName);

    List<StandardsDataLevelDO> getDgDataLevelListAll(StandardsDataLevelPageReqVO StandardsDataLevel);
}
