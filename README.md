# Mobile Authentication Security Test Automation

A complete mobile test automation framework using Java, TestNG, and Appium focused on security testing for authentication and password recovery workflows.

## Project Overview

This framework simulates real-world antifraud and security test scenarios for mobile applications, including:

1. **Login Security Tests**:
   - Testing 3 consecutive failed login attempts
   - CAPTCHA challenge validation after multiple failures
   - Login form validation

2. **Password Recovery Flow**:
   - Complete "Forgot password" flow automation
   - Email verification with MailSlurp API
   - Token extraction and validation
   - Redirection validation after password reset

## Project Structure

```
mobile-auth-automation/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── securitytests/
│   │   │           ├── config/      # Appium and test configurations
│   │   │           ├── pages/       # Page Object Models
│   │   │           └── utils/       # Test utilities and helpers
│   │   └── resources/
│   │       └── config.properties    # Configuration properties
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── securitytests/
│       │           └── tests/       # TestNG test classes
│       └── resources/
│           ├── testdata/            # Test data files
│           └── testng.xml           # TestNG configuration
└── pom.xml                          # Maven dependencies
```

## Setup Instructions

### Prerequisites

1. Java JDK 11 or higher
2. Maven 3.6 or higher
3. Appium Server 2.0 or higher
4. Android SDK (for Android tests) or Xcode (for iOS tests)
5. A running Android Emulator or iOS Simulator
6. MailSlurp API key (for email testing)

### Installation

1. Clone this repository
2. Install the required dependencies:

```bash
mvn clean install -DskipTests
```

3. Configure `src/main/resources/config.properties` with your specific settings:
   - Set your Appium server URL
   - Configure your device settings
   - Add your MailSlurp API key

### Setting up Mock Backend

The project includes a mock server to simulate backend endpoints for:
- CAPTCHA verification
- Password reset email sending
- Token validation

The mock server starts automatically during test execution and runs on port 8080 by default.

## Running Tests

### Running All Tests

```bash
mvn clean test
```

### Running Specific Test Classes

```bash
mvn clean test -Dtest=LoginSecurityTest
mvn clean test -Dtest=PasswordRecoveryTest
```

### Generating Allure Reports

```bash
mvn clean test
mvn allure:report
```

The generated report will be available in `target/site/allure-maven-plugin/` directory.

## Key Features

### Page Object Model

The framework implements the Page Object Model (POM) design pattern for better maintainability and reusability.

### Data-Driven Testing

Tests use TestNG data providers to run multiple test cases with different data sets.

### Email Integration

The framework uses MailSlurp to create real email inboxes for testing password recovery flows and email verification.

### Mock Server

The project includes a mock server to simulate backend API responses for comprehensive testing without relying on an actual backend.

### Allure Reporting

The framework integrates with Allure reporting to provide detailed test reports with screenshots for failures.

## Best Practices Demonstrated

1. **Security Testing**: Focus on authentication flows and security validations
2. **Clean Architecture**: Separation of concerns between pages, tests and utilities
3. **Reusability**: Common methods abstracted in base classes
4. **Logging**: Comprehensive logging for debugging
5. **Error Handling**: Robust error handling and screenshots on failures

## Extending the Framework

### Adding New Page Objects

Create a new class extending `BasePage` in the `com.securitytests.pages` package.

### Adding New Tests

Create a new test class extending `BaseTest` in the `com.securitytests.tests` package and use TestNG annotations.

## Troubleshooting

### Common Issues

1. **Appium Connection Issues**:
   - Ensure Appium server is running
   - Verify the URL in config.properties

2. **Device Not Found**:
   - Check that emulator/simulator is running
   - Verify device name in config.properties

3. **Element Not Found Exceptions**:
   - Update locator strategies in page objects
   - Increase wait timeouts in config.properties

## License

This project is created for demonstration purposes and is available under the MIT License.
