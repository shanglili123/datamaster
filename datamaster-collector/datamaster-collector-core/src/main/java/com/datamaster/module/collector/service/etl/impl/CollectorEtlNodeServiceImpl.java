

package com.datamaster.module.collector.service.etl.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlNodeRespDTO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlNodeMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据集成节点Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlNodeServiceImpl extends ServiceImpl<CollectorEtlNodeMapper, CollectorEtlNodeDO> implements ICollectorEtlNodeService {
    @Resource
    private CollectorEtlNodeMapper CollectorEtlNodeMapper;

    @Override
    public PageResult<CollectorEtlNodeDO> getCollectorEtlNodePage(CollectorEtlNodePageReqVO pageReqVO) {
        return CollectorEtlNodeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CollectorEtlNodeRespVO> getCollectorEtlNodeRespList(CollectorEtlNodePageReqVO reqVO) {

        MPJLambdaWrapper<CollectorEtlNodeDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlNodeDO.class)
                .in(CollectionUtils.isNotEmpty(reqVO.getCodeList()), CollectorEtlNodeDO::getCode, reqVO.getCodeList());
        List<CollectorEtlNodeDO> CollectorEtlTaskNodeRelDOS = CollectorEtlNodeMapper.selectList(wrapper);
        return BeanUtils.toBean(CollectorEtlTaskNodeRelDOS, CollectorEtlNodeRespVO.class);
    }

    @Override
    public List<CollectorEtlNodeRespVO> listNodeByTaskId(Long taskId) {
        MPJLambdaWrapper<CollectorEtlNodeDO> wrapper = new MPJLambdaWrapper();
        wrapper.selectAll(CollectorEtlNodeDO.class)
                .innerJoin(CollectorEtlTaskNodeRelDO.class, CollectorEtlTaskNodeRelDO::getPostNodeCode, CollectorEtlNodeDO::getCode)
                .eq(CollectorEtlTaskNodeRelDO::getTaskId, taskId);
        List<CollectorEtlNodeDO> CollectorEtlTaskNodeRelDOS = CollectorEtlNodeMapper.selectList(wrapper);
        return BeanUtils.toBean(CollectorEtlTaskNodeRelDOS, CollectorEtlNodeRespVO.class);
    }

    @Override
    public CollectorEtlNodeRespVO getCollectorEtlNodeRespVOByReqVO(CollectorEtlNodePageReqVO reqVO) {
        MPJLambdaWrapper<CollectorEtlNodeDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlNodeDO.class)
                .eq(StringUtils.isNotEmpty(reqVO.getCode()), CollectorEtlNodeDO::getCode, reqVO.getCode())
                .eq(reqVO.getVersion() != null, CollectorEtlNodeDO::getVersion, reqVO.getVersion());
//        CollectorEtlNodeDO CollectorEtlNodeLogDO = CollectorEtlNodeMapper.selectOne(wrapper);
        List<CollectorEtlNodeDO> CollectorEtlTaskNodeRelDOS = CollectorEtlNodeMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(CollectorEtlTaskNodeRelDOS)) {
            return BeanUtils.toBean(CollectorEtlTaskNodeRelDOS.get(0), CollectorEtlNodeRespVO.class);
        }
        return BeanUtils.toBean(new CollectorEtlNodeDO(), CollectorEtlNodeRespVO.class);
    }

    @Override
    public Long createCollectorEtlNode(CollectorEtlNodeSaveReqVO createReqVO) {
        CollectorEtlNodeDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlNodeDO.class);
        CollectorEtlNodeMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public List<CollectorEtlNodeDO> createCollectorEtlNodeBatch(List<CollectorEtlNodeSaveReqVO> CollectorEtlNodeSaveReqVOList) {
        List<CollectorEtlNodeDO> CollectorEtlNodeDOList = new ArrayList<>();
        for (CollectorEtlNodeSaveReqVO CollectorEtlNodeSaveReqVO : CollectorEtlNodeSaveReqVOList) {
            CollectorEtlNodeDO dictType = BeanUtils.toBean(CollectorEtlNodeSaveReqVO, CollectorEtlNodeDO.class);
            CollectorEtlNodeMapper.insert(dictType);
            CollectorEtlNodeDOList.add(dictType);
        }
        return CollectorEtlNodeDOList;
    }

    @Override
    public int updateCollectorEtlNode(CollectorEtlNodeSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成节点
        CollectorEtlNodeDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlNodeDO.class);
        return CollectorEtlNodeMapper.updateById(updateObj);
    }

    @Override
    public int removeCollectorEtlNode(Collection<Long> idList) {
        // 批量删除数据集成节点
        return CollectorEtlNodeMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlNodeDO getCollectorEtlNodeById(Long id) {
        return CollectorEtlNodeMapper.selectById(id);
    }

    @Override
    public List<CollectorEtlNodeDO> getCollectorEtlNodeList() {
        return CollectorEtlNodeMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlNodeDO> getCollectorEtlNodeMap() {
        List<CollectorEtlNodeDO> CollectorEtlNodeList = CollectorEtlNodeMapper.selectList();
        return CollectorEtlNodeList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlNodeDO::getId,
                        CollectorEtlNodeDO -> CollectorEtlNodeDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据集成节点数据
     *
     * @param importExcelList 数据集成节点数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCollectorEtlNode(List<CollectorEtlNodeRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CollectorEtlNodeRespVO respVO : importExcelList) {
            try {
                CollectorEtlNodeDO CollectorEtlNodeDO = BeanUtils.toBean(respVO, CollectorEtlNodeDO.class);
                Long CollectorEtlNodeId = respVO.getId();
                if (isUpdateSupport) {
                    if (CollectorEtlNodeId != null) {
                        CollectorEtlNodeDO existingCollectorEtlNode = CollectorEtlNodeMapper.selectById(CollectorEtlNodeId);
                        if (existingCollectorEtlNode != null) {
                            CollectorEtlNodeMapper.updateById(CollectorEtlNodeDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CollectorEtlNodeId + " 的数据集成节点记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CollectorEtlNodeId + " 的数据集成节点记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CollectorEtlNodeDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CollectorEtlNodeId);
                    CollectorEtlNodeDO existingCollectorEtlNode = CollectorEtlNodeMapper.selectOne(queryWrapper);
                    if (existingCollectorEtlNode == null) {
                        CollectorEtlNodeMapper.insert(CollectorEtlNodeDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CollectorEtlNodeId + " 的数据集成节点记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CollectorEtlNodeId + " 的数据集成节点记录已存在。");
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
    public void removeOldCollectorEtlNode(List<String> code) {
        CollectorEtlNodeMapper.removeOldCollectorEtlNode(code);
    }

    @Override
    public Long getNodeIdByNodeCode(String nodeCode) {
        CollectorEtlNodeDO collectorEtlNodeDO = baseMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlNodeDO.class)
                .eq(CollectorEtlNodeDO::getCode, nodeCode));
        if (collectorEtlNodeDO != null) {
            return collectorEtlNodeDO.getId();
        }
        return null;
    }

    @Override
    public CollectorEtlNodeRespDTO getNodeByNodeCode(String nodeCode) {
        CollectorEtlNodeDO collectorEtlNodeDO = baseMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlNodeDO.class)
                .eq(CollectorEtlNodeDO::getCode, nodeCode));
        if (collectorEtlNodeDO != null) {
            return BeanUtils.toBean(collectorEtlNodeDO, CollectorEtlNodeRespDTO.class);
        }
        return null;
    }

}
