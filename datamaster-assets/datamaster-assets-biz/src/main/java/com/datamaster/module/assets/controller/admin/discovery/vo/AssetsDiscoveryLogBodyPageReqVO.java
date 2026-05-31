package com.datamaster.module.assets.controller.admin.discovery.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;
import java.util.Date;/** * - Request VO  DA_DISCOVERY_LOG_BODY * * @author DATAMASTER * @date 2025-10-15 */
@Schema(description = "- Request VO")
@Data
public class AssetsDiscoveryLogBodyPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
@Schema(description = "id", example = "123")    private Long taskId;
@Schema(description = "", example = "")    private String logContent;
@Schema(description = ";01", example = "1")    private Boolean validFlag;
@Schema(description = ";10", example = "0")    private Boolean delFlag;
@Schema(description = "", example = "2025-10-01 00:00:00")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date beginTm;
@Schema(description = "", example = "2025-10-31 23:59:59")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date endTm;}
