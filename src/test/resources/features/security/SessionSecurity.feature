Feature: Session Security Management
  As a security analyst
  I want to ensure that mobile authentication sessions are secured
  So that user accounts are protected from session-based attacks

  Background:
    Given the authentication API is available
    And test user accounts are configured

  @security @session @severity-critical
  Scenario: Session timeout enforcement
    Given a user is authenticated with the mobile app
    When the user is inactive for the timeout period
    Then the session should expire
    And the user should be redirected to the login screen
    And access to protected resources should be denied

  @security @session @severity-critical
  Scenario: Session fixation resistance
    Given a user has an existing unauthenticated session
    When the user successfully authenticates
    Then a new session identifier should be generated
    And the original session identifier should be invalidated
    And the new session should have proper security attributes

  @security @session @severity-high
  Scenario: Concurrent session handling
    Given a user is authenticated with the mobile app
    When the user authenticates from a different device
    Then the system should detect concurrent sessions
    And the user should be notified of multiple active sessions
    And one of the following security actions should occur:
      | action                          |
      | Allow both sessions             |
      | Terminate the oldest session    |
      | Require re-authentication       |
      | Force logout of all sessions    |

  @security @session @severity-high
  Scenario: Session termination upon logout
    Given a user is authenticated with the mobile app
    When the user logs out
    Then the session should be completely terminated
    And the session cookie should be deleted
    And session data should be removed from the server
    And access to protected resources should be denied

  @security @session @severity-high
  Scenario: Session cookie security attributes
    Given a user is authenticated with the mobile app
    When examining the session cookies
    Then secure flag should be set
    And httpOnly flag should be set
    And SameSite attribute should be set appropriately
    And secure cookie transmission should be enforced

  @security @token @severity-critical
  Scenario: JWT token validation
    Given a user is authenticated with the mobile app
    When examining the issued JWT token
    Then the token should have a valid structure
    And the token should not contain sensitive data
    And the token should have a reasonable expiry time
    And the token signature should be verified with the proper key

  @security @token @severity-high
  Scenario Outline: Token protection against tampering
    Given a user is authenticated with the mobile app
    When the JWT token is modified by tampering with the <component>
    Then access to protected resources should be denied
    And token validation should fail with appropriate error

    Examples:
      | component   |
      | header      |
      | payload     |
      | signature   |

  @security @session @severity-medium
  Scenario: Session persistence after app restart
    Given a user is authenticated with the mobile app
    When the app is closed and reopened
    Then the user session should remain valid
    And the user should not be prompted to log in again
    And access to protected resources should be maintained

  @security @session @severity-high
  Scenario: Session invalidation after password change
    Given a user is authenticated with the mobile app
    When the user changes their password
    Then all active sessions should be invalidated
    And the user should be required to log in with the new password

  @security @headers @severity-medium
  Scenario: Security headers validation
    Given a user is authenticated with the mobile app
    When examining the API response headers
    Then security headers should be properly configured:
      | header                      | value                           |
      | Strict-Transport-Security   | max-age=31536000; includeSubDomains |
      | X-Content-Type-Options      | nosniff                         |
      | X-XSS-Protection           | 1; mode=block                   |
      | Content-Security-Policy     | default-src 'self'              |
