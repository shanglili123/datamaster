

package com.datamaster.module.standards.service.dataCategoryCat.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatPageReqVO;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatRespVO;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataCategoryCat.StandardsDataCategoryCatDO;
import com.datamaster.module.standards.dal.mapper.dataCategoryCat.StandardsDataCategoryCatMapper;
import com.datamaster.module.standards.service.dataCategory.IStandardsDataCategoryService;
import com.datamaster.module.standards.service.dataCategoryCat.IStandardsDataCategoryCatService;

/**
 * 数据分类-类目Service业务层处理
 *
 * @author FXB
 * @date 2026-04-07
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDataCategoryCatServiceImpl extends ServiceImpl<StandardsDataCategoryCatMapper, StandardsDataCategoryCatDO> implements IStandardsDataCategoryCatService {
    @Resource
    private StandardsDataCategoryCatMapper StandardsDataCategoryCatMapper;

    @Resource
    private IStandardsDataCategoryService StandardsDataCategoryService;

    @Override
    public PageResult<StandardsDataCategoryCatDO> getDgDataCategoryCatPage(StandardsDataCategoryCatPageReqVO pageReqVO) {
        return StandardsDataCategoryCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDgDataCategoryCat(StandardsDataCategoryCatSaveReqVO createReqVO) {
        StandardsDataCategoryCatDO dictType = BeanUtils.toBean(createReqVO, StandardsDataCategoryCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        StandardsDataCategoryCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDgDataCategoryCat(StandardsDataCategoryCatSaveReqVO updateReqVO) {
        StandardsDataCategoryCatDO catDO = baseMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        //判断是否选择了他自己
        if (catDO.getId().equals(updateReqVO.getParentId())) {
            throw new ServiceException("切换上级不能选择自身作为上级类目");
        }
        //修改上下级判断
        boolean flag = false;
        if (!catDO.getParentId().equals(updateReqVO.getParentId())) {
            updateReqVO.setCode(createCode(updateReqVO.getParentId(), null));
            flag = true;
        }

        // 更新数据服务类目管理
        StandardsDataCategoryCatDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDataCategoryCatDO.class);
        int i = StandardsDataCategoryCatMapper.updateById(updateObj);

        StandardsDataCategoryService.updateCatCode(catDO.getCode(), updateObj.getCode());
        //判断上下级是否发生了改变
        if (flag) {
            //更改所有下级
            changeCodeByPid(updateObj.getId(), updateObj.getCode());
        }
        return i;
    }

    @Override
    public int removeDgDataCategoryCat(Collection<Long> idList) {
        List<StandardsDataCategoryCatDO> attApiCatDOS = baseMapper.selectBatchIds(idList);
        for (StandardsDataCategoryCatDO catDO : attApiCatDOS) {
            Long countData = StandardsDataCategoryService.getCountByCatCode(catDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在分类，不允许删除");
            }
        }
        // 批量删除数据分类-类目
        return StandardsDataCategoryCatMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDataCategoryCatDO getDgDataCategoryCatById(Long id) {
        return StandardsDataCategoryCatMapper.selectById(id);
    }

    @Override
    public List<StandardsDataCategoryCatDO> getDgDataCategoryCatList() {
        return StandardsDataCategoryCatMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDataCategoryCatDO> getDgDataCategoryCatMap() {
        List<StandardsDataCategoryCatDO> StandardsDataCategoryCatList = StandardsDataCategoryCatMapper.selectList();
        return StandardsDataCategoryCatList.stream()
                .collect(Collectors.toMap(
                        StandardsDataCategoryCatDO::getId,
                        StandardsDataCategoryCatDO -> StandardsDataCategoryCatDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据分类-类目数据
     *
     * @param importExcelList 数据分类-类目数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importDgDataCategoryCat(List<StandardsDataCategoryCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (StandardsDataCategoryCatRespVO respVO : importExcelList) {
            try {
                StandardsDataCategoryCatDO StandardsDataCategoryCatDO = BeanUtils.toBean(respVO, StandardsDataCategoryCatDO.class);
                Long StandardsDataCategoryCatId = respVO.getId();
                if (isUpdateSupport) {
                    if (StandardsDataCategoryCatId != null) {
                        StandardsDataCategoryCatDO existingDgDataCategoryCat = StandardsDataCategoryCatMapper.selectById(StandardsDataCategoryCatId);
                        if (existingDgDataCategoryCat != null) {
                            StandardsDataCategoryCatMapper.updateById(StandardsDataCategoryCatDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + StandardsDataCategoryCatId + " 的数据分类-类目记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + StandardsDataCategoryCatId + " 的数据分类-类目记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<StandardsDataCategoryCatDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", StandardsDataCategoryCatId);
                    StandardsDataCategoryCatDO existingDgDataCategoryCat = StandardsDataCategoryCatMapper.selectOne(queryWrapper);
                    if (existingDgDataCategoryCat == null) {
                        StandardsDataCategoryCatMapper.insert(StandardsDataCategoryCatDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + StandardsDataCategoryCatId + " 的数据分类-类目记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + StandardsDataCategoryCatId + " 的数据分类-类目记录已存在。");
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
    public String createCode(Long parentId, String parentCode) {
        String categoryCode = null;
        /*
         * 分成三种情况
         * 1.数据库无数据 调用YouBianCodeUtil.getNextYouBianCode(null);
         * 2.添加子节点，无兄弟元素 YouBianCodeUtil.getSubYouBianCode(parentCode,null);
         * 3.添加子节点有兄弟元素 YouBianCodeUtil.getNextYouBianCode(lastCode);
         * */
        //找同类 确定上一个最大的code值
        LambdaQueryWrapper<StandardsDataCategoryCatDO> query = new LambdaQueryWrapper<StandardsDataCategoryCatDO>()
                .eq(StandardsDataCategoryCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), StandardsDataCategoryCatDO::getCode, parentCode)
                .isNotNull(StandardsDataCategoryCatDO::getCode)
                .orderByDesc(StandardsDataCategoryCatDO::getCode);
        List<StandardsDataCategoryCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                StandardsDataCategoryCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }

    @Override
    public void changeCodeByPid(Long pid, String parentCode) {
        List<StandardsDataCategoryCatDO> list = baseMapper.selectList(Wrappers.lambdaQuery(StandardsDataCategoryCatDO.class)
                .eq(StandardsDataCategoryCatDO::getParentId, pid)
                .orderByAsc(StandardsDataCategoryCatDO::getCreateTime));
        if (list != null && list.size() > 0) {
            list.forEach(e -> {
                String codeOld = e.getCode();
                String codeNew = createCode(e.getParentId(), parentCode);
                e.setCode(codeNew);
                baseMapper.updateById(e);
                StandardsDataCategoryService.updateCatCode(codeOld, codeNew);
                this.changeCodeByPid(e.getId(), e.getCode());
            });
        }
    }
}
