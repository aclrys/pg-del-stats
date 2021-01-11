package org.prebid.pg.delstats.repository;

public class DeliveryProgressReportsConstants {

    public static final String DELIVERY_PROGRESS_REPORT_TABLE_NAME =
            "delivery_progress_reports";

    public static final String DELIVERY_PROGRESS_REPORT_KEY_COLUMN =
            "report_id, line_item_id";

    public static final String DELIVERY_PROGRESS_REPORT_REGION_VENDOR_COLUMNS =
            "vendor, region";

    public static final String DELIVERY_PROGRESS_REPORT_INDEXED_COLUMNS =
            DELIVERY_PROGRESS_REPORT_KEY_COLUMN + ", instance_id, " + DELIVERY_PROGRESS_REPORT_REGION_VENDOR_COLUMNS
                    + ", bidder_code, ext_line_item_id, "
                    + "data_window_start_timestamp, data_window_end_timestamp";

    public static final String DELIVERY_PROGRESS_REPORT_COLUMNS =
            DELIVERY_PROGRESS_REPORT_INDEXED_COLUMNS + ", "
                    + "report_timestamp, client_auctions, line_item_status";

    public static final String DELIVERY_PROGRESS_REPORT_TIMESTAMP_RANGE_CLAUSE =
            "report_timestamp >= :startTime AND report_timestamp < :endTime";

    public static final String DELIVERY_PROGRESS_REPORT_DATA_WINDOW_WITHIN_RANGE_CLAUSE =
            "data_window_start_timestamp <= :endTime AND data_window_end_timestamp >= :startTime";

    public static final String LINE_ITEM_REPORT_TIMESTAMP_RANGE_CLAUSE =
            "(data_window_end_timestamp >= :startTime AND data_window_end_timestamp < :endTime)";

    public static final String DELIVERY_PROGRESS_REPORT_AGGREGATION_SQL = "SELECT "
            + DELIVERY_PROGRESS_REPORT_INDEXED_COLUMNS
            + ", report_timestamp, client_auctions, line_item_status"
            + " FROM " + DELIVERY_PROGRESS_REPORT_TABLE_NAME
            + " WHERE report_timestamp > :startTime AND report_timestamp <= :endTime";

    public static final String DELIVERY_PROGRESS_REPORT_AGGREGATION_COUNT_SQL = "SELECT COUNT(*)"
            + " FROM " + DELIVERY_PROGRESS_REPORT_TABLE_NAME
            + " WHERE report_timestamp > :startTime AND report_timestamp <= :endTime";

    public static final String DELIVERY_PROGRESS_REPORT_FIND_OLD_SQL =
            "SELECT " + DELIVERY_PROGRESS_REPORT_COLUMNS
                    + " FROM " + DELIVERY_PROGRESS_REPORT_TABLE_NAME + " WHERE report_timestamp < :expired";

    public static final String DELIVERY_PROGRESS_REPORT_GET_LINES_BY_TIME_RANGE_SQL =
            "SELECT " + DELIVERY_PROGRESS_REPORT_COLUMNS
            + " FROM " + DELIVERY_PROGRESS_REPORT_TABLE_NAME
            + " WHERE " + LINE_ITEM_REPORT_TIMESTAMP_RANGE_CLAUSE;

    public static final String DELIVERY_PROGRESS_REPORT_GET_LINES_BY_TIME_RANGE_COUNT_SQL =
            "SELECT COUNT(*)"
            + " FROM " + DELIVERY_PROGRESS_REPORT_TABLE_NAME
                    + " WHERE " + LINE_ITEM_REPORT_TIMESTAMP_RANGE_CLAUSE;

    public static final String DELIVERY_PROGRESS_REPORT_GET_WITHIN_TIME_RANGE_SQL =
            "SELECT " + DELIVERY_PROGRESS_REPORT_COLUMNS
            + " FROM " + DELIVERY_PROGRESS_REPORT_TABLE_NAME
            + " WHERE " + DELIVERY_PROGRESS_REPORT_DATA_WINDOW_WITHIN_RANGE_CLAUSE;

    public static final String DELIVERY_PROGRESS_REPORT_GET_WITHIN_TIME_RANGE_COUNT_SQL =
            "SELECT COUNT(*)"
                    + " FROM " + DELIVERY_PROGRESS_REPORT_TABLE_NAME
                    + " WHERE " + DELIVERY_PROGRESS_REPORT_DATA_WINDOW_WITHIN_RANGE_CLAUSE;

    public static final String DELIVERY_PROGRESS_REPORT_GET_BY_LINES_TIME_RANGE_SQL =
            DELIVERY_PROGRESS_REPORT_GET_LINES_BY_TIME_RANGE_SQL
            + " AND line_item_id in (:lineItemIds)";

    public static final String DELIVERY_PROGRESS_REPORT_GET_BY_LINES_TIME_RANGE_COUNT_SQL =
            DELIVERY_PROGRESS_REPORT_GET_LINES_BY_TIME_RANGE_COUNT_SQL
            + " AND line_item_id in (:lineItemIds)";

    public static final String DELIVERY_PROGRESS_REPORT_GET_REGION_AND_VENDORS_BY_TIME_RANGE_SQL =
            "SELECT DISTINCT " + DELIVERY_PROGRESS_REPORT_REGION_VENDOR_COLUMNS + " FROM "
                + DELIVERY_PROGRESS_REPORT_TABLE_NAME
                + " WHERE " + DELIVERY_PROGRESS_REPORT_TIMESTAMP_RANGE_CLAUSE;

    private DeliveryProgressReportsConstants() { }

}
