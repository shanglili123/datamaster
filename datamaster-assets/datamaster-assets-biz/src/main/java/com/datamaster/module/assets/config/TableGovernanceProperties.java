package com.datamaster.module.assets.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "datamaster.governance.table-access")
public class TableGovernanceProperties {

    private Boolean enabled = false;

    /**
     * off: no enforcement; warn: resolve only; strict: deny when asset exists and project is not authorized.
     */
    private String mode = "off";

    private Boolean fallbackToCatalog = true;
}
