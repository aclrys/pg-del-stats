package org.prebid.pg.delstats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
public class AlertSource {

    private String env;

    @JsonProperty("data-center")
    private String dataCenter;

    private String region;

    private String system;

    @JsonProperty("sub-system")
    private String subSystem;

    @JsonProperty("host-id")
    private String hostId;
}
