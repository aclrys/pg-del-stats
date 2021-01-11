package org.prebid.pg.delstats.services;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.utils.MockSystemService;

@ExtendWith(MockitoExtension.class)
public class SystemServiceTest {
    SoftAssertions softAssertions;

    @BeforeEach
    public void setup() {
        softAssertions = new SoftAssertions();
    }

    @Test
    public void shouldProvideNAServiceIdOnInitializationAsECS() {
        DeploymentConfiguration deploymentConfiguration = new DeploymentConfiguration();
        deploymentConfiguration.setProfile(DeploymentConfiguration.ProfileType.TEST);
        deploymentConfiguration.setInfra(DeploymentConfiguration.InfraType.ECS);

        softAssertions.assertThat(new MockSystemService(deploymentConfiguration).getServiceInstanceId())
                .isEqualTo(DeploymentConfiguration.NA_SERVICE_INSTANCE_ID);

        softAssertions.assertAll();
    }

    @Test
    public void shouldProvideHostServiceIdOnInitialzationAsVM() {
        DeploymentConfiguration deploymentConfiguration = new DeploymentConfiguration();
        deploymentConfiguration.setProfile(DeploymentConfiguration.ProfileType.TEST);
        deploymentConfiguration.setInfra(DeploymentConfiguration.InfraType.VM);

        softAssertions.assertThat(new MockSystemService(deploymentConfiguration).getServiceInstanceId())
                .isNotEqualTo(DeploymentConfiguration.NA_SERVICE_INSTANCE_ID);

        softAssertions.assertAll();
    }
}
