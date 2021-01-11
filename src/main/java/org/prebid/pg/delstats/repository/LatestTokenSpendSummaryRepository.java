package org.prebid.pg.delstats.repository;

import org.prebid.pg.delstats.persistence.LatestTokenSpendSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface LatestTokenSpendSummaryRepository extends JpaRepository<LatestTokenSpendSummary, Long> {

    @Query(value = LatestTokenSpendSummaryConstants.LATEST_TOKEN_SPEND_RETRIEVE_SINCE_SQL,
            countQuery = LatestTokenSpendSummaryConstants.LATEST_TOKEN_SPEND_RETRIEVE_SINCE_COUNT_SQL,
            nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "10000"),
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    List<LatestTokenSpendSummary> retrieveSince(@Param("report_timestamp") Timestamp reportTimestamp);

    @Query(value = LatestTokenSpendSummaryConstants.LATEST_TOKEN_SPEND_RETRIEVE_SINCE_BY_VENDOR_REGION_SQL,
            countQuery = LatestTokenSpendSummaryConstants.LATEST_TOKEN_SPEND_RETRIEVE_SINCE_BY_VENDOR_REGION_COUNT_SQL,
            nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "10000"),
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    List<LatestTokenSpendSummary> retrieveSinceByVednorRegion(@Param("report_timestamp") Timestamp reportTimestamp,
                                                              @Param("vendor") String vendor,
                                                              @Param("region") String region);

    @Query(value = LatestTokenSpendSummaryConstants.LATEST_TOKEN_SPEND_FIND_OLDEST_SQL, nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "1000"),
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    List<LatestTokenSpendSummary> findOldRecords(@Param("expired") Timestamp expired, Pageable pageable);

    @Modifying
    @Query(value = LatestTokenSpendSummaryConstants.LATEST_TOKEN_SPEND_UPSERT_TOKEN_MATCH_COUNT_SQL,
            nativeQuery = true)
    int upsertTokenSummaries(@Param("vendor") String vendor, @Param("region") String region,
                              @Param("startTime") Timestamp starTime, @Param("endTime") Timestamp endTime);
}
