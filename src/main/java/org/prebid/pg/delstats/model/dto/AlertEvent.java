package org.prebid.pg.delstats.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
public class AlertEvent {

    private String id;

    private String action;

    private String priority;

    private Instant updatedAt;

    private String name;

    private String details;

    private AlertSource source;
}
