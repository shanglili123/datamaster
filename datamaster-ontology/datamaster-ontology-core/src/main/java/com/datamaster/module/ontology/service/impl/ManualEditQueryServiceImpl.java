package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.api.IManualEditQueryService;
import com.datamaster.module.ontology.api.dto.ManualEditDTO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.module.ontology.dal.mapper.ActionExecutionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 人为编辑登记查询实现 — 供数据接收工程（datamaster-ingestion）冲突仲裁使用
 *
 * <p>将本体动作执行记录（ONT_ACTION_EXECUTION）中已生效的人为编辑（EXECUTED / ROLLED_BACK）
 * 按目标物理表暴露给接收工程，实现「周期增量写数仓」与「人工编辑登记」两条写路径的冲突仲裁。</p>
 */
@Service
public class ManualEditQueryServiceImpl implements IManualEditQueryService {

    @Resource
    private ActionExecutionMapper executionMapper;

    @Override
    public List<ManualEditDTO> listLatestManualEdits(String targetTable, Integer limit) {
        if (targetTable == null || targetTable.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<ActionExecutionDO> records = executionMapper.selectLatestManualEditsByTable(targetTable.trim(), limit);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<ManualEditDTO> result = new ArrayList<>(records.size());
        for (ActionExecutionDO record : records) {
            ManualEditDTO dto = new ManualEditDTO();
            dto.setExecutionId(record.getId());
            dto.setActionId(record.getActionId());
            dto.setActionType(null); // 动作类型由调用方按需从执行记录关联动作解析，此处不引入动作表耦合
            dto.setStatus(record.getStatus());
            dto.setTargetTable(record.getTargetTable());
            dto.setBeforeData(record.getBeforeData());
            dto.setAfterData(record.getAfterData());
            dto.setRollbackBeforeData(record.getRollbackBeforeData());
            dto.setRollbackAfterData(record.getRollbackAfterData());
            dto.setExecuteTime(record.getExecuteTime());
            result.add(dto);
        }
        return result;
    }
}