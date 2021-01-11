# Line Item Summary Endpoint

## Get Line Item Summaries

For reporting metrics on line items derived from Delivery Reports.

### `GET /del-stats/api/v1/report/line-item-summary`

#### Query parameters

| Parameter | Format | Required? | Description |
| --- | --- | --- | --- |
| start | date time | no | Start of time range used to retrieve line item summaries (see notes about time range below)
| lineItemIds | string | no | Comma separated list of line items for filtering results if provided. |
| metrics | string | no | Comma separated list of metric names for filtering results if provided (see [Metrics](#metrics) below). |

The start and end time range used for to determine which token spend reports to send is determined by the value of the _since_ parameter.
If _since_ is not provided, a default value is determined based on the following property:

```yaml
api:
  get-line-item-summary:
    start-since-period-sec: 900
    end-since-period-sec: 0
    max-time-range-sec: 345600
```

If not provided, the start time is calculated by subtracting _start-since-period-sec_ seconds from current system time.

The value of end time is always calculated by subtracting _end-since-period-sec_ seconds from the current system time.

Additionally, if the difference between the start and end times is greater than _max-time-range-sec_ seconds then the end time
is set to that many seconds after the start.

#### Metrics

The following metrics are provided by this end point. The metric names below can be provided as a parameter
to the request to retrieve only the named list of metrics if desired.

- accountAuctions
- domainMatched
- pacingDeferred
- receivedFromBidder
- receivedFromBidderInvalidated
- sentToBidder
- sentToBidderAsTopMatch
- sentToClient
- sentToClientAsTopMatch
- targetMatched
- targetMatchedButFcapped
- targetMatchedButFcapLookupFailed
- winEvents


#### Expected Response

Delivery Stats will respond with token spend reports calculated in the given time range (the default being previous
3 mins) inclusive of the start time but excluding the end time. 

[get token spend response sample](samples/get_line_item_summary_sample.txt).
