# Mobile Authentication Security Test Automation Framework

## Overview

This comprehensive test automation framework focuses on authentication security testing for mobile applications. It features multi-layered testing capabilities covering UI, API, and performance aspects of authentication workflows, with robust logging, test data management, and reporting features.

## Key Features

- **Multi-layer Testing**: UI, API, and performance testing for comprehensive coverage
- **Advanced Test Data Strategy**: JavaFaker data generation with thread-local isolation and external data sources
- **Comprehensive Logging**: Structured logging with correlation IDs and contextual information
- **Flakiness Handling**: Retry mechanisms, smart waits, and stability metrics
- **Visual Validation**: CAPTCHA and UI element validation with AShot
- **Device Farm Integration**: Support for BrowserStack and AWS Device Farm
- **Email Testing**: Real email validation with MailSlurp
- **Mock Server**: Backend simulation with MockServer
- **Accessibility Testing**: Validates screens against accessibility guidelines
- **Performance Monitoring**: Measures and reports on authentication flow performance

## Architecture

```
mobile-auth-automation/
├── src/
│   ├── main/java/com/securitytests/
│   │   ├── api/                    # API testing components
│   │   ├── pages/                  # Page Objects for UI testing
│   │   └── utils/                  # Utility classes
│   │       ├── accessibility/      # Accessibility testing utilities
│   │       ├── data/               # Test data management
│   │       ├── logging/            # Structured logging
│   │       ├── performance/        # Performance monitoring
│   │       ├── retry/              # Test flakiness handling
│   │       └── wait/               # Smart wait strategies
│   ├── test/
│   │   ├── java/com/securitytests/
│   │   │   ├── api/                # API tests
│   │   │   ├── runners/            # Test runners
│   │   │   ├── steps/              # Cucumber step definitions
│   │   │   └── ui/                 # UI tests
│   │   └── resources/
│   │       ├── features/           # BDD feature files
│   │       └── testdata/           # External test data
└── docs/                           # Documentation
```

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven 3.6.3 or higher
- Appium 2.0 or higher
- Android SDK / iOS Development Tools
- API keys for MailSlurp and device farms (if used)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/mobile-auth-automation.git
```

2. Install dependencies:
```bash
cd mobile-auth-automation
mvn clean install -DskipTests
```

3. Configure environment variables:
```bash
export MAILSLURP_API_KEY=your_api_key
export BROWSERSTACK_USERNAME=your_username
export BROWSERSTACK_ACCESS_KEY=your_access_key
```

### Running Tests

#### UI Tests
```bash
mvn test -Dtest=com.securitytests.ui.*
```

#### API Tests
```bash
mvn test -Dtest=com.securitytests.api.*
```

#### Performance Tests
```bash
mvn test -Dtest=com.securitytests.api.AuthPerformanceTests
```

#### Accessibility Tests
```bash
mvn test -Dtest=com.securitytests.ui.AccessibilityTests
```

#### All Tests
```bash
mvn test
```

#### Cucumber BDD Tests
```bash
mvn test -Dtest=com.securitytests.runners.CucumberTestRunner
```

## Key Components

### 1. Test Data Strategy

#### DataGenerator
- Uses JavaFaker to generate realistic test data
- Thread-local storage for parallel test isolation
- Specialized generators for usernames, passwords, emails, etc.

#### ExternalDataReader
- Supports CSV, Excel, and JSON formats
- Handles structured test data for different scenarios
- Enables data-driven testing patterns

### 2. Structured Logging

The `StructuredLogger` provides:
- Test and step correlation with unique IDs
- Contextual information attached to each log entry
- Log formatting suitable for aggregation and analysis
- Integration with Allure reporting

### 3. Flakiness Handling

#### RetryAnalyzer
- Automatic retry of flaky tests
- Collection of test stability metrics
- Reporting on test flakiness patterns

#### SmartWait
- Resilient element waiting with recovery strategies
- Platform-specific waiting optimizations
- Timeout handling with diagnostic information

### 4. API Testing

- Comprehensive API client for authentication endpoints
- Performance measurement of API calls
- Validation of security policies and rate limiting

### 5. Accessibility Testing

- Automated validation against WCAG guidelines
- Detailed reporting of accessibility issues
- Integration with visual testing

### 6. Performance Monitoring

- Precise timing of authentication operations
- Statistical analysis of performance metrics
- Performance reports with Allure integration

## Best Practices

1. **Data Isolation**: Always use thread-local test data for parallel execution.
2. **Logging Context**: Include operation and test IDs in logs for correlation.
3. **Smart Waiting**: Use the SmartWait utility instead of Thread.sleep.
4. **API + UI Testing**: Validate critical flows at both API and UI levels.
5. **Visual Validation**: Use screenshots for complex UI elements like CAPTCHA.
6. **Performance Baselines**: Establish and monitor against performance baselines.
7. **Accessibility First**: Run accessibility tests early in development.

## Troubleshooting

### Common Issues

1. **Test Flakiness**
   - Check device stability and connection
   - Review SmartWait timeout configuration
   - Check for overlapping element locators

2. **Performance Degradation**
   - Compare with baseline metrics
   - Check for memory leaks in test code
   - Verify device resources during test execution

3. **CI/CD Integration**
   - Ensure proper environment variables
   - Configure appropriate timeouts for device farms
   - Set up retry mechanisms for network issues

## Contributing

1. Follow the existing code structure and naming conventions
2. Add unit tests for new utilities
3. Update documentation for new features
4. Run the full test suite before submitting changes

## Maintenance and Extensions

### Adding New Test Types
1. Create appropriate package structure
2. Implement supporting utilities
3. Add documentation and examples
4. Update CI/CD workflow

### Adding Device Farm Support
1. Update DeviceFarmManager with new provider
2. Configure authentication and capabilities
3. Test on actual devices before committing

## License

This project is licensed under the MIT License - see the LICENSE file for details.
