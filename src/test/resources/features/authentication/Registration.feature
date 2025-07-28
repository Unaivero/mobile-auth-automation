Feature: User Registration
  As a new mobile application user
  I want to register for an account securely
  So that I can access protected features and personalize my experience

  Background:
    Given the mobile application is installed
    And the user is on the registration screen

  @smoke @regression
  Scenario: Successful registration with valid information
    When the user enters "newuser@example.com" in the email field
    And the user enters "Secure1Password!" in the password field
    And the user enters "Secure1Password!" in the confirm password field
    And the user enters "John" in the first name field
    And the user enters "Doe" in the last name field
    And the user accepts the terms and conditions
    And the user taps on the register button
    Then the registration should be successful
    And a verification email should be sent
    And the user should be directed to the verification pending screen

  @regression
  Scenario Outline: Failed registration with invalid information
    When the user enters "<email>" in the email field
    And the user enters "<password>" in the password field
    And the user enters "<confirm>" in the confirm password field
    And the user enters "<first_name>" in the first name field
    And the user enters "<last_name>" in the last name field
    And the user <terms_action> the terms and conditions
    And the user taps on the register button
    Then the registration should fail
    And an error message "<error_message>" should be displayed

    Examples:
      | email              | password         | confirm          | first_name | last_name | terms_action | error_message                                       |
      | existing@example.com | Secure1Password! | Secure1Password! | John       | Doe       | accepts      | Email address is already in use.                    |
      | not-an-email       | Secure1Password! | Secure1Password! | John       | Doe       | accepts      | Please enter a valid email address.                 |
      | newuser@example.com | short            | short            | John       | Doe       | accepts      | Password must be at least 8 characters.            |
      | newuser@example.com | Secure1Password! | DifferentPass!   | John       | Doe       | accepts      | Passwords do not match.                            |
      | newuser@example.com | Secure1Password! | Secure1Password! |            | Doe       | accepts      | First name is required.                            |
      | newuser@example.com | Secure1Password! | Secure1Password! | John       |           | accepts      | Last name is required.                             |
      | newuser@example.com | Secure1Password! | Secure1Password! | John       | Doe       | does not accept | You must accept the terms and conditions.         |

  @security
  Scenario: Password strength validation during registration
    When the user enters "newuser@example.com" in the email field
    And the user enters the following passwords
      | password            | strength    | message                                  |
      | password            | weak        | Password is too weak                     |
      | Password1           | moderate    | Password should include special characters |
      | Password1!          | strong      | Password strength is good                 |
      | Secure1Password!@#$ | very strong | Password strength is excellent            |
    Then the password strength indicator should match the expected strength

  @regression
  Scenario: Email verification required after registration
    Given the user has registered with "newuser@example.com"
    When the user tries to log in before verification
    Then access should be limited
    And the user should be prompted to verify their email
    When the user verifies their email address
    And the user logs in with "newuser@example.com" and "Secure1Password!"
    Then the login should be successful

  @security @regression
  Scenario: Registration with social media account
    When the user selects "Continue with Google" option
    Then the OAuth consent screen should be displayed
    When the user approves the OAuth request
    Then the registration should be successful
    And the user should be logged in automatically
    And the profile should be pre-filled with information from the social account

  @accessibility
  Scenario: Registration screen accessibility compliance
    When the user enables screen reader
    Then all registration form fields should be properly labeled
    And focus order should follow a logical sequence
    And error messages should be announced by the screen reader
    And all interactive elements should be reachable by keyboard/switch control

  @performance
  Scenario: Registration response time within acceptable range
    When the user completes all registration fields with valid data
    And the user taps on the register button
    Then the system should respond within 3 seconds
    And performance metrics should be recorded
