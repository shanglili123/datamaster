

package com.datamaster.metadata.controller.qa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.metadata.controller.qa.vo.EvaluateLogRespVO;
import com.datamaster.metadata.controller.qa.vo.EvaluateLogStatisticsVO;

import java.util.List;

@Data
public class QualityTaskAssetRespVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;

    @Schema(description = "资产id")
    private Long assetId;


    /** 评分 */
    private Long score;

    /** 问题数据 */
    private Long problemData;

    private List<EvaluateLogStatisticsVO> voList;

    private List<EvaluateLogRespVO> evaluateLogRespVOS;
}
