package org.prebid.pg.delstats.services;

import org.prebid.pg.delstats.model.dto.FreshnessStates;
import org.prebid.pg.delstats.model.dto.FreshnessStates.SummaryReportFreshness;
import org.prebid.pg.delstats.persistence.SystemState;
import org.prebid.pg.delstats.repository.SystemStateConstants;
import org.prebid.pg.delstats.repository.SystemStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A service for CRUD of {@link SystemState}.
 */

@Service
public class SystemStateService {

    private final SystemStateRepository systemStateRepo;

    public SystemStateService(SystemStateRepository systemStateRepo) {
        this.systemStateRepo = systemStateRepo;
    }

    @Transactional(readOnly = true)
    public FreshnessStates getFreshnessStates() {
        List<SystemState> systemStates = systemStateRepo.findAll();
        List<SummaryReportFreshness> states = systemStates.stream()
                .filter(item -> item.getTag().endsWith(SystemStateConstants.SYSTEM_STATE_TAG_SUMMARY_REPORT_SUFFIX))
                .map(item -> {
                    String[] parts = item.getTag().split("-");
                    return SummaryReportFreshness.builder()
                            .serviceInstanceId(parts[0])
                            .source(parts[1])
                            .extLineItemId(parts[2])
                            .ts(item.getVal())
                            .build();
                })
                .sorted(Comparator.comparing(SummaryReportFreshness::getTs))
                .collect(Collectors.toList());
        return FreshnessStates.builder()
                .latestSummaryReportWindowEndTimestamps(states)
                .build();
    }

}
