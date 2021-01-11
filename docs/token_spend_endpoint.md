# Token Spend Endpoint

## Get Token Spend

Allows the General Planner to gain insight into how tokens are being spent by PBS instances based on the delivery reports sent.

### `GET /del-stats/api/v1/report/token-spend`

#### Query parameters

| Parameter | Format | Required? | Description |
| --- | --- | --- | --- |
| since | date time | no | Start of time range when token spend calculations performed (see notes about time range below)
| vendor | string | no | Filter reports that match vendor, if provided. |
| region | string | no | Filter reports that match region, if provided. |

The start and end time range used for to determine which token spend reports to send is determined by the value of the _since_ parameter.
If _since_ is not provided, a default value is determined based on the following property:

```yaml
api:
  get-token-spend-report:
    start-since-period-sec: 180
    end-since-period-sec: 0
```

If not provided, the start time is calculated by subtracting _start-since-period-sec_ seconds from current system time.

The value of end time is always calculated by subtracting _end-since-period-sec_ seconds from the current system time.

#### Expected Response

Delivery Stats will respond with token spend reports calculated in the given time range (the default being previous
3 mins) inclusive of the start time but excluding the end time. 

[get token spend response sample](samples/get_token_spend_response_sample.json).
