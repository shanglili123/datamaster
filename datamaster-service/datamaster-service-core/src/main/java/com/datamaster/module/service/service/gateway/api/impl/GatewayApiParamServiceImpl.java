package com.datamaster.module.service.service.gateway.api.impl;

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
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamRespVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamSaveReqVO;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiParamDO;
import com.datamaster.module.service.dal.mapper.gateway.api.GatewayApiParamMapper;
import com.datamaster.module.service.service.gateway.api.IGatewayApiParamService;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据服务-API网关-参数 Service 实现
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GatewayApiParamServiceImpl extends ServiceImpl<GatewayApiParamMapper, GatewayApiParamDO>
        implements IGatewayApiParamService {
    @Resource
    private GatewayApiParamMapper GatewayApiParamMapper;

    @Override
    public PageResult<GatewayApiParamDO> getGatewayApiParamPage(GatewayApiParamPageReqVO pageReqVO) {
        return GatewayApiParamMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createGatewayApiParam(GatewayApiParamSaveReqVO createReqVO) {
        GatewayApiParamDO dictType = BeanUtils.toBean(createReqVO, GatewayApiParamDO.class);
        GatewayApiParamMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createGatewayApiParamDeep(List<GatewayApiParamSaveReqVO> paramList, Long gatewayApiId) {
        this.removeThemeRelByGatewayApiId(gatewayApiId);
        if (paramList == null || paramList.isEmpty()) {
            return;
        }
        paramList.forEach(param -> createRecursively(param, null, gatewayApiId));
    }

    /**
     * 递归创建参数树
     *
     * @param vo           参数VO
     * @param parentId     父级ID，根节点为null
     * @param gatewayApiId API网关ID
     */
    private void createRecursively(GatewayApiParamSaveReqVO vo, Long parentId, Long gatewayApiId) {
        vo.setParentId(parentId);
        vo.setApiId(gatewayApiId);
        vo.setId(null);
        // 插入当前节点，获取生成的主键
        Long newId = createGatewayApiParam(vo);
        // 处理子节点
        List<GatewayApiParamSaveReqVO> children = vo.getGatewayApiParamList();
        if (children != null && !children.isEmpty()) {
            children.forEach(child -> createRecursively(child, newId, gatewayApiId));
        }
    }

    @Override
    public int updateGatewayApiParam(GatewayApiParamSaveReqVO updateReqVO) {
        GatewayApiParamDO updateObj = BeanUtils.toBean(updateReqVO, GatewayApiParamDO.class);
        return GatewayApiParamMapper.updateById(updateObj);
    }

    @Override
    public int removeGatewayApiParam(Collection<Long> idList) {
        return GatewayApiParamMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeThemeRelByGatewayApiId(Long gatewayApiId) {
        GatewayApiParamMapper.removeThemeRelByGatewayApiId(gatewayApiId);
        return 0;
    }

    @Override
    public GatewayApiParamDO getGatewayApiParamById(Long id) {
        return GatewayApiParamMapper.selectById(id);
    }

    @Override
    public List<GatewayApiParamDO> getGatewayApiParamList() {
        return GatewayApiParamMapper.selectList();
    }

    @Override
    public List<GatewayApiParamRespVO> getGatewayApiParamList(Long id) {
        MPJLambdaWrapper<GatewayApiParamDO> lambdaWrapper = new MPJLambdaWrapper<>();
        lambdaWrapper.eq(id != null, GatewayApiParamDO::getApiId, id);
        List<GatewayApiParamDO> gatewayApiParamDOS = GatewayApiParamMapper.selectList(lambdaWrapper);
        List<GatewayApiParamRespVO> gatewayApiParamRespVOList = BeanUtils.toBean(gatewayApiParamDOS, GatewayApiParamRespVO.class);
        return buildParamTree(gatewayApiParamRespVOList);
    }

    /**
     * 构建参数树
     *
     * @param flatList 扁平RespVO列表
     * @return 树形RespVO列表
     */
    public List<GatewayApiParamRespVO> buildParamTree(List<GatewayApiParamRespVO> flatList) {
        if (flatList == null || flatList.isEmpty()) {
            return Collections.emptyList();
        }
        // 用 id->节点 的映射，加速查找
        Map<Long, GatewayApiParamRespVO> idMap = flatList.stream()
                .collect(Collectors.toMap(GatewayApiParamRespVO::getId, Function.identity()));

        List<GatewayApiParamRespVO> tree = new ArrayList<>();
        for (GatewayApiParamRespVO node : flatList) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0) {
                // 无父节点，视为根
                tree.add(node);
            } else {
                GatewayApiParamRespVO parent = idMap.get(parentId);
                if (parent != null) {
                    if (parent.getGatewayApiParamList() == null) {
                        parent.setGatewayApiParamList(new ArrayList<>());
                    }
                    parent.getGatewayApiParamList().add(node);
                } else {
                    // 找不到父节点，也当作根处理
                    tree.add(node);
                }
            }
        }
        return tree;
    }

    @Override
    public Map<Long, GatewayApiParamDO> getGatewayApiParamMap() {
        List<GatewayApiParamDO> gatewayApiParamList = GatewayApiParamMapper.selectList();
        return gatewayApiParamList.stream()
                .collect(Collectors.toMap(
                        GatewayApiParamDO::getId,
                        gatewayApiParamDO -> gatewayApiParamDO,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public String importGatewayApiParam(List<GatewayApiParamRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (GatewayApiParamRespVO respVO : importExcelList) {
            try {
                GatewayApiParamDO gatewayApiParamDO = BeanUtils.toBean(respVO, GatewayApiParamDO.class);
                Long gatewayApiParamId = respVO.getId();
                if (isUpdateSupport) {
                    if (gatewayApiParamId != null) {
                        GatewayApiParamDO existingGatewayApiParam = GatewayApiParamMapper.selectById(gatewayApiParamId);
                        if (existingGatewayApiParam != null) {
                            GatewayApiParamMapper.updateById(gatewayApiParamDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + gatewayApiParamId + " 的API网关参数记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + gatewayApiParamId + " 的API网关参数记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<GatewayApiParamDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", gatewayApiParamId);
                    GatewayApiParamDO existingGatewayApiParam = GatewayApiParamMapper.selectOne(queryWrapper);
                    if (existingGatewayApiParam == null) {
                        GatewayApiParamMapper.insert(gatewayApiParamDO);
                        successNum++;
                        successMessages.add("数据导入成功，ID为 " + gatewayApiParamId + " 的API网关参数记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据导入失败，ID为 " + gatewayApiParamId + " 的API网关参数记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("导入失败 ").append(failureNum).append(" 条");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("导入成功 ").append(successNum).append(" 条");
        }
        return resultMsg.toString();
    }
}
