
# Full list of application configuration options

This document describes all the configuration properties available for the PG General Planner.

## HTTP
- `server.port` - HTTP listener port
- `server.compression.*` - HTTP compression settings

## Deployment
- `deployment.profile` - could be `dev`, `test`, `prod`, or any other deployment environment
- `deployment.infra` - if set to `ecs`, system can be deployed on a managed single instance service task in AWS Use 'vm' if deployed on bare metal/virtual machine/EC2 instance.
- `deployment.data-center` - identifier to distinguish among AWS, on-premise or any other data center provider
- `deployment.region` - identifier for data center region
- `deployment.system` - overall system identifier `PG`
- `deployment.sub-system` - sub-system identifier `General Planner` in `PG` system

## Database
- `spring.datasource.url` - JDBC-based URL to database (which includes host, port and database name)
- `spring.datasource.username` - application user for above schema  
- `spring.datasource.password` - password for application user - should be supplied externally in protected fashion 
- `spring.datasource.hikari.*` - Hikari Connection Pool configs
- `spring.jpa.properties.hibernate.*` - Hibernate configuration settings

## API Settings
- `api.get-delivery-report.enabled` - enables or disables API for getting Delivery Reports
- `api.get-delivery-report.start-since-period-sec` - number of seconds before 'now' if start time not provided in request
- `api.get-delivery-report.end-since-period-sec` - number of seconds before 'now' if end time not provided in request
- `api.get-token-spend-report.enabled` - enabled or disables API for getting Token Spend Reports
- `api.get-token-spend-report.start-since-period-sec` - number of seconds before 'now' if start time not provided in request
- `api.get-token-spend-report.end-since-period-sec` - number of seconds before 'now' if end time not provided in request
- `api.get-line-item-summary.enabled` - enables or disables API for getting Line Item Summary Reports
- `api.get-line-item-summary.start-since-period-sec` - number of seconds before 'now' if start time not provided in request
- `api.get-line-item-summary.end-since-period-sec` - number of seconds before 'now' if end time not provided in request
- `api.get-line-item-summary.max-time-range-sec` - maximum number of seconds allowed between start and end times.`
- `api.delivery-summary-freshness.enabled` - enables or disables API for getting Delivery Summary Freshness Reports
- `api.recreate-line-item-summary.enabled` - enabled or disables API for endpoint to recreate Line Item Summary Reports
- `api.recreate-line-item-summary.max-look-back-in-days` - maximum number of days back that Line Item Summary can be recreated


## Services
- `services.base-url` - base service request mapping URL path
- `services.admin-base-url` - base service request mapping URL path for admin services
- `services.validation.enabled` - enables or disables Delivery Report validation
- `services.cors.*` - CORS settings (see [Spring Boot Actuator Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html#actuator-properties))
- `services.line-item-biddder-code-separator` - line item in Delivery Reports consists of two parts: bidder code and id. The value in this property is expected to separate the two parts.
- `services.delivery-report.bidder-alias-mappings` - a comma separated list of mappings in the form of 'A:B' which will map a bidderCode value of 'A' to 'B'
- `services.delivery-report.instance-name-cache.*` - settings for a cache of PBS instances name that have provided reports; current size of cache is reported as a gauge metric.
- `services.delivery-summary.enabled` - enables or disabled scheduled Delivery Report summarization
- `services.delivery-summary.cron` - a cron tab entry defining the schedule for Delivery Report summarization
- `services.delivery-summary.aggregate-interval-minute` - number of minutes to aggregate together for a summary
- `services.delivery-summary.max-aggregate-intervals` - when summarization is trying to catch up (due to service being down), this limits the number of aggregation intervals to be performed.
- `services.delivery-summary.max-summary-intervals` - maximum number of summary reports intervals to return from GET endpoint.
- `services.token-aggr.enabled` - enabled or disables scheduled Token Aggregation
- `services.token-aggr.initial-delay-sec` - seconds to wait after application startup before running scheduled Token Aggregation
- `services.token-aggr.refresh-period-sec` - seconds between scheduled Token Aggregations
- `services.token-aggr.max-look-back-sec` - normally, Token Aggregation keeps track of previous time it ran; if something goes wrong, it will use this setting to determine how far back consider Delivery Reports for Token Aggregation calculations.
- `services.token-aggr.*-field-name` - field name definitions used to determining token aggregation calculations


## Alert Proxy 
- `services.alert-proxy.enabled` - boolean flag to enable this service
- `services.alert-proxy.url` - the Alert Proxy URL
- `services.alert-proxy.timeout-sec` - timeout in seconds in call to Alert Proxy URL
- `services.alert-proxy.url` - the Alert Proxy URL
- `services.alert-proxy.username` - HTTP Basic Auth user to access the Alert Proxy
- `services.alert-proxy.password` - HTTP Basic Auth password to access the Alert Proxy
- `services.alert-proxy.policies[0].alert-name` - default alert throttle policy
- `services.alert-proxy.policies[0].initial-alerts` - the number of alerts to send without throttling
- `services.alert-proxy.policies[0].alert-frequency` - alert throttling level. Send Nth alerts.

## Server Authentication
- `server-auth.authentication-enabled` - boolean flag to enable authentication
- `server-auth.principals[0].username` - username
- `server-auth.principals[0].password` - password
- `server-auth.principals[0].roles` - comma separated roles assigned to this user
- `server-api-roles.post-delivery-report` - role allowing PBS Delivery Reports to be posted
- `server-api-roles.get-delivery-repor` - role allowing Planning Adapters to fetch Delivery Reports
- `server-api-roles.get-token-spend-report` - role allowing General Planners to fetch Token Summaries
- `server-api-roles.get-line-item-summary` - role allowing fetching of Line Item Summaries
- `server-api-roles.get-delivery-summary-freshness` - role allowing fetching of Delivery Summary Freshness Reports


## Metrics
- `metrics.graphite.enabled` - boolean flag to enable publishing metrics to Graphite
- `metrics.graphite.prefix` - prefix to classify metrics source
- `metrics.graphite.host` - target graphite host
- `metrics.graphite.port` - target graphite port
- `metrics.graphite.interval` - interval in seconds to publish metrics

## Admin Interface
- `admin.apps` - comma separated list of applications recognizing admin events
- `admin.db-store-batch-size` - batch size to store admin commands in database
- `admin.trace.max-duration-in-seconds` - maximum duration in seconds for which targetted trace can be turned on
 
