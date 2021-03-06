#!/usr/bin/env bash
curl -X POST \
  http://localhost:8080/del-stats/api/v1/report/delivery \
  -H 'Accept: */*' \
  -H 'Accept-Encoding: gzip, deflate' \
  -H 'Authorization: Basic dXNlcjE6cGFzc3dvcmQx' \
  -H 'Cache-Control: no-cache' \
  -H 'Connection: keep-alive' \
  -H 'Content-Length: 5918' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=CE5D06C1E3980AE344FCABA22D5BC319' \
  -H 'Host: localhost:8080' \
  -H 'Postman-Token: 5190ef87-f7f0-47ae-9187-f205ae0156d5,9623793d-d8e0-4d39-ace5-e4ca9ea2c919' \
  -H 'User-Agent: PostmanRuntime/7.19.0' \
  -H 'cache-control: no-cache' \
  -H 'pg-algotest-pbs-stats-report-time: 2010-01-01T00:01:00.000Z,2010-01-01T00:01:00.000Z' \
  -H 'pg-trx-id: 16ce62b8-f0b2-4078-b50e-5cfc4608215e' \
  -d '{
  "reportId":"f6798b66-1bd5-4270-94b7-00000000aaa1",
  "reportTimeStamp":"2010-01-01T00:01:00.000Z",
  "dataWindowStartTimeStamp":"2010-01-01T00:00:00.000Z",
  "dataWindowEndTimeStamp":"2010-01-01T00:00:59.999Z",
  "vendor":"vendor",
  "region": "us-east",
  "instanceId": "fhbp-pbs0000.iad3.fanops.net",
  "clientAuctions":2000,
  "lineItemStatus":[
    {
      "lineItemSource":"bidder",
      "lineItemId":"bidderPG-1111a1",
      "extLineItemId":"1111a1",
      "accountAuctions":400,
      "domainMatched":370,
      "targetMatched":302,
      "targetMatchedButFcapped":1,
      "targetMatchedButFcapLookupFailed":0,
      "sentToBidder":302,
      "sentToBidderAsTopMatch":260,
      "receivedFromBidder":302,
      "receivedFromBidderInvalidated":1,
      "sentToClient":302,
      "sentToClientAsTopMatch":250,
      "lostToLineItems":[
        {
          "lineItemSource":"bidder",
          "lineItemId":"5555",
          "count":25
        },
        {
          "lineItemSource":"otherbidder",
          "lineItemId":"7777",
          "count":33
        }
      ],
      "events":[
        {
          "type":"win",
          "count":20
        }
      ],
      "deliverySchedule":[
        {
          "relativePriority":5,
          "planStartTimeStamp":"2010-01-01T00:00:00.000Z",
          "planExpirationTimeStamp":"2010-01-01T00:59:59.999Z",
          "planUpdatedTimeStamp":"2010-01-01T00:00:00.000Z",
          "planId": "plan9",
          "tokens":[
            {
              "class":1,
              "total":5000,
              "spent":190
            },
            {
              "class":2,
              "total":500,
              "spent":28
            }
          ]
        },
        {
          "relativePriority":6,
          "planStartTimeStamp":"2010-01-01T00:00:00.000Z",
          "planExpirationTimeStamp":"2010-01-01T00:59:59.999Z",
          "planUpdatedTimeStamp":"2010-01-01T00:00:00.000Z",
          "tokens":[
            {
              "class":1,
              "total":2500,
              "spent":500
            },
            {
              "class":2,
              "total":50,
              "spent":0
            }
          ]
        }
      ]
    },
    {
      "lineItemSource":"bidder",
      "lineItemId":"bidderPG-1112a1",
      "extLineItemId":"1112a1",
      "accountAuctions":400,
      "domainMatched":370,
      "targetMatched":302,
      "targetMatchedButFcapped":1,
      "targetMatchedButFcapLookupFailed":0,
      "sentToBidder":302,
      "sentToBidderAsTopMatch":260,
      "receivedFromBidder":302,
      "receivedFromBidderInvalidated":1,
      "sentToClient":302,
      "sentToClientAsTopMatch":250,
      "lostToLineItems":[
        {
          "lineItemSource":"bidder",
          "lineItemId":"5555",
          "count":25
        },
        {
          "lineItemSource":"otherbidder",
          "lineItemId":"7777",
          "count":33
        }
      ],
      "events":[
        {
          "type":"win",
          "count":20
        }
      ],
      "deliverySchedule":[
        {
          "relativePriority":5,
          "planStartTimeStamp":"2010-01-01T00:00:00.000Z",
          "planExpirationTimeStamp":"2010-01-01T00:59:59.999Z",
          "planUpdatedTimeStamp":"2010-01-01T00:00:00.000Z",
          "planId": "plan9",
          "tokens":[
            {
              "class":1,
              "total":5000,
              "spent":190
            },
            {
              "class":2,
              "total":500,
              "spent":28
            }
          ]
        },
        {
          "relativePriority":6,
          "planStartTimeStamp":"2010-01-01T00:00:00.000Z",
          "planExpirationTimeStamp":"2010-01-01T00:59:59.999Z",
          "planUpdatedTimeStamp":"2010-01-01T00:00:00.000Z",
          "tokens":[
            {
              "class":1,
              "total":2500,
              "spent":250
            },
            {
              "class":2,
              "total":50,
              "spent":40
            }
          ]
        }
      ]
    },
    {
      "lineItemSource":"bidder",
      "lineItemId":"bidderPG-1113a1",
      "extLineItemId":"1113a1",
      "accountAuctions":400,
      "domainMatched":370,
      "targetMatched":302,
      "targetMatchedButFcapped":1,
      "targetMatchedButFcapLookupFailed":0,
      "sentToBidder":302,
      "sentToBidderAsTopMatch":260,
      "receivedFromBidder":302,
      "receivedFromBidderInvalidated":1,
      "sentToClient":302,
      "sentToClientAsTopMatch":250,
      "lostToLineItems":[
        {
          "lineItemSource":"bidder",
          "lineItemId":"5555",
          "count":25
        },
        {
          "lineItemSource":"otherbidder",
          "lineItemId":"7777",
          "count":33
        }
      ],
      "events":[
        {
          "type":"win",
          "count":20
        }
      ],
      "deliverySchedule":[
        {
          "relativePriority":5,
          "planStartTimeStamp":"2010-01-01T00:00:00.000Z",
          "planExpirationTimeStamp":"2010-01-01T00:59:59.999Z",
          "planUpdatedTimeStamp":"2010-01-01T00:00:00.000Z",
          "planId": "plan9",
          "tokens":[
            {
              "class":1,
              "total":5000,
              "spent":190
            },
            {
              "class":2,
              "total":500,
              "spent":28
            }
          ]
        },
        {
          "relativePriority":6,
          "planStartTimeStamp":"2010-01-01T00:00:00.000Z",
          "planExpirationTimeStamp":"2010-01-01T00:59:59.999Z",
          "planUpdatedTimeStamp":"2010-01-01T00:00:00.000Z",
          "tokens":[
            {
              "class":1,
              "total":2500,
              "spent":1000
            },
            {
              "class":2,
              "total":50,
              "spent":75
            }
          ]
        }
      ]
    }
  ]
}
'