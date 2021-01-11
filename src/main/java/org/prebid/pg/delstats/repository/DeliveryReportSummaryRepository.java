package org.prebid.pg.delstats.repository;

import org.prebid.pg.delstats.model.dto.PlanDataSummary;
import org.prebid.pg.delstats.persistence.DeliveryReportSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public interface DeliveryReportSummaryRepository extends JpaRepository<DeliveryReportSummary, Integer> {

    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    @Query(value = "SELECT MAX(id) AS id, MIN(report_window_start_timestamp) AS report_window_start_timestamp,"
            + " MAX(report_window_end_timestamp) AS report_window_end_timestamp,"
            + " MIN(data_window_start_timestamp) AS data_window_start_timestamp,"
            + " MAX(data_window_end_timestamp) AS data_window_end_timestamp,"
            + " line_item_id, ext_line_item_id, bidder_code, line_item_source,"
            + " SUM(account_auctions) AS account_auctions, SUM(domain_matched) AS domain_matched, "
            + " SUM(target_matched) AS target_matched, SUM(target_matched_but_fcapped) AS target_matched_but_fcapped,"
            + " SUM(target_matched_but_fcap_lookup_failed) AS target_matched_but_fcap_lookup_failed,"
            + " SUM(pacing_deferred) AS pacing_deferred, SUM(sent_to_bidder) AS sent_to_bidder,"
            + " SUM(sent_to_bidder_as_top_match) AS sent_to_bidder_as_top_match,"
            + " SUM(received_from_bidder_invalidated) AS received_from_bidder_invalidated,"
            + " SUM(received_from_bidder) AS received_from_bidder,"
            + " SUM(sent_to_client) AS sent_to_client, SUM(sent_to_client_as_top_match) AS sent_to_client_as_top_match,"
            + " SUM(win_events) AS win_events, plan_data, MAX(created_at) AS created_at"
            + " FROM delivery_progress_reports_summary"
            + " WHERE report_window_start_timestamp >= :startTime AND report_window_end_timestamp < :endTime"
            + " GROUP BY DATE_FORMAT(report_window_start_timestamp, '%Y%m%d%H'), line_item_id"
            + " ORDER BY data_window_start_timestamp, line_item_id",
            countQuery = "SELECT COUNT(*) FROM delivery_progress_reports_summary"
                    + " WHERE report_window_start_timestamp >= :startTime AND report_window_end_timestamp < :endTime",
            nativeQuery = true)
    Page<DeliveryReportSummary> findByReportWindowHour(@Param("startTime") Instant startTime,
                                                       @Param("endTime") Instant endTime, Pageable pageable);

    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    @Query("SELECT new org.prebid.pg.delstats.model.dto.PlanDataSummary(t.lineItemId, t.planData) "
            + " FROM DeliveryReportSummary t"
            + " WHERE t.reportWindowStartTimestamp >= :startTime AND t.reportWindowEndTimestamp < :endTime"
           )
    List<PlanDataSummary> findPlanDataByReportWindowHour(@Param("startTime") Instant startTime,
                                                         @Param("endTime") Instant endTime);

    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "15000") }
    )
    @Query(value = "SELECT MAX(id) AS id, MIN(report_window_start_timestamp) AS report_window_start_timestamp,"
            + " MAX(report_window_end_timestamp) AS report_window_end_timestamp,"
            + " MIN(data_window_start_timestamp) AS data_window_start_timestamp,"
            + " MAX(data_window_end_timestamp) AS data_window_end_timestamp,"
            + " line_item_id, ext_line_item_id, bidder_code, line_item_source,"
            + " SUM(account_auctions) AS account_auctions, SUM(domain_matched) AS domain_matched, "
            + " SUM(target_matched) AS target_matched, SUM(target_matched_but_fcapped) AS target_matched_but_fcapped,"
            + " SUM(target_matched_but_fcap_lookup_failed) AS target_matched_but_fcap_lookup_failed,"
            + " SUM(pacing_deferred) AS pacing_deferred, SUM(sent_to_bidder) AS sent_to_bidder,"
            + " SUM(sent_to_bidder_as_top_match) AS sent_to_bidder_as_top_match,"
            + " SUM(received_from_bidder_invalidated) AS received_from_bidder_invalidated,"
            + " SUM(received_from_bidder) AS received_from_bidder,"
            + " SUM(sent_to_client) AS sent_to_client, SUM(sent_to_client_as_top_match) AS sent_to_client_as_top_match,"
            + " plan_data, "
            + " SUM(win_events) AS win_events, MAX(created_at) AS created_at"
            + " FROM delivery_progress_reports_summary"
            + " WHERE report_window_start_timestamp >= :startTime AND report_window_end_timestamp < :endTime"
            + " AND line_item_id IN (:lineItemIds)"
            + " GROUP BY DATE_FORMAT(report_window_start_timestamp, '%Y%m%d%H'), line_item_id"
            + " ORDER BY data_window_start_timestamp, line_item_id",
            countQuery = "SELECT COUNT(*) FROM delivery_progress_reports_summary"
                    + " WHERE report_window_start_timestamp >= :startTime AND report_window_end_timestamp < :endTime"
                    + " AND line_item_id IN (:lineItemIds)",
            nativeQuery = true)
    Page<DeliveryReportSummary> findByLineIdAndReportWindowHour(
            @Param("lineItemIds") List<String> lineItemIds,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime, Pageable pageable);

    @Modifying
    @QueryHints(value = {
            @javax.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "120000") }
    )
    @Query(value = "INSERT INTO delivery_progress_reports_summary "
            + " (report_window_start_timestamp, report_window_end_timestamp,"
            + " data_window_start_timestamp, data_window_end_timestamp,"
            + " line_item_id, ext_line_item_id, bidder_code, line_item_source,"
            + " account_auctions, domain_matched,"
            + " target_matched, target_matched_but_fcapped, target_matched_but_fcap_lookup_failed,"
            + " pacing_deferred, sent_to_bidder, sent_to_bidder_as_top_match,"
            + " received_from_bidder_invalidated, received_from_bidder,"
            + " sent_to_client, sent_to_client_as_top_match, plan_data, win_events) "
            + "SELECT MIN(report_timestamp) AS report_window_start_timestamp,"
            + " MAX(report_timestamp) AS report_window_end_timestamp,"
            + " MIN(data_window_start_timestamp) AS data_window_start_timestamp,"
            + " MAX(data_window_end_timestamp) AS data_window_end_timestamp,"
            + " line_item_id, ext_line_item_id, bidder_code,"
            + " TRIM(BOTH '\"' FROM line_item_status->\"$.lineItemSource\") AS line_item_source,"
            + " COALESCE(SUM(line_item_status->\"$.accountAuctions\"), 0) AS account_auctions,"
            + " COALESCE(SUM(line_item_status->\"$.domainMatched\"), 0) AS domain_matched, "
            + " COALESCE(SUM(line_item_status->\"$.targetMatched\"), 0) AS target_matched,"
            + " COALESCE(SUM(line_item_status->\"$.targetMatchedButFcapped\"), 0) AS target_matched_but_fcapped,"
            + " COALESCE(SUM(line_item_status->\"$.targetMatchedButFcapLookupFailed\"), 0)"
            + " AS target_matched_but_fcap_lookup_failed,"
            + " COALESCE(SUM(line_item_status->\"$.pacingDeferred\"), 0) AS pacing_deferred,"
            + " COALESCE(SUM(line_item_status->\"$.sentToBidder\"), 0) AS sent_to_bidder,"
            + " COALESCE(SUM(line_item_status->\"$.sentToBidderAsTopMatch\"), 0) AS sent_to_bidder_as_top_match,"
            + " COALESCE(SUM(line_item_status->\"$.receivedFromBidderInvalidated\"), 0)"
            + " AS received_from_bidder_invalidated,"
            + " COALESCE(SUM(line_item_status->\"$.receivedFromBidder\"), 0) AS received_from_bidder,"
            + " COALESCE(SUM(line_item_status->\"$.sentToClient\"), 0) AS sent_to_client,"
            + " COALESCE(SUM(line_item_status->\"$.sentToClientAsTopMatch\"), 0) AS sent_to_client_as_top_match,"
            + " CONCAT("
            + "     TRIM(BOTH '\"' FROM COALESCE(line_item_status->\"$.deliverySchedule[0].planId\", '')), \",\","
            + "     COALESCE(sum(line_item_status->\"$.deliverySchedule[0].tokens[0].spent\"),0),\",\","
            + "     TRIM(BOTH '\"' FROM COALESCE(line_item_status->\"$.deliverySchedule[1].planId\", '')), \",\","
            + "     COALESCE(sum(line_item_status->\"$.deliverySchedule[0].tokens[0].spent\"), 0)"
            + " ),"
            + " COALESCE(SUM(line_item_status->\"$.events[0].count\"), 0) AS win_events"
            + " FROM delivery_progress_reports"
            + " WHERE report_timestamp >= :startTime AND report_timestamp < :endTime"
            + " GROUP BY line_item_id", nativeQuery = true)
    int insertLineSummariesDirectly(@Param("startTime") Timestamp startTime, @Param("endTime") Timestamp endTime);

    @Modifying
    @Query(value = "DELETE FROM delivery_progress_reports_summary"
            + " WHERE report_window_start_timestamp >= :startTime AND report_window_end_timestamp < :endTime",
            nativeQuery = true)
    @QueryHints(value = @javax.persistence.QueryHint(name = "javax.persistence.query.timeout", value = "120000"))
    int deleteLineSummaries(@Param("startTime") Timestamp startTime, @Param("endTime") Timestamp endTime);

    @Query(value = "SELECT COUNT(*) from delivery_progress_reports_summary "
            + "WHERE report_window_start_timestamp >= :startTime AND report_window_end_timestamp < :endTime",
            nativeQuery = true)
    int countByReportWindow(@Param("startTime") Timestamp startTime, @Param("endTime") Timestamp endTime);

}


