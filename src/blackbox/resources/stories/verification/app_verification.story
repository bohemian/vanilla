Narrative:
In order to confirm a newly deployed pod is functionally ready before it receives traffic
As the deployment pipeline
I want to verify the app responds correctly to non-invasive read operations

Scenario: Application readiness endpoint reports UP
When I check the readiness endpoint
Then the health status is UP

Scenario: Health sentinel product is queryable
When I fetch product with code HEALTH
Then the response status is 200
And the response contains a product with code HEALTH
