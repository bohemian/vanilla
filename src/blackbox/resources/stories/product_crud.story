Narrative:
In order to manage the product catalogue
As an API client
I want to create, update and delete products via the REST API

Scenario: Create a new product returns 201 with location header
Given no product with code BB1 exists
When I create a product with code BB1 name Widget and price 500
Then the response status is 201
And the response contains a product with code BB1

Scenario: Creating a duplicate product returns 409
Given a product with code BB2 name Widget price 100 exists
When I create a product with code BB2 name Widget and price 100
Then the response status is 409

Scenario: Update an existing product returns 200
Given a product with code BB3 name OldWidget price 100 exists
When I update product BB3 with name NewWidget and price 999
Then the response status is 200
And the response contains a product with code BB3

Scenario: Update a non-existent product returns 404
When I update product BB404 with name Widget and price 0
Then the response status is 404

Scenario: Delete an existing product returns 204
Given a product with code BB4 name Widget price 100 exists
When I delete product BB4
Then the response status is 204

Scenario: Delete a non-existent product returns 404
When I delete product BB404DEL
Then the response status is 404
