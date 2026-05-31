package com.datamaster.module.standards.service.document.impl;

import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.api.service.document.IStandardsDocumentApiService;
import com.datamaster.module.standards.controller.admin.document.vo.*;
import com.datamaster.module.standards.dal.dataobject.document.StandardsDocumentDO;
import com.datamaster.module.standards.dal.mapper.document.StandardsDocumentMapper;
import com.datamaster.module.standards.service.document.IStandardsDocumentService;
import com.datamaster.mybatis.core.util.MyBatisUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标准信息登记Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDocumentServiceImpl  extends ServiceImpl<StandardsDocumentMapper,StandardsDocumentDO> implements IStandardsDocumentService, IStandardsDocumentApiService {
    @Resource
    private StandardsDocumentMapper StandardsDocumentMapper;

    @Override
    public PageResult<StandardsDocumentDO> getDpDocumentPage(StandardsDocumentPageReqVO pageReqVO) {
        return StandardsDocumentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<StandardsDocumentDO> getDpDocumentList(StandardsDocumentPageReqVO reqVO) {
        MPJLambdaWrapper<StandardsDocumentDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(StandardsDocumentDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_DOCUMENT_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .like(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getName()), StandardsDocumentDO::getName, reqVO.getName())
                .like(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getCode()), StandardsDocumentDO::getCode, reqVO.getCode())
                .and(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getKeyWordParam()),
                        q -> q.like(StandardsDocumentDO::getCode, reqVO.getKeyWordParam())
                                .or()
                                .like(StandardsDocumentDO::getName, reqVO.getKeyWordParam()))
                .like(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getKeyWordParam()), StandardsDocumentDO::getCode, reqVO.getKeyWordParam())
                .like(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getKeyWordParam()), StandardsDocumentDO::getName, reqVO.getKeyWordParam())
                .like(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getIssuingAgency()), StandardsDocumentDO::getIssuingAgency, reqVO.getIssuingAgency())
                .likeRight(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getCatCode()), StandardsDocumentDO::getCatCode, reqVO.getCatCode())
                .eq(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getType()),StandardsDocumentDO::getType, reqVO.getType())
                .eq(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getStatus()),StandardsDocumentDO::getStatus, reqVO.getStatus())
                .eq(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getVersion()),StandardsDocumentDO::getVersion, reqVO.getVersion());
        return StandardsDocumentMapper.selectList(lambdaWrapper);
    }

    @Override
    public Long createDpDocument(StandardsDocumentSaveReqVO createReqVO) {
        StandardsDocumentDO dictType = BeanUtils.toBean(createReqVO, StandardsDocumentDO.class);
        StandardsDocumentMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpDocument(StandardsDocumentSaveReqVO updateReqVO) {
        // 相关校验

        // 更新标准信息登记
        StandardsDocumentDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDocumentDO.class);
        return StandardsDocumentMapper.updateById(updateObj);
    }
    @Override
    public int removeDpDocument(Collection<Long> idList) {
        // 批量删除标准信息登记
        return StandardsDocumentMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDocumentDO getDpDocumentById(Long id) {
        MPJLambdaWrapper<StandardsDocumentDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(StandardsDocumentDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_DOCUMENT_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .eq( StandardsDocumentDO::getId, id);
        return StandardsDocumentMapper.selectOne(lambdaWrapper);
    }

    @Override
    public List<StandardsDocumentDO> getDpDocumentList() {
        return StandardsDocumentMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDocumentDO> getDpDocumentMap() {
        List<StandardsDocumentDO> StandardsDocumentList = StandardsDocumentMapper.selectList();
        return StandardsDocumentList.stream()
                .collect(Collectors.toMap(
                        StandardsDocumentDO::getId,
                        StandardsDocumentDO -> StandardsDocumentDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入标准信息登记数据
         *
         * @param importExcelList 标准信息登记数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDpDocument(List<StandardsDocumentRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsDocumentRespVO respVO : importExcelList) {
                try {
                    StandardsDocumentDO StandardsDocumentDO = BeanUtils.toBean(respVO, StandardsDocumentDO.class);
                    Long StandardsDocumentId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsDocumentId != null) {
                            StandardsDocumentDO existingDpDocument = StandardsDocumentMapper.selectById(StandardsDocumentId);
                            if (existingDpDocument != null) {
                                StandardsDocumentMapper.updateById(StandardsDocumentDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsDocumentId + " 的标准信息登记记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsDocumentId + " 的标准信息登记记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsDocumentDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsDocumentId);
                        StandardsDocumentDO existingDpDocument = StandardsDocumentMapper.selectOne(queryWrapper);
                        if (existingDpDocument == null) {
                            StandardsDocumentMapper.insert(StandardsDocumentDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsDocumentId + " 的标准信息登记记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsDocumentId + " 的标准信息登记记录已存在。");
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
    public PageResult<StandardsDocumentSearchRespVO> getDpDocumentSearchPage(StandardsDocumentSearchReqVO StandardsDocument) {
        IPage<StandardsDocumentSearchRespVO> mpPage = StandardsDocumentMapper.getDpDocumentSearchPage(MyBatisUtils.buildPage(StandardsDocument),StandardsDocument);//BeanUtils.toBean(StandardspEtlTaskDOPageResult, StandardspEtlTaskRespVO.class);
        return new PageResult(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    public Long getCountByCatCode(String catCode) {
        return baseMapper.selectCount(Wrappers.lambdaQuery(StandardsDocumentDO.class)
                .likeRight(StandardsDocumentDO::getCatCode, catCode));
    }

}
