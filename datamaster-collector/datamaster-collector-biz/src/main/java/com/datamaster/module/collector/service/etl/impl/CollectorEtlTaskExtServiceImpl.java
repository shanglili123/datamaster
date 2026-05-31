

package com.datamaster.module.collector.service.etl.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskExtPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskExtRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskExtSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskExtMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据集成任务-扩展数据Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-04-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlTaskExtServiceImpl extends ServiceImpl<CollectorEtlTaskExtMapper, CollectorEtlTaskExtDO> implements ICollectorEtlTaskExtService {
    @Resource
    private CollectorEtlTaskExtMapper CollectorEtlTaskExtMapper;

    @Override
    public PageResult<CollectorEtlTaskExtDO> getCollectorEtlTaskExtPage(CollectorEtlTaskExtPageReqVO pageReqVO) {
        return CollectorEtlTaskExtMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCollectorEtlTaskExt(CollectorEtlTaskExtSaveReqVO createReqVO) {
        CollectorEtlTaskExtDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlTaskExtDO.class);
        CollectorEtlTaskExtMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorEtlTaskExt(CollectorEtlTaskExtSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成任务-扩展数据
        CollectorEtlTaskExtDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlTaskExtDO.class);
        return CollectorEtlTaskExtMapper.updateById(updateObj);
    }

    @Override
    public int removeCollectorEtlTaskExt(Collection<Long> idList) {
        // 批量删除数据集成任务-扩展数据
        return CollectorEtlTaskExtMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlTaskExtDO getCollectorEtlTaskExtById(Long id) {
        return CollectorEtlTaskExtMapper.selectById(id);
    }

    @Override
    public List<CollectorEtlTaskExtDO> getCollectorEtlTaskExtList() {
        return CollectorEtlTaskExtMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlTaskExtDO> getCollectorEtlTaskExtMap() {
        List<CollectorEtlTaskExtDO> CollectorEtlTaskExtList = CollectorEtlTaskExtMapper.selectList();
        return CollectorEtlTaskExtList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlTaskExtDO::getId,
                        CollectorEtlTaskExtDO -> CollectorEtlTaskExtDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据集成任务-扩展数据数据
     *
     * @param importExcelList 数据集成任务-扩展数据数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCollectorEtlTaskExt(List<CollectorEtlTaskExtRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CollectorEtlTaskExtRespVO respVO : importExcelList) {
            try {
                CollectorEtlTaskExtDO CollectorEtlTaskExtDO = BeanUtils.toBean(respVO, CollectorEtlTaskExtDO.class);
                Long CollectorEtlTaskExtId = respVO.getId();
                if (isUpdateSupport) {
                    if (CollectorEtlTaskExtId != null) {
                        CollectorEtlTaskExtDO existingCollectorEtlTaskExt = CollectorEtlTaskExtMapper.selectById(CollectorEtlTaskExtId);
                        if (existingCollectorEtlTaskExt != null) {
                            CollectorEtlTaskExtMapper.updateById(CollectorEtlTaskExtDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CollectorEtlTaskExtId + " 的数据集成任务-扩展数据记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CollectorEtlTaskExtId + " 的数据集成任务-扩展数据记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CollectorEtlTaskExtDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CollectorEtlTaskExtId);
                    CollectorEtlTaskExtDO existingCollectorEtlTaskExt = CollectorEtlTaskExtMapper.selectOne(queryWrapper);
                    if (existingCollectorEtlTaskExt == null) {
                        CollectorEtlTaskExtMapper.insert(CollectorEtlTaskExtDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CollectorEtlTaskExtId + " 的数据集成任务-扩展数据记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CollectorEtlTaskExtId + " 的数据集成任务-扩展数据记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
        }
        return resultMsg.toString();
    }

    @Override
    public CollectorEtlTaskExtDO getByTaskId(Long taskId) {
        return this.getOne(Wrappers.lambdaQuery(CollectorEtlTaskExtDO.class)
                .eq(CollectorEtlTaskExtDO::getTaskId, taskId));
    }
}
