package com.datamaster.module.standards.service.dataElem.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.standards.api.service.dataElem.IDataElemApiService;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemDO;
import com.datamaster.module.standards.dal.dataobject.document.StandardsDocumentDO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelDO;
import com.datamaster.module.standards.dal.mapper.dataElem.StandardsDataElemMapper;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemService;
import com.datamaster.module.standards.service.document.IStandardsDocumentService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 数据元Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDataElemServiceImpl extends ServiceImpl<StandardsDataElemMapper, StandardsDataElemDO> implements IStandardsDataElemService, IDataElemApiService {
    @Resource
    private StandardsDataElemMapper StandardsDataElemMapper;

    @Resource
    private IStandardsDocumentService StandardsDocumentService;

    @Override
    public PageResult<StandardsDataElemDO> getDpDataElemPage(StandardsDataElemPageReqVO pageReqVO) {
        return StandardsDataElemMapper.selectPage(pageReqVO);
    }

    @Override
    public List<StandardsDataElemDO> getDpDataElemList(StandardsDataElemPageReqVO reqVO) {
        LambdaQueryWrapperX<StandardsDataElemDO> queryWrapper = new LambdaQueryWrapperX<>();
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        queryWrapper.likeIfPresent(StandardsDataElemDO::getName, reqVO.getName())
                .likeIfPresent(StandardsDataElemDO::getEngName, reqVO.getEngName())
                .eqIfPresent(StandardsDataElemDO::getCatCode, reqVO.getCatCode())
                .eqIfPresent(StandardsDataElemDO::getType, reqVO.getType())
                .eqIfPresent(StandardsDataElemDO::getPersonCharge, reqVO.getPersonCharge())
                .eqIfPresent(StandardsDataElemDO::getContactNumber, reqVO.getContactNumber())
                .eqIfPresent(StandardsDataElemDO::getColumnType, reqVO.getColumnType())
                .eqIfPresent(StandardsDataElemDO::getStatus, reqVO.getStatus())
                .eqIfPresent(StandardsDataElemDO::getDocumentId, reqVO.getDocumentId())
                .eqIfPresent(StandardsDataElemDO::getDescription, reqVO.getDescription())
                .eqIfPresent(StandardsDataElemDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(StandardsDataElemDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns);

        return StandardsDataElemMapper.selectList(queryWrapper);
    }

    @Override
    public Long createDpDataElem(StandardsDataElemSaveReqVO createReqVO) {
        StandardsDataElemDO dictType = BeanUtils.toBean(createReqVO, StandardsDataElemDO.class);
        StandardsDataElemMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpDataElem(StandardsDataElemSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据元
        StandardsDataElemDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDataElemDO.class);
        return StandardsDataElemMapper.updateById(updateObj);
    }

    @Override
    public int removeDpDataElem(List<Long> idList) {
        //判断当前元数据是否被模型及资产使用
        Long count = StandardsDataElemMapper.checkHasRel(idList);
        if (count > 0) {
            throw new ServiceException("数据元被模型或资产使用，请先解除关联关系");
        }
        // 批量删除数据元
        return StandardsDataElemMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDataElemDO getDpDataElemById(Long id) {
        MPJLambdaWrapper<StandardsDataElemDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(StandardsDataElemDO.class)
                .select("t2.NAME AS catName","t3.NICK_NAME AS personChargeName")
                .leftJoin("TAX_DATA_ELEM_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .leftJoin("SYSTEM_USER t3 on t.PERSON_CHARGE = " + ("mysql".equals(MasterDataSourceConfig.getDatabaseType()) ? "CAST(t3.USER_ID AS CHAR)" : "CAST(t3.USER_ID AS VARCHAR)") + " AND t3.DEL_FLAG = '0'")
                .eq(StandardsDataElemDO::getId, id);
        StandardsDataElemDO StandardsDataElemDO = StandardsDataElemMapper.selectJoinOne(StandardsDataElemDO.class, lambdaWrapper);

        if(StandardsDataElemDO.getDocumentId() != null){
            StandardsDocumentDO StandardsDocumentById = StandardsDocumentService.getDpDocumentById(StandardsDataElemDO.getDocumentId());
            StandardsDocumentById = StandardsDocumentById == null ? new StandardsDocumentDO():StandardsDocumentById;

            StandardsDataElemDO.setDocumentCode(StandardsDocumentById.getCode());
            StandardsDataElemDO.setDocumentName(StandardsDocumentById.getName());
            StandardsDataElemDO.setDocumentType(StandardsDocumentById.getType());
        }

        return StandardsDataElemDO;
    }

    @Override
    public List<StandardsDataElemDO> getDpDataElemList() {
        return StandardsDataElemMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDataElemDO> getDpDataElemMap() {
        List<StandardsDataElemDO> StandardsDataElemList = StandardsDataElemMapper.selectList();
        return StandardsDataElemList.stream()
                .collect(Collectors.toMap(
                        StandardsDataElemDO::getId,
                        StandardsDataElemDO -> StandardsDataElemDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据元数据
     *
     * @param importExcelList 数据元数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importDpDataElem(List<StandardsDataElemRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (StandardsDataElemRespVO respVO : importExcelList) {
            try {
                StandardsDataElemDO StandardsDataElemDO = BeanUtils.toBean(respVO, StandardsDataElemDO.class);
                Long StandardsDataElemId = respVO.getId();
                if (isUpdateSupport) {
                    if (StandardsDataElemId != null) {
                        StandardsDataElemDO existingDpDataElem = StandardsDataElemMapper.selectById(StandardsDataElemId);
                        if (existingDpDataElem != null) {
                            StandardsDataElemMapper.updateById(StandardsDataElemDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + StandardsDataElemId + " 的数据元记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + StandardsDataElemId + " 的数据元记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<StandardsDataElemDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", StandardsDataElemId);
                    StandardsDataElemDO existingDpDataElem = StandardsDataElemMapper.selectOne(queryWrapper);
                    if (existingDpDataElem == null) {
                        StandardsDataElemMapper.insert(StandardsDataElemDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + StandardsDataElemId + " 的数据元记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + StandardsDataElemId + " 的数据元记录已存在。");
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateStatus(Long id, Long status) {
        return this.update(Wrappers.lambdaUpdate(StandardsDataElemDO.class)
                .eq(StandardsDataElemDO::getId, id)
                .set(StandardsDataElemDO::getStatus, status));
    }

    @Override
    public Long getCountByCatCode(String catCode) {
        return baseMapper.selectCount(Wrappers.lambdaQuery(StandardsDataElemDO.class)
                .likeRight(StandardsDataElemDO::getCatCode, catCode));
    }
}
