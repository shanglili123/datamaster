package com.datamaster.module.standards.service.standard.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemCatPageReqVO;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemCatSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.standard.StandardsDataElemCatDO;
import com.datamaster.module.standards.dal.mapper.standard.StandardsDataElemCatMapper;
import com.datamaster.module.standards.dal.mapper.standard.StandardsDataMetaMapper;
import com.datamaster.module.standards.service.standard.IStandardsDataElemCatService;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 数据元类目管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class StandardsDataElemCatServiceImpl extends ServiceImpl<StandardsDataElemCatMapper, StandardsDataElemCatDO> implements IStandardsDataElemCatService {
    private final StandardsDataElemCatMapper StandardsDataElemCatMapper;
    private final StandardsDataMetaMapper dataElemMapper;

    @Override
    public Long createDgDataElemCat(StandardsDataElemCatSaveReqVO createReqVO) {
        StandardsDataElemCatDO dictType = BeanUtils.toBean(createReqVO, StandardsDataElemCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        StandardsDataElemCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDgDataElemCat(StandardsDataElemCatSaveReqVO updateReqVO) {
        StandardsDataElemCatDO StandardsDataElemCatDO = StandardsDataElemCatMapper.selectById(updateReqVO.getId());
        if (StandardsDataElemCatDO == null) {
            return 0;
        }
        //判断是否选择了他自己
        if (StandardsDataElemCatDO.getId().equals(updateReqVO.getParentId())) {
            throw new ServiceException("切换上级不能选择自身作为上级类目");
        }
        // 更新数据元类目管理
        StandardsDataElemCatDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDataElemCatDO.class);
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            StandardsDataElemCatMapper.updateValidFlag(StandardsDataElemCatDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            StandardsDataElemCatDO parent = StandardsDataElemCatMapper.selectById(StandardsDataElemCatDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }

        //修改上下级判断
        boolean flag = false;
        if (updateReqVO.getParentId() != null && !StandardsDataElemCatDO.getParentId().equals(updateReqVO.getParentId())) {
            updateReqVO.setCode(createCode(updateReqVO.getParentId(), null));
            flag = true;
        }

        int i = StandardsDataElemCatMapper.updateById(updateObj);

        //判断上下级是否发生了改变
        if (flag) {
            //更改所有下级
            changeCodeByPid(updateObj.getId(), updateObj.getCode());
        }

        return i;
    }

    @Override
    public void changeCodeByPid(Long pid, String parentCode) {
        List<StandardsDataElemCatDO> list = baseMapper.selectList(Wrappers.lambdaQuery(StandardsDataElemCatDO.class)
                .eq(StandardsDataElemCatDO::getParentId, pid)
                .orderByAsc(StandardsDataElemCatDO::getCreateTime));
        if (list != null && !list.isEmpty()) {
            list.forEach(e -> {
                String codeNew = createCode(e.getParentId(), parentCode);
                e.setCode(codeNew);
                baseMapper.updateById(e);
                this.changeCodeByPid(e.getId(), e.getCode());
            });
        }
    }

    @Override
    public int removeDgDataElemCat(Collection<Long> idList) {
        int count = 0;
        List<StandardsDataElemCatDO> list = baseMapper.selectBatchIds(idList);
        for (StandardsDataElemCatDO cat : list) {
            if (dataElemMapper.existsByCatCode(cat.getCode())) {
                throw new ServiceException("被标准数据元引用，不可删除");
            }
        }
        for (StandardsDataElemCatDO cat : list) {
            count += baseMapper.delete(Wrappers.lambdaQuery(StandardsDataElemCatDO.class)
                    .likeRight(StandardsDataElemCatDO::getCode, cat.getCode()));
        }
        return count;
    }

    @Override
    public StandardsDataElemCatDO getDgDataElemCatById(Long id) {
        return StandardsDataElemCatMapper.selectById(id);
    }

    @Override
    public List<StandardsDataElemCatDO> getDgDataElemCatList(StandardsDataElemCatPageReqVO reqVO) {
        return StandardsDataElemCatMapper.selectList(reqVO);
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
        LambdaQueryWrapper<StandardsDataElemCatDO> query = new LambdaQueryWrapper<StandardsDataElemCatDO>()
                .eq(StandardsDataElemCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), StandardsDataElemCatDO::getCode, parentCode)
                .isNotNull(StandardsDataElemCatDO::getCode)
                .orderByDesc(StandardsDataElemCatDO::getCode);
        List<StandardsDataElemCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                StandardsDataElemCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }

    @Override
    public BatchDeleteCheck<Long> batchDeleteCheck(List<Long> ids) {
        List<StandardsDataElemCatDO> list = baseMapper.selectBatchIds(ids);
        int cannotDeleteCount = 0;
        List<Long> canDeleteIds = new ArrayList<>();
        for (StandardsDataElemCatDO one : list) {
            if (one.getValidFlag()) {
                cannotDeleteCount++;
                continue;
            }
            boolean exists = dataElemMapper.existsByCatCode(one.getCode());
            if (exists) {
                cannotDeleteCount++;
            } else {
                canDeleteIds.add(one.getId());
            }
        }
        return new BatchDeleteCheck<>(cannotDeleteCount, canDeleteIds);
    }

}
