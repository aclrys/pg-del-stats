package org.prebid.pg.delstats.model.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TokenSpendSummaryDto {

    List<TokenSpendSummaryLineDto> tokenSpendSummaryLines;
}
