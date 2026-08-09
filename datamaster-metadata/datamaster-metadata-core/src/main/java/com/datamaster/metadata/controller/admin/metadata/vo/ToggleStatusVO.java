package com.datamaster.metadata.controller.admin.metadata.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ToggleStatusVO {

    @NotNull(message = "id null")
    private Long id;

    @NotBlank(message = "status blank")
    private String status;

}
