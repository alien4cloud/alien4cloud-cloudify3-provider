Feature: Test log dispatch to registered clients

  Scenario: Sending logs when no dispatchers are registered should not create activity
    When I POST "inputs/log.json" to "/api/v1/logs"
    And I POST "inputs/event.json" to "/api/v1/logs"
    # Wait for the flush timeout
    And I wait 10 seconds
    Then no cfy log files should be created
    When I GET "/api/v1/logs/dummy"
    Then I should receive a RestResponse with an error code 504
    # Register
    When I POST "inputs/log.json" to "/api/v1/logs"
    And I POST "inputs/event.json" to "/api/v1/logs"
    # Wait for the flush timeout
    And I wait 10 seconds
