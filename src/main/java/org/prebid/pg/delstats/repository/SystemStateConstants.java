package org.prebid.pg.delstats.repository;

public class SystemStateConstants {

    public static final String SYSTEM_STATE_TABLE_NAME = "system_state";

    public static final String SYSTEM_STATE_KEY_COLUMN = "tag";

    public static final String SYSTEM_STATE_READ_COLUMNS = SYSTEM_STATE_KEY_COLUMN + ", val, updated_at";

    public static final String SYSTEM_STATE_WRITE_COLUMNS = SYSTEM_STATE_KEY_COLUMN + ", val";

    public static final String SYSTEM_STATE_WRITE_COLUMN_PARAMS =
            ":tag, :val";

    public static final String SYSTEM_STATE_STORE_SQL = "REPLACE INTO " + SYSTEM_STATE_TABLE_NAME + " ("
            +
            SYSTEM_STATE_WRITE_COLUMNS
            +
            ") VALUES ("
            +
            SYSTEM_STATE_WRITE_COLUMN_PARAMS
            +
            ")";

    public static final String SYSTEM_STATE_RETRIEVE_BY_TAG_SQL = "SELECT "
            +
            SYSTEM_STATE_READ_COLUMNS
            +
            " FROM " + SYSTEM_STATE_TABLE_NAME + " WHERE tag = :tag";

    public static final String SYSTEM_STATE_TAG_LAST_SUMMARY_REPORT =
            "latest_token_summary_data_window_end_timestamp_str";

    public static final String SYSTEM_STATE_TAG_DELIVERY_SUMMARY =
            "latest_delivery_summary_interval_end_timestamp_str";

    public static final String SYSTEM_STATE_TAG_DELIVERY_SUMMARY_INTERVAL_END_FORMAT =
            "latest_delivery_summary_interval_end_%s_timestamp_str";

    public static final String SYSTEM_STATE_TAG_DELIVERY_SUMMARY_RECREATE_INTERVAL_END_FORMAT =
            "latest_delivery_summary_recreate_interval_end_%s_str";

    public static final String SYSTEM_STATE_TAG_SUMMARY_REPORT_SUFFIX =
            "summary_report_window_end_timestamp_str";

    public static final String SYSTEM_STATE_TAG_SUMMARY_REPORT_RECREATE_SUFFIX =
            "summary_report_recreate_window_end_str";

    public static final String SAVE_DELIVERY_SUMMARY_REPORT_STATES_SQL =
            "REPLACE INTO system_state(tag, val) "
            + "SELECT CONCAT_WS('-', :serviceInstanceId, bidder_code, ext_line_item_id, :tagSuffix), :endTimeStr "
            + "FROM delivery_progress_reports "
            + "WHERE report_timestamp >= :startTime AND report_timestamp < :endTime "
            + "GROUP BY bidder_code, ext_line_item_id";

    public static final String GET_LATEST_DELIVERY_SUMMARY_REPORT_STATES_SQL =
            "SELECT " + SYSTEM_STATE_READ_COLUMNS
            + " FROM " + SYSTEM_STATE_TABLE_NAME
            + " WHERE updated_at > :since"
            + " AND tag LIKE 'latest_delivery_summary_interval_end_%_timestamp_str'"
            + " ORDER BY updated_at desc";

    private SystemStateConstants() { }
}
