-- stats.delivery_progress_reports definition

CREATE TABLE `delivery_progress_reports` (
  `vendor` varchar(32) NOT NULL,
  `region` varchar(32) NOT NULL,
  `instance_id` varchar(64) NOT NULL,
  `bidder_code` varchar(32) NOT NULL,
  `line_item_id` varchar(64) NOT NULL,
  `ext_line_item_id` varchar(64) NOT NULL,
  `data_window_start_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data_window_end_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `report_id` varchar(36) NOT NULL,
  `report_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `client_auctions` int(11) NOT NULL DEFAULT '0',
  `line_item_status` json NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`report_id`,`line_item_id`),
KEY `delivery_progress_reports_report_timestamp` (`report_timestamp`),
KEY `data_window_end_timestamp_idx` (`data_window_end_timestamp`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- stats.latest_token_spend_summary definition

CREATE TABLE IF NOT EXISTS `latest_token_spend_summary` (
  `instance_id` varchar(64) NOT NULL,
  `vendor` varchar(32) NOT NULL,
  `region` varchar(32) NOT NULL,
  `bidder_code` varchar(32) NOT NULL,
  `line_item_id` varchar(64) NOT NULL,
  `ext_line_item_id` varchar(64) NOT NULL,
  `data_window_start_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data_window_end_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `report_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `service_instance_id` varchar(128) NOT NULL,
  `summary_data` json NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`instance_id`,`vendor`,`region`,`bidder_code`,`line_item_id`),
KEY `latest_token_spend_summary_report_timestamp_idx` (`report_timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- stats.system_state definition

CREATE TABLE IF NOT EXISTS `system_state` (
  `tag` varchar(64) NOT NULL,
  `val` varchar(1024) NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- stats.delivery_progress_reports_summary definition

CREATE TABLE IF NOT EXISTS `delivery_progress_reports_summary` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `report_window_start_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `report_window_end_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data_window_start_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data_window_end_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `line_item_id` varchar(64) NOT NULL,
  `ext_line_item_id` varchar(64) NOT NULL,
  `bidder_code` varchar(32) NOT NULL,
  `line_item_source` varchar(32) NOT NULL,
  `account_auctions` mediumint(9) NOT NULL DEFAULT '0',
  `domain_matched` mediumint(9) NOT NULL DEFAULT '0',
  `target_matched` mediumint(9) NOT NULL DEFAULT '0',
  `target_matched_but_fcapped` mediumint(9) NOT NULL DEFAULT '0',
  `target_matched_but_fcap_lookup_failed` mediumint(9) NOT NULL DEFAULT '0',
  `pacing_deferred` mediumint(9) NOT NULL DEFAULT '0',
  `sent_to_bidder` mediumint(9) NOT NULL DEFAULT '0',
  `sent_to_bidder_as_top_match` mediumint(9) NOT NULL DEFAULT '0',
  `received_from_bidder_invalidated` mediumint(9) NOT NULL DEFAULT '0',
  `received_from_bidder` mediumint(9) NOT NULL DEFAULT '0',
  `sent_to_client` mediumint(9) NOT NULL DEFAULT '0',
  `sent_to_client_as_top_match` mediumint(9) NOT NULL DEFAULT '0',
  `plan_data` varchar(512) DEFAULT NULL,
  `win_events` mediumint(9) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
KEY `delivery_progress_reports_summaryreport_timestamp` (`created_at`),
KEY `delivery_progress_reports_summaryreport_window` (`report_window_start_timestamp`,`report_window_end_timestamp`) USING BTREE,
KEY `delivery_progress_reports_summaryreport_window_line` (`report_window_start_timestamp`,`report_window_end_timestamp`,`line_item_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=240637 DEFAULT CHARSET=utf8mb4;