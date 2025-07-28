Feature: User Authentication
  As a mobile application user
  I want to be able to authenticate securely
  So that I can access my account and protected features

  Background:
    Given the mobile application is installed
    And the user is on the login screen

  @smoke @regression
  Scenario: Successful login with valid credentials
    When the user enters "valid@example.com" in the email field
    And the user enters "Valid1Password!" in the password field
    And the user taps on the login button
    Then the user should be successfully logged in
    And the home screen should be displayed
    And the user profile should be loaded correctly

  @regression
  Scenario Outline: Failed login with invalid credentials
    When the user enters "<email>" in the email field
    And the user enters "<password>" in the password field
    And the user taps on the login button
    Then the login should fail
    And an error message "<error_message>" should be displayed

    Examples:
      | email               | password          | error_message                       |
      | invalid@example.com | Valid1Password!   | Invalid email or password.          |
      | valid@example.com   | WrongPassword123! | Invalid email or password.          |
      | not-an-email        | Valid1Password!   | Please enter a valid email address. |
      |                     | Valid1Password!   | Email is required.                  |
      | valid@example.com   |                   | Password is required.               |

  @security
  Scenario: Account lockout after multiple failed login attempts
    Given the account "locktest@example.com" has 0 failed login attempts
    When the user enters "locktest@example.com" in the email field
    And the user enters "WrongPassword1" in the password field
    And the user taps on the login button 5 times
    Then the account should be temporarily locked
    And a lockout message should be displayed
    And the lockout duration should be at least 30 minutes

  @regression
  Scenario: Password recovery through email
    When the user taps on the "Forgot Password" link
    And the user enters "valid@example.com" for password recovery
    And the user submits the password recovery request
    Then a password reset email should be sent
    And the user should be informed to check their email
    And the reset link in the email should be valid

  @security @regression
  Scenario: Session timeout after inactivity
    Given the user is logged in with "valid@example.com"
    When the application is left idle for the configured timeout period
    Then the user session should expire
    And the login screen should be displayed when returning to the app
    And stored credentials should not be accessible

  @accessibility
  Scenario: Login with biometric authentication
    Given biometric authentication is enabled on the device
    And the user has previously enabled biometric login
    When the user taps on the biometric login option
    Then the biometric prompt should be displayed
    And upon successful biometric verification, the user should be logged in

  @performance
  Scenario: Login response time within acceptable range
    When the user enters "valid@example.com" in the email field
    And the user enters "Valid1Password!" in the password field
    And the user taps on the login button
    Then the system should respond within 3 seconds
    And performance metrics should be recorded
