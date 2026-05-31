

package com.datamaster.module.standards.service.desensitizeList;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnSaveReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnPageReqVO;
import com.datamaster.module.standards.dal.dataobject.desensitizeList.StandardsDesensitizeAssetcolumnDO;
/**
 * 脱敏清单关联关系Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
public interface IStandardsDesensitizeAssetcolumnService extends IService<StandardsDesensitizeAssetcolumnDO> {

    /**
     * 获得脱敏清单关联关系分页列表
     *
     * @param pageReqVO 分页请求
     * @return 脱敏清单关联关系分页列表
     */
    PageResult<StandardsDesensitizeAssetcolumnDO> getDgDesensitizeAssetcolumnPage(StandardsDesensitizeAssetcolumnPageReqVO pageReqVO);

    /**
     * 创建脱敏清单关联关系
     *
     * @param createReqVO 脱敏清单关联关系信息
     * @return 脱敏清单关联关系编号
     */
    Long createDgDesensitizeAssetcolumn(StandardsDesensitizeAssetcolumnSaveReqVO createReqVO);

    /**
     * 更新脱敏清单关联关系
     *
     * @param updateReqVO 脱敏清单关联关系信息
     */
    int updateDgDesensitizeAssetcolumn(StandardsDesensitizeAssetcolumnSaveReqVO updateReqVO);

    /**
     * 删除脱敏清单关联关系
     *
     * @param idList 脱敏清单关联关系编号
     */
    int removeDgDesensitizeAssetcolumn(Collection<Long> idList);

    /**
     * 获得脱敏清单关联关系详情
     *
     * @param id 脱敏清单关联关系编号
     * @return 脱敏清单关联关系
     */
    StandardsDesensitizeAssetcolumnDO getDgDesensitizeAssetcolumnById(Long id);

    StandardsDesensitizeAssetcolumnDO getDgDesensitizeAssetcolumnByAid(Long assetcolumnId);

    /**
     * 获得全部脱敏清单关联关系列表
     *
     * @return 脱敏清单关联关系列表
     */
    List<StandardsDesensitizeAssetcolumnDO> getDgDesensitizeAssetcolumnList();

    /**
     * 获得全部脱敏清单关联关系 Map
     *
     * @return 脱敏清单关联关系 Map
     */
    Map<Long, StandardsDesensitizeAssetcolumnDO> getDgDesensitizeAssetcolumnMap();


    /**
     * 导入脱敏清单关联关系数据
     *
     * @param importExcelList 脱敏清单关联关系数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDgDesensitizeAssetcolumn(List<StandardsDesensitizeAssetcolumnRespVO> importExcelList, boolean isUpdateSupport, String operName);

    PageResult<StandardsDesensitizeAssetcolumnDO> getDgDesensitizePagebyRuleId(StandardsDesensitizeAssetcolumnPageReqVO StandardsDesensitizeAssetcolumn);

    StandardsDesensitizeAssetcolumnDO getByassetcolumnId(Long assetcolumnId);

    int deleteByassetcolumnId(Long assetcolumnId);
}
