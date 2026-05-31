

package com.datamaster.module.taxonomy.service.cat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTagCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyTagCatMapper;
import com.datamaster.module.taxonomy.service.Tag.ITaxonomyTagService;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyTagCatService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标签类目管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyTagCatServiceImpl extends ServiceImpl<TaxonomyTagCatMapper,TaxonomyTagCatDO> implements ITaxonomyTagCatService {
    @Resource
    private TaxonomyTagCatMapper TaxonomyTagCatMapper;

    @Resource
    private ITaxonomyTagService TaxonomyTagService;

    @Override
    public PageResult<TaxonomyTagCatDO> getAttTagCatPage(TaxonomyTagCatPageReqVO pageReqVO) {
        return TaxonomyTagCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttTagCat(TaxonomyTagCatSaveReqVO createReqVO) {
        TaxonomyTagCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyTagCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyTagCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttTagCat(TaxonomyTagCatSaveReqVO updateReqVO) {
        TaxonomyTagCatDO catDO = TaxonomyTagCatMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        //判断是否选择了他自己
        if (catDO.getId().equals(updateReqVO.getParentId())){
            throw new ServiceException("切换上级不能选择自身作为上级类目");
        }
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            Long countData = TaxonomyTagService.getCountByCatCode(catDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在标签，不允许禁用");
            }
            TaxonomyTagCatMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            TaxonomyTagCatDO parent = TaxonomyTagCatMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }

        //修改上下级判断
        boolean flag = false;
        if (!catDO.getParentId().equals(updateReqVO.getParentId()) ) {
            updateReqVO.setCode(createCode(updateReqVO.getParentId(), null));
            flag = true;
        }

        // 更新标签类目管理
        TaxonomyTagCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyTagCatDO.class);
        int i = TaxonomyTagCatMapper.updateById(updateObj);

        TaxonomyTagService.updateCatCode(catDO.getCode(),updateObj.getCode());
        //判断上下级是否发生了改变
        if (flag) {
            //更改所有下级
            changeCodeByPid(updateObj.getId(), updateObj.getCode());
        }

        return i;
    }

    @Override
    public void changeCodeByPid(Long pid, String parentCode) {
        List<TaxonomyTagCatDO> list = baseMapper.selectList(Wrappers.lambdaQuery(TaxonomyTagCatDO.class)
                .eq(TaxonomyTagCatDO::getParentId, pid)
                .orderByAsc(TaxonomyTagCatDO::getCreateTime));
        if (list != null && list.size() > 0) {
            list.forEach(e -> {
                String codeOld = e.getCode();
                String codeNew = createCode(e.getParentId(), parentCode);
                e.setCode(codeNew);
                baseMapper.updateById(e);
                TaxonomyTagService.updateCatCode(codeOld,codeNew);
                this.changeCodeByPid(e.getId(), e.getCode());
            });
        }
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
        LambdaQueryWrapper<TaxonomyTagCatDO> query = new LambdaQueryWrapper<TaxonomyTagCatDO>()
                .eq(TaxonomyTagCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyTagCatDO::getCode, parentCode)
                .isNotNull(TaxonomyTagCatDO::getCode)
                .orderByDesc(TaxonomyTagCatDO::getCode);
        List<TaxonomyTagCatDO> list = TaxonomyTagCatMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyTagCatDO parent = TaxonomyTagCatMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }

    @Override
    public Integer removeAttTagCat(Long id) {
        int count = 0;
        TaxonomyTagCatDO cat = TaxonomyTagCatMapper.selectById(id);
        //判断是否存在数据
        if (TaxonomyTagService.getCountByCatCode(cat.getCode()) > 0) {
            throw new ServiceException("存在标签，不允许删除");
        }
        if (cat != null) {
            count += TaxonomyTagCatMapper.delete(Wrappers.lambdaQuery(TaxonomyTagCatDO.class)
                    .likeRight(TaxonomyTagCatDO::getCode, cat.getCode()));
        }
        return count;
    }

//    @Override
//    public int removeAttTagCat(Collection<Long> idList) {
//        // 批量删除标签类目管理
//        return TaxonomyTagCatMapper.deleteBatchIds(idList);
//    }

    @Override
    public TaxonomyTagCatDO getAttTagCatById(Long id) {
        return TaxonomyTagCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyTagCatDO> getAttTagCatList() {
        return TaxonomyTagCatMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyTagCatDO> getAttTagCatMap() {
        List<TaxonomyTagCatDO> TaxonomyTagCatList = TaxonomyTagCatMapper.selectList();
        return TaxonomyTagCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyTagCatDO::getId,
                        TaxonomyTagCatDO -> TaxonomyTagCatDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入标签类目管理数据
         *
         * @param importExcelList 标签类目管理数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importAttTagCat(List<TaxonomyTagCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (TaxonomyTagCatRespVO respVO : importExcelList) {
                try {
                    TaxonomyTagCatDO TaxonomyTagCatDO = BeanUtils.toBean(respVO, TaxonomyTagCatDO.class);
                    Long TaxonomyTagCatId = respVO.getId();
                    if (isUpdateSupport) {
                        if (TaxonomyTagCatId != null) {
                            TaxonomyTagCatDO existingAttTagCat = TaxonomyTagCatMapper.selectById(TaxonomyTagCatId);
                            if (existingAttTagCat != null) {
                                TaxonomyTagCatMapper.updateById(TaxonomyTagCatDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + TaxonomyTagCatId + " 的标签类目管理记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + TaxonomyTagCatId + " 的标签类目管理记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<TaxonomyTagCatDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", TaxonomyTagCatId);
                        TaxonomyTagCatDO existingAttTagCat = TaxonomyTagCatMapper.selectOne(queryWrapper);
                        if (existingAttTagCat == null) {
                            TaxonomyTagCatMapper.insert(TaxonomyTagCatDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + TaxonomyTagCatId + " 的标签类目管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + TaxonomyTagCatId + " 的标签类目管理记录已存在。");
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
    public List<TaxonomyTagCatDO> getAttTagCatLIst(TaxonomyTagCatPageReqVO reqVO) {
        LambdaQueryWrapperX<TaxonomyTagCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyTagCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyTagCatDO::getParentId, reqVO.getParentId())
                .eq(reqVO.getValidFlag() != null, TaxonomyTagCatDO::getValidFlag, Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .eqIfPresent(TaxonomyTagCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyTagCatDO::getDescription, reqVO.getDescription())
                .likeRightIfPresent(TaxonomyTagCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyTagCatDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(TaxonomyTagCatDO::getSortOrder);
        return TaxonomyTagCatMapper.selectList(queryWrapperX);
    }
}
