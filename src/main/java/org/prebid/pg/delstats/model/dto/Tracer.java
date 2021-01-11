package org.prebid.pg.delstats.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

@Setter
@ToString
@Component
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Tracer {

    private boolean enabled;

    private boolean raw;

    private int durationInSeconds;

    private Instant expiresAt = Instant.EPOCH;

    private Filters filters = new Filters();

    public void setTracer(Tracer tracerIn) {
        enabled = tracerIn.enabled;
        if (enabled) {
            expiresAt = Instant.now().plus(Duration.ofSeconds(tracerIn.durationInSeconds));
            filters.accountId = tracerIn.filters.accountId;
            filters.bidderCode = tracerIn.filters.bidderCode == null ? null : tracerIn.filters.bidderCode.toUpperCase();
            filters.lineItemId = tracerIn.filters.lineItemId;
            filters.region = tracerIn.filters.region;
            filters.vendor = tracerIn.filters.vendor;
            raw = tracerIn.raw;
            durationInSeconds = tracerIn.durationInSeconds;
        } else {
            expiresAt = Instant.EPOCH;
            durationInSeconds = 0;
            filters.setNull();
        }
    }

    public boolean isActive() {
        return enabled && Instant.now().isBefore(expiresAt);
    }

    public boolean isActiveAndRaw() {
        return enabled && Instant.now().isBefore(expiresAt) && raw;
    }

    public boolean isMatchingOn(String vendor, String region, String bidderCode, String lineItemId) {
        return matchVendor(vendor) && matchRegion(region) && matchBidderCode(bidderCode) && matchLineItemId(lineItemId);
    }

    public boolean matchAccount(String accountIdData) {
        return StringUtils.isEmpty(filters.accountId) || accountIdData.equals(filters.accountId);
    }

    public boolean matchBidderCode(String bidderCodeData) {
        return
                StringUtils.isEmpty(filters.bidderCode)
                ||
                bidderCodeData.equalsIgnoreCase(filters.bidderCode)
                ||
                bidderCodeData.toUpperCase().contains(filters.bidderCode);
    }

    public boolean matchLineItemId(String lineItemIdData) {
        return
                StringUtils.isEmpty(filters.lineItemId)
                ||
                lineItemIdData.equals(filters.lineItemId)
                ||
                lineItemIdData.toUpperCase().contains(filters.lineItemId)
                ||
                filters.lineItemId.toUpperCase().contains(lineItemIdData);
    }

    public boolean matchRegion(String regionData) {
        return StringUtils.isEmpty(filters.region) || regionData.equals(filters.region);
    }

    public boolean matchVendor(String vendorData) {
        return StringUtils.isEmpty(filters.vendor) || vendorData.equals(filters.vendor);
    }

    public boolean match(
            String vendorData, String regionData, String bidderCodeData, String lineItemIdData, String accountIdData
    ) {
        return matchVendor(vendorData)
                && matchRegion(regionData)
                && matchBidderCode(bidderCodeData)
                && matchLineItemId(lineItemIdData)
                && matchAccount(accountIdData);
    }
}

@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Filters {

    String accountId;

    String bidderCode;

    String lineItemId;

    String region;

    String vendor;

    void setNull() {
        accountId = null;
        bidderCode = null;
        lineItemId = null;
        region = null;
        vendor = null;
    }
}
