package org.prebid.pg.delstats.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

@Configuration
@Data
@ToString
@Validated
@NoArgsConstructor
@ConfigurationProperties(prefix = "deployment")
@Slf4j
public class DeploymentConfiguration {
    public static final String NA_SERVICE_INSTANCE_ID = "NA";

    @NotNull
    ProfileType profile;

    @NotNull
    InfraType infra;

    @AllArgsConstructor
    public enum ProfileType {
        PROD,
        TEST,
        QA,
        DEV,
        ALGOTEST;
    }

    @AllArgsConstructor
    public enum InfraType {
        VM,
        EC2,
        ECS;
    }

    @NotNull
    private String dataCenter;

    @NotNull
    private String region;

    @NotNull
    private String system;

    @NotNull
    private String subSystem;

    @PostConstruct
    public void init() {
        log.info("Deployment Configuration is {} - {}.", profile, infra);
    }
}
