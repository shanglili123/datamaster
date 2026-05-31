package com.datamaster.module.catalog.service.domain.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.catalog.controller.admin.domain.vo.CatalogDomainPageReqVO;
import com.datamaster.module.catalog.controller.admin.domain.vo.CatalogDomainSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.domain.CatalogDomainDO;
import com.datamaster.module.catalog.dal.mapper.domain.CatalogDomainMapper;
import com.datamaster.module.catalog.dal.mapper.metadata.CatalogDbMapper;
import com.datamaster.module.catalog.service.domain.ICatalogDomainService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务域Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-02-12
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogDomainServiceImpl  extends ServiceImpl<CatalogDomainMapper,CatalogDomainDO> implements ICatalogDomainService {
    @Resource
    private CatalogDomainMapper CatalogDomainMapper;
    @Resource
    private CatalogDbMapper dbMapper;

    @Override
    public PageResult<CatalogDomainDO> getCatalogDomainPage(CatalogDomainPageReqVO pageReqVO) {
        return CatalogDomainMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCatalogDomain(CatalogDomainSaveReqVO createReqVO) {
        CatalogDomainDO dictType = BeanUtils.toBean(createReqVO, CatalogDomainDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        CatalogDomainMapper.insert(dictType);
        return dictType.getId();
    }

    public String createCode(Long parentId, String parentCode) {
        String categoryCode = null;
        /*
         * 分成三种情况
         * 1.数据库无数据 调用YouBianCodeUtil.getNextYouBianCode(null);
         * 2.添加子节点，无兄弟元素 YouBianCodeUtil.getSubYouBianCode(parentCode,null);
         * 3.添加子节点有兄弟元素 YouBianCodeUtil.getNextYouBianCode(lastCode);
         * */
        //找同类 确定上一个最大的code值
        LambdaQueryWrapper<CatalogDomainDO> query = new LambdaQueryWrapper<CatalogDomainDO>()
                .eq(CatalogDomainDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), CatalogDomainDO::getCode, parentCode)
                .isNotNull(CatalogDomainDO::getCode)
                .orderByDesc(CatalogDomainDO::getCode);
        List<CatalogDomainDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                CatalogDomainDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }


    @Override
    public int updateCatalogDomain(CatalogDomainSaveReqVO updateReqVO) {
//        // 相关校验
//
//        // 更新业务域
//        CatalogDomainDO updateDomainDO = BeanUtils.toBean(updateReqVO, CatalogDomainDO.class);
//        return CatalogDomainMapper.updateById(updateDomainDO);
        CatalogDomainDO CatalogDomainDO = CatalogDomainMapper.selectById(updateReqVO.getId());
        if (CatalogDomainDO == null) {
            return 0;
        }
        //判断是否选择了他自己
        if (CatalogDomainDO.getId().equals(updateReqVO.getParentId())) {
            throw new ServiceException("切换上级不能选择自身作为上级类目");
        }
        // 更新业务域管理
        CatalogDomainDO updateObj = BeanUtils.toBean(updateReqVO, CatalogDomainDO.class);
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            CatalogDomainMapper.updateValidFlag(CatalogDomainDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            CatalogDomainDO parent = CatalogDomainMapper.selectById(CatalogDomainDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }
        //修改上下级判断
        boolean flag = false;
        if (updateReqVO.getParentId() != null && !CatalogDomainDO.getParentId().equals(updateReqVO.getParentId())) {
            updateReqVO.setCode(createCode(updateReqVO.getParentId(), null));
            flag = true;
        }

        int i = CatalogDomainMapper.updateById(updateObj);

        //判断上下级是否发生了改变
        if (flag) {
            //更改所有下级
            changeCodeByPid(updateObj.getId(), updateObj.getCode());
        }

        return i;

    }

    public void changeCodeByPid(Long pid, String parentCode) {
        List<CatalogDomainDO> list = baseMapper.selectList(Wrappers.lambdaQuery(CatalogDomainDO.class)
                .eq(CatalogDomainDO::getParentId, pid)
                .orderByAsc(CatalogDomainDO::getCreateTime));
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
    public int removeCatalogDomain(Collection<Long> idList) {
        // 批量删除业务域
       // return CatalogDomainMapper.deleteBatchIds(idList);
        int count = 0;
        List<CatalogDomainDO> list = baseMapper.selectBatchIds(idList);
        for (CatalogDomainDO one : list) {
//            if (CatalogTaskApiService.existsByDomainCode(one.getCode())) {
//                throw new ServiceException("被元数据采集引用，不可删除");
//            }
            if (dbMapper.existsBySourceSystemName(one.getCode())) {
                throw new ServiceException("被库元数据引用，不可删除");
            }
        }
        for (CatalogDomainDO one : list) {
            count += baseMapper.delete(Wrappers.lambdaQuery(CatalogDomainDO.class)
                    .likeRight(CatalogDomainDO::getCode, one.getCode()));
        }
        return count;
    }

    @Override
    public CatalogDomainDO getCatalogDomainById(Long id) {
        return CatalogDomainMapper.selectById(id);
    }

    @Override
    public List<CatalogDomainDO> getCatalogDomainList(CatalogDomainPageReqVO CatalogDomain) {
        return CatalogDomainMapper.selectList(CatalogDomain);
    }

    @Override
    public Map<Long, CatalogDomainDO> getCatalogDomainMap() {
        List<CatalogDomainDO> CatalogDomainList = CatalogDomainMapper.selectList();
        return CatalogDomainList.stream()
                .collect(Collectors.toMap(
                        CatalogDomainDO::getId,
                        CatalogDomainDO -> CatalogDomainDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public BatchDeleteCheck<Long> batchDeleteCheck(List<Long> ids) {
        List<CatalogDomainDO> list = baseMapper.selectBatchIds(ids);
        int cannotDeleteCount = 0;
        List<Long> canDeleteIds = new ArrayList<>();
        for (CatalogDomainDO one : list) {
            if (one.getValidFlag()) {
                cannotDeleteCount++;
                continue;
            }
            boolean exists = dbMapper.existsBySourceSystemName(one.getCode());
            if (exists) {
                cannotDeleteCount++;
            } else {
                canDeleteIds.add(one.getId());
            }
        }
        return new BatchDeleteCheck<>(cannotDeleteCount, canDeleteIds);
    }
}
