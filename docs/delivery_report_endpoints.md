# Delivery Report Endpoints

## Get Delivery Report Endpoint (V2)

Allows each PBS instance to retrieve aggregated line items summaries from Del Stats.

### `GET /del-stats/api/v2/report/delivery`

#### Query parameters

| Parameter | Format | Required? | Description |
| --- | --- | --- | --- |
| bidderCode | string | yes | Code for the bidder |
| startTime | date time | no | Start of time range when reports were received by Del Stats (see details below) |
| endTime | date time | no | Start of time range when reports were received by Del Stats (see details below) |

If startTime or endTime are not provided, default values are determined based on the following properties:

```yaml
api:
  get-delivery-report:
    start-since-period-sec: 300
    end-since-period-sec: 0
```

If not provided, the value of startTime is calculated by subtracting _start-since-period-sec_ seconds from current system time.

If not provided, the value of endTime is calculated by subtracting _end-since-period-sec_ seconds from the current system time.

#### Expected Response

Delivery Stats will respond with line items summaries received in the given time range (the default being previous
15 mins) inclusive of the start time but excluding the end time.

[get delivery report V2 response sample](samples/get_delivery_report_v2_response_sample.json).

## Get Delivery Report Endpoint (V1)

We recommend using V2 of this endpoint, as this one provides JSON that is harder to parse.

Allows Planning Adapter to retrieve Line Item Summaries from all Delivery Reports delivered in a time range
for a specific bidder code.

### `GET /del-stats/api/v1/report/delivery`

#### Query parameters

| Parameter | Format | Required? | Description |
| --- | --- | --- | --- |
| bidderCode | string | yes | Code for the bidder |
| startTime | date time | no | Start of time range when reports were received by Del Stats (see details below) |
| endTime | date time | no | Start of time range when reports were received by Del Stats (see details below) |

If startTime or endTime are not provided, default values are determined based on the following properties:

```yaml
api:
  get-delivery-report:
    start-since-period-sec: 300
    end-since-period-sec: 0
```

If not provided, the value of startTime is calculated by subtracting _start-since-period-sec_ seconds from current system time.

If not provided, the value of endTime is calculated by subtracting _end-since-period-sec_ seconds from the current system time.

#### Expected Response

Delivery Stats will respond with line items summaries received in the given time range (the default being previous
15 mins) inclusive of the start time but excluding the end time. 

[get delivery report V1 response sample](samples/get_delivery_report_v1_response_sample.json).

## Post Delivery Report Endpoint

This endpoint is how data is submitted to the service. It's intented only for use by Prebid Servers. It allows PBS instances to store Line Item Summaries.

### `POST /del-stats/api/v1/report/delivery` 

Endpoint used to store delivery reports.

#### Request payload

A sample request payload can be found [here](samples/post_delivery_report_request_sample.json).

#### Expected Response

Empty response with HTTP Status 200 (accepted and stored), or an exception message body with a return code of
 * 400 (bad request - unable to process request due to validation issues)
 * 401 (unauthorized - basic authentication failed)
 * 409 (conflict - duplicate report id)
