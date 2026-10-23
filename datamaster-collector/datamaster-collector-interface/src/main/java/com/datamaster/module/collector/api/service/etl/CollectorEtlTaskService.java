

package com.datamaster.module.collector.api.service.etl;

import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlNodeRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskNodeRelRespDTO;

import java.util.Map;
import java.util.List;

public interface CollectorEtlTaskService {

    /**
     * 查询数据源主键集合是否被引用
     * @param datasourceIdList 数据源id集合
     * @return 条数
     */
    int checkTaskIdInDatasource(List<Long> datasourceIdList,List<Long> spaceIdList);

    /**
     * 查询资产主键集合是否被引用
     * @param assetIdList 资产主键集合
     * @return 条数
     */
    int checkTaskIdInAsset(List<Long> assetIdList);

    /** 查询当前空间的数据开发任务目录。 */
    List<CollectorEtlTaskRespDTO> listVisibleTasks(Long spaceId, String spaceCode,
                                                   String name, String status, int limit);

    /** 查询当前空间的单个数据开发任务。 */
    CollectorEtlTaskRespDTO getVisibleTask(Long id, Long spaceId, String spaceCode);

    /** 查询当前空间任务的节点摘要来源数据；调用方仍需自行脱敏。 */
    List<CollectorEtlNodeRespDTO> listVisibleTaskNodes(Long taskId, Long spaceId, String spaceCode);

    /** 查询当前空间任务的节点依赖关系。 */
    List<CollectorEtlTaskNodeRelRespDTO> listVisibleTaskRelations(Long taskId, Long spaceId, String spaceCode);

    /** 将已校验的数据开发计划保存为当前空间任务草稿，不发布、不启动。 */
    Map<String, Object> applyVisibleTaskPlan(Long taskId, Long spaceId, String spaceCode, String planJson);

    /** 启动当前空间已发布的数据开发任务。 */
    Map<String, Object> startVisibleTask(Long id, Long spaceId, String spaceCode);
}
