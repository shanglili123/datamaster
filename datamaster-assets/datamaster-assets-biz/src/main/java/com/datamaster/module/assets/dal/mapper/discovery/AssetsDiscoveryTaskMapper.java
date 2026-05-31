package com.datamaster.module.assets.dal.mapper.discovery;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskPageReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
public interface AssetsDiscoveryTaskMapper extends BaseMapperX<AssetsDiscoveryTaskDO> {

    default PageResult<AssetsDiscoveryTaskDO> selectPage(AssetsDiscoveryTaskPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<AssetsDiscoveryTaskDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsDiscoveryTaskDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_DISCOVER_TASK_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getName()), AssetsDiscoveryTaskDO::getName, reqVO.getName())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), AssetsDiscoveryTaskDO::getCatCode, reqVO.getCatCode())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), AssetsDiscoveryTaskDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getContactId() != null, AssetsDiscoveryTaskDO::getContactId, reqVO.getContactId())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);

        return selectJoinPage(reqVO, AssetsDiscoveryTaskDO.class, lambdaWrapper);

    }

    /**
     *  CAT_CODE  CAT_CODE
     *
     * @param oldCatCode
     * @param newCatCode
     * @return
     */
    default int updateCatCode(String oldCatCode, String newCatCode) {
        return this.update(
                null,
                Wrappers.<AssetsDiscoveryTaskDO>lambdaUpdate()
                        .set(AssetsDiscoveryTaskDO::getCatCode, newCatCode)
                        .eq(AssetsDiscoveryTaskDO::getDelFlag, "0")
                        .eq(AssetsDiscoveryTaskDO::getCatCode, oldCatCode)
        );
    }
}
