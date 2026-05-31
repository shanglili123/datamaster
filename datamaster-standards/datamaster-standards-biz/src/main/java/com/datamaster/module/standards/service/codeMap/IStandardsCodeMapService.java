package com.datamaster.module.standards.service.codeMap;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapPageReqVO;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapRespVO;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.codeMap.StandardsCodeMapDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据元代码映射Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IStandardsCodeMapService extends IService<StandardsCodeMapDO> {

    /**
     * 获得数据元代码映射分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据元代码映射分页列表
     */
    PageResult<StandardsCodeMapDO> getDpCodeMapPage(StandardsCodeMapPageReqVO pageReqVO);

    /**
     * 创建数据元代码映射
     *
     * @param createReqVO 数据元代码映射信息
     * @return 数据元代码映射编号
     */
    Long createDpCodeMap(StandardsCodeMapSaveReqVO createReqVO);

    /**
     * 更新数据元代码映射
     *
     * @param updateReqVO 数据元代码映射信息
     */
    int updateDpCodeMap(StandardsCodeMapSaveReqVO updateReqVO);

    /**
     * 删除数据元代码映射
     *
     * @param idList 数据元代码映射编号
     */
    int removeDpCodeMap(Collection<Long> idList);

    /**
     * 获得数据元代码映射详情
     *
     * @param id 数据元代码映射编号
     * @return 数据元代码映射
     */
    StandardsCodeMapDO getDpCodeMapById(Long id);

    /**
     * 获得全部数据元代码映射列表
     *
     * @return 数据元代码映射列表
     */
    List<StandardsCodeMapDO> getDpCodeMapList();

    /**
     * 获得全部数据元代码映射 Map
     *
     * @return 数据元代码映射 Map
     */
    Map<Long, StandardsCodeMapDO> getDpCodeMapMap();


    /**
     * 导入数据元代码映射数据
     *
     * @param importExcelList 数据元代码映射数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDpCodeMap(List<StandardsCodeMapRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
