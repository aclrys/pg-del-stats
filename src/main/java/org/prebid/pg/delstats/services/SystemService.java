package org.prebid.pg.delstats.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.exception.SystemInitializationException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Instant;

@Service
public class SystemService {
    private static final Logger logger = LoggerFactory.getLogger(SystemService.class);

    private final DeploymentConfiguration deploymentConfiguration;

    private final Tracer tracer;

    private final Shutdown shutdown;

    private final AlertProxyHttpClient alertProxyHttpClient;

    private final String serviceInstanceId;

    private final GraphiteMetricsRecorder recorder;

    private final ObjectMapper objectMapper;

    private final Instant startupTime = Instant.now();

    public SystemService(DeploymentConfiguration deploymentConfiguration,
                         Tracer tracer,
                         Shutdown shutdown,
                         AlertProxyHttpClient alertProxyHttpClient,
                         GraphiteMetricsRecorder recorder,
                         ObjectMapper objectMapper) {
        this.deploymentConfiguration = deploymentConfiguration;
        this.tracer = tracer;
        this.shutdown = shutdown;
        this.alertProxyHttpClient = alertProxyHttpClient;
        this.recorder = recorder;
        this.objectMapper = objectMapper;

        if (DeploymentConfiguration.InfraType.ECS.equals(deploymentConfiguration.getInfra())) {
            serviceInstanceId = DeploymentConfiguration.NA_SERVICE_INSTANCE_ID;
        } else {
            try {
                serviceInstanceId = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                throw new SystemInitializationException("Exception in reading hostname");
            }
        }
        logger.info("server::serviceInstanceId={}", serviceInstanceId);
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public DeploymentConfiguration getDeploymentConfiguration() {
        return deploymentConfiguration;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public Shutdown getShutdown() {
        return shutdown;
    }

    public AlertProxyHttpClient getAlertProxyHttpClient() {
        return alertProxyHttpClient;
    }

    public GraphiteMetricsRecorder getRecorder() {
        return recorder;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public Instant getStartupTime() {
        return startupTime;
    }

}

