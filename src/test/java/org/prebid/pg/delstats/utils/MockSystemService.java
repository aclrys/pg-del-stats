package org.prebid.pg.delstats.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.services.SystemService;

import static org.mockito.Mockito.mock;

public class MockSystemService extends SystemService {
    public MockSystemService() {
        this(mock(DeploymentConfiguration.class));
    }

    public MockSystemService(Shutdown shutdown) {
        super(mock(DeploymentConfiguration.class), mock(Tracer.class), shutdown,
                mock(AlertProxyHttpClient.class), mock(GraphiteMetricsRecorder.class), new ObjectMapper());
    }

    public MockSystemService(DeploymentConfiguration deploymentConfiguration) {
        super(deploymentConfiguration, mock(Tracer.class), mock(Shutdown.class),
                mock(AlertProxyHttpClient.class), mock(GraphiteMetricsRecorder.class), new ObjectMapper());

    }
}
