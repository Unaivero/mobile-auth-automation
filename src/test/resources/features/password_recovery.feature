Feature: Password Recovery
  As a user who has forgotten their password
  I want to be able to reset my password securely
  So that I can regain access to my account

  Background:
    Given the mobile application is launched
    And I am on the login screen

  Scenario: Complete password recovery flow
    When I tap on the forgot password link
    And I enter my email address
    And I tap on the send recovery email button
    Then I should see a confirmation message that an email was sent
    When I retrieve the password reset token from my email
    And I enter the reset token
    And I verify the token
    And I enter a new password "NewSecure@Password123"
    And I tap on the reset password button
    Then I should be redirected to the login screen
    When I enter my email address and new password "NewSecure@Password123"
    And I tap on the login button
    Then I should be successfully logged in
    And I should see the dashboard screen

  Scenario Outline: Validation of invalid reset tokens
    When I tap on the forgot password link
    And I enter my email address
    And I tap on the send recovery email button
    And I enter invalid token "<token>"
    And I verify the token
    Then I should see an error message containing "<error_message>"

    Examples:
      | token   | error_message           |
      | 000000  | Invalid or expired token |
      | 123     | Token must be 6 digits   |
      | abcdef  | Token must contain only digits |

  Scenario: Token expiration validation
    When I tap on the forgot password link
    And I enter my email address
    And I tap on the send recovery email button
    And I enter an expired token "999999"
    And I verify the token
    Then I should see an error message containing "expired"
