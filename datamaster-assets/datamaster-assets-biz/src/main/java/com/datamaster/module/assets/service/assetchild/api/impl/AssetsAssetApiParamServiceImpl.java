package com.datamaster.module.assets.service.assetchild.api.impl;

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
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.api.AssetsAssetApiParamDO;
import com.datamaster.module.assets.dal.mapper.assetchild.api.AssetsAssetApiParamMapper;
import com.datamaster.module.assets.service.assetchild.api.IAssetsAssetApiParamService;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * -API-Service
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetApiParamServiceImpl  extends ServiceImpl<AssetsAssetApiParamMapper,AssetsAssetApiParamDO> implements IAssetsAssetApiParamService {
    @Resource
    private AssetsAssetApiParamMapper AssetsAssetApiParamMapper;

    @Override
    public PageResult<AssetsAssetApiParamDO> getAssetApiParamPage(AssetsAssetApiParamPageReqVO pageReqVO) {
        return AssetsAssetApiParamMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAssetApiParam(AssetsAssetApiParamSaveReqVO createReqVO) {
        AssetsAssetApiParamDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetApiParamDO.class);
        AssetsAssetApiParamMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAssetApiParamDeep(List<AssetsAssetApiParamSaveReqVO> paramList, Long AssetsAssetApiId) {
        this.removeThemeRelByAssetApiId(AssetsAssetApiId);
        if (paramList == null || paramList.isEmpty()) {
            return;
        }
        paramList.forEach(param -> createRecursively(param, null,AssetsAssetApiId));
    }

    /**
     *
     *
     * @param vo        VO
     * @param parentId  ID null
     */
    private void createRecursively(AssetsAssetApiParamSaveReqVO vo, Long parentId, Long AssetsAssetApiId) {
        vo.setParentId(parentId);
        vo.setApiId(AssetsAssetApiId);
        vo.setId(null);
        // 插入当前节点，获取生成的主键
        Long newId = createAssetApiParam(vo);
        // 处理子节点
        List<AssetsAssetApiParamSaveReqVO> children = vo.getAssetsAssetApiParamList();
        if (children != null && !children.isEmpty()) {
            children.forEach(child -> createRecursively(child, newId,AssetsAssetApiId));
        }
    }

    @Override
    public int updateAssetApiParam(AssetsAssetApiParamSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产-外部API-参数
        AssetsAssetApiParamDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetApiParamDO.class);
        return AssetsAssetApiParamMapper.updateById(updateObj);
    }
    @Override
    public int removeAssetApiParam(Collection<Long> idList) {
        // 批量删除数据资产-外部API-参数
        return AssetsAssetApiParamMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeThemeRelByAssetApiId(Long assetApiId) {
        AssetsAssetApiParamMapper.removeThemeRelByAssetApiId(assetApiId);
        return 0;
    }

    @Override
    public AssetsAssetApiParamDO getAssetApiParamById(Long id) {
        return AssetsAssetApiParamMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetApiParamDO> getAssetApiParamList() {
        return AssetsAssetApiParamMapper.selectList();
    }
    @Override
    public List<AssetsAssetApiParamRespVO> getAssetApiParamList(Long id) {
        MPJLambdaWrapper<AssetsAssetApiParamDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.eq(id != null, AssetsAssetApiParamDO::getApiId, id);
        List<AssetsAssetApiParamDO> AssetsAssetApiParamDOS = AssetsAssetApiParamMapper.selectList(lambdaWrapper);
        List<AssetsAssetApiParamRespVO> AssetsAssetApiParamRespVOList = BeanUtils.toBean(AssetsAssetApiParamDOS, AssetsAssetApiParamRespVO.class);
        return buildParamTree(AssetsAssetApiParamRespVOList);
    }

    /**
     *
     *
     * @param flatList  RespVO
     * @return  RespVO
     */
    public List<AssetsAssetApiParamRespVO> buildParamTree(List<AssetsAssetApiParamRespVO> flatList) {
        if (flatList == null || flatList.isEmpty()) {
            return Collections.emptyList();
        }
        // 用 id->节点 的映射，加速查找
        Map<Long, AssetsAssetApiParamRespVO> idMap = flatList.stream()
                .collect(Collectors.toMap(AssetsAssetApiParamRespVO::getId, Function.identity()));

        List<AssetsAssetApiParamRespVO> tree = new ArrayList<>();
        for (AssetsAssetApiParamRespVO node : flatList) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0) {
                // 无父节点，视为根
                tree.add(node);
            } else {
                AssetsAssetApiParamRespVO parent = idMap.get(parentId);
                if (parent != null) {
                    if (parent.getAssetsAssetApiParamList() == null) {
                        parent.setAssetsAssetApiParamList(new ArrayList<>());
                    }
                    parent.getAssetsAssetApiParamList().add(node);
                } else {
                    // 找不到父节点，也当作根处理
                    tree.add(node);
                }
            }
        }
        return tree;
    }

    @Override
    public Map<Long, AssetsAssetApiParamDO> getAssetApiParamMap() {
        List<AssetsAssetApiParamDO> AssetsAssetApiParamList = AssetsAssetApiParamMapper.selectList();
        return AssetsAssetApiParamList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetApiParamDO::getId,
                        AssetsAssetApiParamDO -> AssetsAssetApiParamDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     * -API-
     *
     * @param importExcelList -API-
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    @Override
    public String importAssetApiParam(List<AssetsAssetApiParamRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsAssetApiParamRespVO respVO : importExcelList) {
            try {
                AssetsAssetApiParamDO AssetsAssetApiParamDO = BeanUtils.toBean(respVO, AssetsAssetApiParamDO.class);
                Long AssetsAssetApiParamId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetApiParamId != null) {
                        AssetsAssetApiParamDO existingAssetApiParam = AssetsAssetApiParamMapper.selectById(AssetsAssetApiParamId);
                        if (existingAssetApiParam != null) {
                            AssetsAssetApiParamMapper.updateById(AssetsAssetApiParamDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetApiParamId + " -API-");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetApiParamId + " -API-");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetApiParamDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetApiParamId);
                    AssetsAssetApiParamDO existingAssetApiParam = AssetsAssetApiParamMapper.selectOne(queryWrapper);
                    if (existingAssetApiParam == null) {
                        AssetsAssetApiParamMapper.insert(AssetsAssetApiParamDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetApiParamId + " -API-");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetApiParamId + " -API-");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append(" ").append(failureNum).append(" ");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append(" ").append(successNum).append(" ");
        }
        return resultMsg.toString();
    }
}
