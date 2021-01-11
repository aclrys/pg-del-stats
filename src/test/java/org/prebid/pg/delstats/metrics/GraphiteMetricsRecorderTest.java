package org.prebid.pg.delstats.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.config.GraphiteConfig;
import org.prebid.pg.delstats.exception.MissingTransactionIdException;
import org.prebid.pg.delstats.repository.RepositoryItem;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphiteMetricsRecorderTest {
    SoftAssertions softAssertions;

    GraphiteMetricsRecorder recorder;

    ConsoleReporter consoleReporter;

    ByteArrayOutputStream reportOutput;

    @BeforeEach
    public void init() {
        softAssertions = new SoftAssertions();

        GraphiteConfig graphiteConfig = mock(GraphiteConfig.class);
        when(graphiteConfig.isEnabled()).thenReturn(false);

        recorder = new GraphiteMetricsRecorder(graphiteConfig);
        reportOutput = new ByteArrayOutputStream();
        consoleReporter = ConsoleReporter.forRegistry(recorder.getRegistry())
                .outputTo(new PrintStream(reportOutput))
                .filter(MetricFilter.contains("post-delivery-report"))
                .build();
    }

    @Test
    public void shouldNotJostleUpAndDown() {
        // Because Registry is static, in test suites where other tests might have entered
        reportOutput.reset();
        for (int loops = 0; loops < 5; loops++) {
            for (int req = 0; req < 6; req++) {
                recorder.markPostRequestForDeliveryReport();
            }
            consoleReporter.report();
            softAssertions.assertThat(reportOutput.toString())
                    .as("In loop {} after 6 calls, markPostRequestForDeliveryReport off", loops)
                    .contains("count = 6");
            reportOutput.reset();
        }
        for (int loops = 0; loops < 5; loops++) {
            for (int req = 0; req < 3; req++) {
                recorder.markPostRequestForDeliveryReport();
            }
            consoleReporter.report();
            softAssertions.assertThat(reportOutput.toString())
                    .as("In loop {} after 3 calls, markPostRequestForDeliveryReport off", loops)
                    .contains("count = 3");
            reportOutput.reset();
        }
        for (int loops = 0; loops < 5; loops++) {
            for (int req = 0; req < 5; req++) {
                recorder.markPostRequestForDeliveryReport();
            }
            consoleReporter.report();
            softAssertions.assertThat(reportOutput.toString())
                    .as("In loop {} after 5 calls, markPostRequestForDeliveryReport off", loops)
                    .contains("count = 5");
            reportOutput.reset();
        }
        softAssertions.assertAll();
    }

    @Test
    public void shouldUpdateRegistryOnMarkingExceptions() {
        GraphiteConfig graphiteConfig = mock(GraphiteConfig.class);
        when(graphiteConfig.isEnabled()).thenReturn(true);

        Graphite graphite = mock(Graphite.class);
        GraphiteMetricsRecorder graphiteMetricsRecorder = new GraphiteMetricsRecorder(graphiteConfig);
        graphiteMetricsRecorder.setGraphiteSupplier((config) -> graphite);
        when(graphite.isConnected()).thenReturn(true);

        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.init()).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markExceptionMeter(new RuntimeException())).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markExceptionMeter(new DataAccessResourceFailureException(""))).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markExceptionMeter(new DuplicateKeyException(""))).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markExceptionMeter(new QueryTimeoutException(""))).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markExceptionMeter(new MissingTransactionIdException(""))).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markExceptionMeter(new HttpMessageNotReadableException("", mock(HttpInputMessage.class)))).doesNotThrowAnyException();

        softAssertions.assertAll();
    }

    @Test
    public void shouldUpdateRegistryOnMarkingEvent() {
        GraphiteConfig graphiteConfig = mock(GraphiteConfig.class);
        when(graphiteConfig.isEnabled()).thenReturn(true);

        Graphite graphite = mock(Graphite.class);
        GraphiteMetricsRecorder graphiteMetricsRecorder = new GraphiteMetricsRecorder(graphiteConfig);
        graphiteMetricsRecorder.setGraphiteSupplier((config) -> graphite);
        when(graphite.isConnected()).thenReturn(true);

        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.init()).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markGetRequestForDeliveryLineItems()).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markGetRequestForDeliveryStats()).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markGetRequestForTokenSpend()).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markScheduledAggregation()).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markScheduledDeliverySummary()).doesNotThrowAnyException();

        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markDeliveryReportsSummariesFetched(1)).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markDeliveryReportRecordsFetched(1)).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markLatestTokenSpendSummariesFetched(1)).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markLatestTokenSpendSummariesStored(1)).doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.markDeliverySummariesStored(1)).doesNotThrowAnyException();

        softAssertions.assertAll();
    }

    @Test
    public void shouldNotThrowExceptionForGettingTimers() {
        GraphiteConfig graphiteConfig = mock(GraphiteConfig.class);
        when(graphiteConfig.isEnabled()).thenReturn(true);

        Graphite graphite = mock(Graphite.class);
        GraphiteMetricsRecorder graphiteMetricsRecorder = new GraphiteMetricsRecorder(graphiteConfig);
        graphiteMetricsRecorder.setGraphiteSupplier((config) -> graphite);
        when(graphite.isConnected()).thenReturn(true);

        softAssertions.assertThatCode(() -> graphiteMetricsRecorder.init()).doesNotThrowAnyException();
        softAssertions.assertThat(graphiteMetricsRecorder.getDeliveryReportsPerformanceTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.getDeliveryReportStatsPerformanceTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.getDeliveryReportLinesPerformanceTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.postDeliveryReportPerformanceTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.getTokenSpendPerformanceTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.scheduledAggregationTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.scheduledDeliverySummaryTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.repositoryFetchDeliveryReportsSummaryTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.repositoryFetchDeliveryReportLinesTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.repositoryStoreDeliveryReportLinesTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.repositoryFetchTokenSpendTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);
        softAssertions.assertThat(graphiteMetricsRecorder.repositoryStoreTokenSpendTimer().get())
                .isExactlyInstanceOf(Timer.Context.class);

        softAssertions.assertAll();
    }
}
