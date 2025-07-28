Feature: Login Security
  As an application security tester
  I want to verify that the login security features work correctly
  So that user accounts remain protected from unauthorized access

  Background:
    Given the mobile application is launched
    And I am on the login screen

  Scenario: Successful login with valid credentials
    When I enter username "validuser@example.com" and password "Valid@Password123"
    And I tap on the login button
    Then I should be successfully logged in
    And I should see the dashboard screen

  Scenario Outline: Failed login with invalid credentials
    When I enter username "<username>" and password "<password>"
    And I tap on the login button
    Then I should see an error message "<error_message>"
    And I should remain on the login screen

    Examples:
      | username           | password       | error_message              |
      | invalid_user       | wrongPassword123 | Invalid username or password |
      | test@example.com   | password123!   | Invalid username or password |
      | admin              | admin          | Invalid username or password |

  Scenario: CAPTCHA appears after three failed login attempts
    When I attempt to login with invalid credentials 3 times
    Then I should see a CAPTCHA challenge
    When I enter the correct CAPTCHA
    And I enter valid username "validuser@example.com" and password "Valid@Password123"
    And I tap on verify CAPTCHA button
    Then I should be successfully logged in
    And I should see the dashboard screen
