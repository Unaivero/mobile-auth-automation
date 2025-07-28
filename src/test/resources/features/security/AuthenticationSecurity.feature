Feature: Authentication Security
  As a security analyst
  I want to ensure that mobile authentication mechanisms are secure
  So that user accounts are protected from unauthorized access

  Background:
    Given the authentication API is available
    And test user accounts are configured

  @security @authentication @severity-critical
  Scenario: Account lockout after multiple failed login attempts
    Given a valid username with incorrect password
    When the user attempts to login 5 times with incorrect credentials
    Then the account should be temporarily locked
    And further login attempts should be rejected
    And an appropriate error message should be shown

  @security @authentication @severity-high
  Scenario: Password strength validation
    Given a user is registering a new account
    When the user submits passwords with different strength levels:
      | password          | expected_result |
      | password          | reject          |
      | Password1         | reject          |
      | Password123!      | accept          |
      | P@ssw0rd2023!     | accept          |
      | abcABC123!@#      | accept          |
    Then only strong passwords should be accepted
    And appropriate strength feedback should be provided

  @security @authentication @severity-critical
  Scenario: Brute force protection with rate limiting
    Given a valid username
    When login attempts are made repeatedly in quick succession
    Then rate limiting should be applied
    And suspicious activity should be logged
    And the user should be notified of unusual login activity

  @security @authentication @severity-high
  Scenario: Secure password reset flow
    Given a registered user account
    When the user requests a password reset
    Then a secure time-limited token should be generated
    And the token should be delivered via a secure channel
    And the password reset should require additional verification

  @security @authentication @severity-critical
  Scenario: Prevention of username enumeration
    Given a non-existent user account
    When authentication is attempted with invalid credentials
    Then response time should be similar to valid login attempts
    And the error message should not reveal account existence
    And the same process flow should be followed for valid and invalid accounts

  @security @authentication @severity-high
  Scenario: 2FA enforcement for sensitive operations
    Given a user is authenticated with the mobile app
    When the user attempts to perform a sensitive operation
    Then additional authentication factor should be required
    And the operation should be denied without completing 2FA
    And 2FA bypass attempts should be prevented

  @security @authentication @severity-medium
  Scenario: Authentication event logging and audit trail
    Given a user performs authentication actions
    When examining system logs
    Then all authentication events should be properly logged
    And logs should include relevant security information
    And log entries should be tamper-evident

  @security @authentication @severity-high
  Scenario: Prevention of credential stuffing attacks
    Given login API endpoints
    When automated login attempts are made with commonly used credentials
    Then CAPTCHA or other challenge should be triggered
    And suspicious IP addresses should be flagged
    And attack detection mechanisms should be enabled

  @security @authentication @severity-high
  Scenario: Credential storage security
    Given user authentication credentials are stored in the system
    When examining the credential storage implementation
    Then passwords should be hashed with strong algorithms
    And appropriate key stretching should be used
    And salt values should be properly implemented
    And original passwords should never be recoverable
