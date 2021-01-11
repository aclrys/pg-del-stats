package org.prebid.pg.delstats.repository;

import org.prebid.pg.delstats.persistence.DeliveryReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public interface DeliveryProgressReportsRepository extends JpaRepository<DeliveryReport, Long> {

    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "1000"),
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    @Query(value = "SELECT dr FROM DeliveryReport dr WHERE dr.bidderCode = :bidderCode AND"
            + " dr.reportTimestamp >= :startTime AND dr.reportTimestamp < :endTime ")
    Page<DeliveryReport> findByBidderCodeAndReportTimestampRange(
            @Param("bidderCode") String bidderCode,
            @Param("startTime") Timestamp startTime,
            @Param("endTime") Timestamp endTime, Pageable page);

    @Query(value = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_AGGREGATION_SQL,
            countQuery = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_AGGREGATION_COUNT_SQL,
            nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "1000"),
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    Page<DeliveryReport> retrieveSummaryData(@Param("startTime") Timestamp startTime,
                                             @Param("endTime") Timestamp endTime,
                                             Pageable pageable);

    @Query(value = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_FIND_OLD_SQL, nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "1000"),
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    List<DeliveryReport> findOldRecords(@Param("expired") Timestamp expired,
                                             Pageable pageable);

    @Query(value = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_GET_WITHIN_TIME_RANGE_SQL,
            countQuery = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_GET_WITHIN_TIME_RANGE_COUNT_SQL,
            nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "1000"),
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    Page<DeliveryReport> findWithinDataStartEnd(@Param("startTime") Timestamp startTime,
                                                @Param("endTime") Timestamp endTime, Pageable page);

    @Query(value = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_GET_LINES_BY_TIME_RANGE_SQL,
            countQuery = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_GET_LINES_BY_TIME_RANGE_COUNT_SQL,
            nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    Page<DeliveryReport> getLineItemReportsWithTimeRange(@Param("startTime") Instant startTime,
             @Param("endTime") Instant endTime, Pageable page);

    @Query(value = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_GET_BY_LINES_TIME_RANGE_SQL,
            countQuery = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_GET_BY_LINES_TIME_RANGE_COUNT_SQL,
            nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    Page<DeliveryReport> getLineItemReportsByLineIdsWithTimeRange(@Param("lineItemIds") List<String> lineItemIds,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable page);

    @Query(value = DeliveryProgressReportsConstants.DELIVERY_PROGRESS_REPORT_GET_REGION_AND_VENDORS_BY_TIME_RANGE_SQL,
            nativeQuery = true)
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    List<Object> getDistinctVendorRegion(@Param("startTime") Timestamp startTime,
                                         @Param("endTime") Timestamp endTime);
}
