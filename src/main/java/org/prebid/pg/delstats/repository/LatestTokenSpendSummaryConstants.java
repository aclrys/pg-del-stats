package org.prebid.pg.delstats.repository;

public class LatestTokenSpendSummaryConstants {
    public static final String LATEST_TOKEN_SPEND_TABLE_NAME = "latest_token_spend_summary as ltss";

    public static final String LATEST_TOKEN_SPEND_KEY_COLUMN =
            "vendor, region, instance_id, bidder_code, line_item_id, ext_line_item_id";

    public static final String LATEST_TOKEN_SPEND_INDEXED_COLUMNS = LATEST_TOKEN_SPEND_KEY_COLUMN
            + ", service_instance_id, data_window_start_timestamp, data_window_end_timestamp";

    public static final String LATEST_TOKEN_SPEND_COLUMNS = LATEST_TOKEN_SPEND_INDEXED_COLUMNS
            + ", report_timestamp, summary_data";

    public static final String LATEST_TOKEN_SPEND_RETRIEVE_SINCE_SQL = "SELECT "
            +
            LATEST_TOKEN_SPEND_COLUMNS
            +
            " FROM " + LATEST_TOKEN_SPEND_TABLE_NAME
            +
            " WHERE report_timestamp >= :report_timestamp";

    public static final String LATEST_TOKEN_SPEND_RETRIEVE_SINCE_BY_VENDOR_REGION_SQL = "SELECT "
            +
            LATEST_TOKEN_SPEND_COLUMNS
            +
            " FROM " + LATEST_TOKEN_SPEND_TABLE_NAME
            +
            " WHERE report_timestamp >= :report_timestamp AND vendor = :vendor AND region = :region";

    public static final String LATEST_TOKEN_SPEND_RETRIEVE_SINCE_COUNT_SQL = "SELECT COUNT(*)"
            +
            " FROM " + LATEST_TOKEN_SPEND_TABLE_NAME
            +
            " WHERE report_timestamp >= :report_timestamp";

    public static final String LATEST_TOKEN_SPEND_RETRIEVE_SINCE_BY_VENDOR_REGION_COUNT_SQL = "SELECT COUNT(*)"
            +
            " FROM " + LATEST_TOKEN_SPEND_TABLE_NAME
            +
            " WHERE report_timestamp >= :report_timestamp AND vendor = :vendor AND region = :region";

    public static final String LATEST_TOKEN_SPEND_FIND_OLDEST_SQL =
            "SELECT " + LATEST_TOKEN_SPEND_COLUMNS
                    + " FROM " + LATEST_TOKEN_SPEND_TABLE_NAME + " WHERE report_timestamp < :expired";

    public static final String LATEST_TOKEN_SPEND_UPSERT_TOKEN_MATCH_COUNT_SQL =
            "REPLACE INTO latest_token_spend_summary "
                    +
                    "SELECT "
                    +
                    "instance_id, vendor, region, bidder_code, line_item_id, ext_line_item_id, "
                    +
                    "data_window_start_timestamp, data_window_end_timestamp, report_timestamp, 'NA', "
                    +
                    "JSON_OBJECT('tokenSpent', JSON_ARRAY(JSON_OBJECT('pc', -10, 'class', 1)), "
                    +
                    "'targetMatched', SUM(line_item_status->\"$.targetMatched\")), "
                    +
                    "CURRENT_TIMESTAMP "
                    +
                    "FROM delivery_progress_reports "
                    +
                    "WHERE vendor = :vendor AND region = :region AND "
                    +
                    "report_timestamp >= :startTime AND report_timestamp < :endTime "
                    +
                    "GROUP BY  instance_id, bidder_code, line_item_id";

    private LatestTokenSpendSummaryConstants() { }
}
