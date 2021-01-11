# Delivery Stats Endpoints
## Primary Feature Endpoints
- [Delivery Report Endpoints](delivery_report_endpoints.md)
- [Token Spend Endpoint](token_spend_endpoint.md)
 
## Authentication
Delivery Stats provides a number of endpoints, each of which are protected with HTTP Basic Authentication.
Any request which does not provide the appropriate authorization will receive an HTTP 401 response code.

The endpoints are each assigned to a role based on the service which is intended to use that endpoint
(Prebid Servers - pbs, Planning Adapter - pa, General Planner - ga, Reporting and Monitoring systems - readOnly). 
The default set of roles, users and passwords is defined in the application.yaml. The admin role
has access to all endpoints.

```yaml
server-api-roles:
  post-delivery-report: pbs
  get-delivery-report: pa
  get-token-spend-report: gp
  get-line-item-summary: readyOnly
  get-delivery-summary-freshness: readOnly

server-auth:
  enabled: true
  realm: pg-del-stats-svc
  principals:
    - username: user1
      password: password1
      roles: pbs
    - username: user2
      password: password2
      roles: pa, bidderPG
    - username: user3
      password: password3
      roles: gp
    - username: admin
      password: admin
      roles: admin
    - username: readOnly
      password:
      roles: readOnly
```

## _Endpoint Configuration_

Additionally, endpoints can be enabled or disabled through configuration. This is useful if you wish to run
multiple pools of Delivery Stat instances. For example, you may have a pool of Del Stats services for each region or 
for different roles. An example property for enabling an endpoint is shown below:

```yaml
api:
  get-delivery-report:
    enabled: true
```

Many endpoints provide settings to control default values for optional parameters. An example is shown below.

```yaml
api:
  get-delivery-report:
    enabled: true
    start-since-period-sec: 300
    end-since-period-sec: 0
```

Endpoints reside beneath the base URL defined in the application.yml property. The default value below
is used in this documentation.

```yaml
services:
  base-url: /del-stats/api
````
