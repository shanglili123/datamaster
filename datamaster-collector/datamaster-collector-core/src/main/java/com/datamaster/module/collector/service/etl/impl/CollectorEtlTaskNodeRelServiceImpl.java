

package com.datamaster.module.collector.service.etl.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskNodeRelMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskNodeRelService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 数据集成任务节点关系Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlTaskNodeRelServiceImpl  extends ServiceImpl<CollectorEtlTaskNodeRelMapper,CollectorEtlTaskNodeRelDO> implements ICollectorEtlTaskNodeRelService {
    @Resource
    private CollectorEtlTaskNodeRelMapper CollectorEtlTaskNodeRelMapper;

    @Override
    public PageResult<CollectorEtlTaskNodeRelDO> getCollectorEtlTaskNodeRelPage(CollectorEtlTaskNodeRelPageReqVO pageReqVO) {
        return CollectorEtlTaskNodeRelMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CollectorEtlTaskNodeRelRespVO> getCollectorEtlTaskNodeRelRespVOList(CollectorEtlTaskNodeRelPageReqVO reqVO) {
        MPJLambdaWrapper<CollectorEtlTaskNodeRelDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlTaskNodeRelDO.class)
                .eq(reqVO.getTaskId() != null, CollectorEtlTaskNodeRelDO::getTaskId, reqVO.getTaskId())
                .eq(reqVO.getTaskVersion() != null, CollectorEtlTaskNodeRelDO::getTaskVersion, reqVO.getTaskVersion())
                .eq(StringUtils.isNotBlank(reqVO.getTaskCode()), CollectorEtlTaskNodeRelDO::getTaskCode, reqVO.getTaskCode());
        List<CollectorEtlTaskNodeRelDO> CollectorEtlTaskNodeRelDOS = CollectorEtlTaskNodeRelMapper.selectList(wrapper);
        return BeanUtils.toBean(CollectorEtlTaskNodeRelDOS, CollectorEtlTaskNodeRelRespVO.class);
    }

    @Override
    public List<CollectorEtlTaskNodeRelRespVO> removeOldCollectorEtlTaskNodeRel(String code) {
        if (StringUtils.isBlank(code)) {
            return new ArrayList<>();
        }
        MPJLambdaWrapper<CollectorEtlTaskNodeRelDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlTaskNodeRelDO.class)
                .eq(CollectorEtlTaskNodeRelDO::getTaskCode, code);
        List<CollectorEtlTaskNodeRelDO> CollectorEtlTaskNodeRelDOS = CollectorEtlTaskNodeRelMapper.selectList(wrapper);
        if (CollectorEtlTaskNodeRelDOS.isEmpty()) {
            return new ArrayList<>();
        }
        this.removeCollectorEtlTaskNodeRel(getIdListFromTaskNodeRel(CollectorEtlTaskNodeRelDOS));
        return BeanUtils.toBean(CollectorEtlTaskNodeRelDOS, CollectorEtlTaskNodeRelRespVO.class);
    }
    /**
     * 从 List<CollectorEtlTaskNodeRelDO> 中提取 ID，并封装为 Collection<Long>
     *
     * @param CollectorEtlTaskNodeRelDOS List<CollectorEtlTaskNodeRelDO> 对象
     * @return Collection<Long> 返回ID列表
     */
    public static Collection<Long> getIdListFromTaskNodeRel(List<CollectorEtlTaskNodeRelDO> CollectorEtlTaskNodeRelDOS) {
        return CollectorEtlTaskNodeRelDOS.stream()
                .map(CollectorEtlTaskNodeRelDO::getId) // 提取 ID
                .collect(Collectors.toList());   // 收集成 List
    }

    @Override
    public Long createCollectorEtlTaskNodeRel(CollectorEtlTaskNodeRelSaveReqVO createReqVO) {
        CollectorEtlTaskNodeRelDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlTaskNodeRelDO.class);
        CollectorEtlTaskNodeRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public void createCollectorEtlTaskNodeRelBatch(List<CollectorEtlTaskNodeRelSaveReqVO> CollectorEtlTaskNodeRelSaveReqVOS) {
        if (CollectorEtlTaskNodeRelSaveReqVOS == null || CollectorEtlTaskNodeRelSaveReqVOS.isEmpty()) {
            return;
        }
        for (CollectorEtlTaskNodeRelSaveReqVO CollectorEtlTaskNodeRelSaveReqVO : CollectorEtlTaskNodeRelSaveReqVOS) {
            this.createCollectorEtlTaskNodeRel(CollectorEtlTaskNodeRelSaveReqVO);
        }
    }

    @Override
    public int updateCollectorEtlTaskNodeRel(CollectorEtlTaskNodeRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成任务节点关系
        CollectorEtlTaskNodeRelDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlTaskNodeRelDO.class);
        return CollectorEtlTaskNodeRelMapper.updateById(updateObj);
    }
    @Override
    public int removeCollectorEtlTaskNodeRel(Collection<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return 0;
        }
        // 批量删除数据集成任务节点关系
        return CollectorEtlTaskNodeRelMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlTaskNodeRelDO getCollectorEtlTaskNodeRelById(Long id) {
        return CollectorEtlTaskNodeRelMapper.selectById(id);
    }

    @Override
    public List<CollectorEtlTaskNodeRelDO> getCollectorEtlTaskNodeRelList() {
        return CollectorEtlTaskNodeRelMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlTaskNodeRelDO> getCollectorEtlTaskNodeRelMap() {
        List<CollectorEtlTaskNodeRelDO> CollectorEtlTaskNodeRelList = CollectorEtlTaskNodeRelMapper.selectList();
        return CollectorEtlTaskNodeRelList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlTaskNodeRelDO::getId,
                        CollectorEtlTaskNodeRelDO -> CollectorEtlTaskNodeRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据集成任务节点关系数据
         *
         * @param importExcelList 数据集成任务节点关系数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importCollectorEtlTaskNodeRel(List<CollectorEtlTaskNodeRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (CollectorEtlTaskNodeRelRespVO respVO : importExcelList) {
                try {
                    CollectorEtlTaskNodeRelDO CollectorEtlTaskNodeRelDO = BeanUtils.toBean(respVO, CollectorEtlTaskNodeRelDO.class);
                    Long CollectorEtlTaskNodeRelId = respVO.getId();
                    if (isUpdateSupport) {
                        if (CollectorEtlTaskNodeRelId != null) {
                            CollectorEtlTaskNodeRelDO existingCollectorEtlTaskNodeRel = CollectorEtlTaskNodeRelMapper.selectById(CollectorEtlTaskNodeRelId);
                            if (existingCollectorEtlTaskNodeRel != null) {
                                CollectorEtlTaskNodeRelMapper.updateById(CollectorEtlTaskNodeRelDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + CollectorEtlTaskNodeRelId + " 的数据集成任务节点关系记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + CollectorEtlTaskNodeRelId + " 的数据集成任务节点关系记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<CollectorEtlTaskNodeRelDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", CollectorEtlTaskNodeRelId);
                        CollectorEtlTaskNodeRelDO existingCollectorEtlTaskNodeRel = CollectorEtlTaskNodeRelMapper.selectOne(queryWrapper);
                        if (existingCollectorEtlTaskNodeRel == null) {
                            CollectorEtlTaskNodeRelMapper.insert(CollectorEtlTaskNodeRelDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + CollectorEtlTaskNodeRelId + " 的数据集成任务节点关系记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + CollectorEtlTaskNodeRelId + " 的数据集成任务节点关系记录已存在。");
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

}
