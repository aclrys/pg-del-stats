package org.prebid.pg.delstats.services;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.model.dto.FreshnessStates;
import org.prebid.pg.delstats.model.dto.FreshnessStates.SummaryReportFreshness;
import org.prebid.pg.delstats.persistence.SystemState;
import org.prebid.pg.delstats.repository.SystemStateConstants;
import org.prebid.pg.delstats.repository.SystemStateRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class SystemStateServiceTest {

    @Mock
    private SystemStateRepository systemStateRepoMock;

    private SystemStateService systemStateService;

    private SoftAssertions softAssertions;

    @BeforeEach
    void setup() {
        this.systemStateService = new SystemStateService(systemStateRepoMock);
        this.softAssertions = new SoftAssertions();
    }

    @Test
    void shouldGetFreshnessStates() {
        String host = "host1";
        String vendor = "bidder";
        String extLineId = "line1";
        String val = Instant.now().toString();
        List<SystemState> states = new ArrayList<>();
        states.add(SystemState.builder()
                .tag("anyTag")
                .val(val)
                .build());
        states.add(SystemState.builder()
                .tag(String.format("%s-%s-%s-%s", host, vendor, extLineId,
                        SystemStateConstants.SYSTEM_STATE_TAG_SUMMARY_REPORT_SUFFIX))
                .val(val)
                .build());

        SummaryReportFreshness reportFreshness = SummaryReportFreshness.builder()
                .serviceInstanceId(host)
                .source(vendor)
                .extLineItemId(extLineId)
                .ts(val)
                .build();
        given(systemStateRepoMock.findAll()).willReturn(states);
        FreshnessStates freshnessStates = systemStateService.getFreshnessStates();
        assertThat(freshnessStates.getLatestSummaryReportWindowEndTimestamps()).hasSize(1);
        assertThat(freshnessStates.getLatestSummaryReportWindowEndTimestamps().get(0)).isEqualTo(reportFreshness);
    }

}

