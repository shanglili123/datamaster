

package com.datamaster.neo4j.config;

import lombok.Data;
import org.neo4j.driver.internal.value.MapValue;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 18:55
 **/
@Data
@Component
@ConfigurationProperties(prefix = "spring.data.neo4j")
public class Neo4jProperties {
    String uri;
    String username;
    String password;
}

