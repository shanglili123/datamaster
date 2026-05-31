

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSqlTempDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据集成SQL模版Service接口
 *
 * @author FXB
 * @date 2025-06-25
 */
public interface ICollectorEtlSqlTempService extends IService<CollectorEtlSqlTempDO> {

    /**
     * 获得数据集成SQL模版分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成SQL模版分页列表
     */
    PageResult<CollectorEtlSqlTempDO> getCollectorEtlSqlTempPage(CollectorEtlSqlTempPageReqVO pageReqVO);

    /**
     * 创建数据集成SQL模版
     *
     * @param createReqVO 数据集成SQL模版信息
     * @return 数据集成SQL模版编号
     */
    Long createCollectorEtlSqlTemp(CollectorEtlSqlTempSaveReqVO createReqVO);

    /**
     * 更新数据集成SQL模版
     *
     * @param updateReqVO 数据集成SQL模版信息
     */
    int updateCollectorEtlSqlTemp(CollectorEtlSqlTempSaveReqVO updateReqVO);

    /**
     * 删除数据集成SQL模版
     *
     * @param idList 数据集成SQL模版编号
     */
    int removeCollectorEtlSqlTemp(Collection<Long> idList);

    /**
     * 获得数据集成SQL模版详情
     *
     * @param id 数据集成SQL模版编号
     * @return 数据集成SQL模版
     */
    CollectorEtlSqlTempDO getCollectorEtlSqlTempById(Long id);

    /**
     * 获得全部数据集成SQL模版列表
     *
     * @return 数据集成SQL模版列表
     */
    List<CollectorEtlSqlTempDO> getCollectorEtlSqlTempList();

    /**
     * 获得全部数据集成SQL模版 Map
     *
     * @return 数据集成SQL模版 Map
     */
    Map<Long, CollectorEtlSqlTempDO> getCollectorEtlSqlTempMap();


    /**
     * 导入数据集成SQL模版数据
     *
     * @param importExcelList 数据集成SQL模版数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importCollectorEtlSqlTemp(List<CollectorEtlSqlTempRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
