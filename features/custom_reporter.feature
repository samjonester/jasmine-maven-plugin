Feature: Support custom reporter

  In order to support advanced reporting
  I want the maven build to allow a custom reporter
  So that values can be stored in tests, and used later by the reporter

  Scenario: project with javascript using a custom reporter

    Given I am currently in the "jasmine-webapp-custom-reporter" project
    When I run "mvn clean test"
    Then the build should succeed
    And I should see "Hello World"
