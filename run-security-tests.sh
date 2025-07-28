#!/bin/bash

# Script to run BDD security tests with proper configuration
# Usage: ./run-security-tests.sh [environment] [tags]

# Default values
ENVIRONMENT=${1:-dev}
TAGS=${2:-@security}

# Colors for console output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}===== Mobile Authentication Security Tests =====${NC}"
echo -e "Environment: ${GREEN}$ENVIRONMENT${NC}"
echo -e "Tags: ${GREEN}$TAGS${NC}"

# Generate dynamic properties file based on environment if required by CI/CD
if [ "$CI" = "true" ]; then
  echo -e "${YELLOW}CI environment detected, generating dynamic properties...${NC}"
  
  # Create config directory if it doesn't exist
  mkdir -p src/test/resources/config

  # Create environment-specific properties file dynamically
  cat > src/test/resources/config/security-test-$ENVIRONMENT.properties << EOF
# Dynamically generated properties for $ENVIRONMENT environment
api.baseUrl=$API_BASE_URL
api.timeout=$API_TIMEOUT
zap.enabled=$ZAP_ENABLED
zap.proxyUrl=$ZAP_PROXY_URL
zap.apiKey=$ZAP_API_KEY
security.owaspLevel=$OWASP_LEVEL
security.sessionTimeout=$SESSION_TIMEOUT
EOF

  echo -e "${GREEN}Dynamic properties file generated.${NC}"
fi

# Set up ZAP proxy if enabled
if [ "$ZAP_ENABLED" = "true" ]; then
  echo -e "${YELLOW}Setting up ZAP proxy...${NC}"
  
  # Check if ZAP container is already running
  if [ ! "$(docker ps -q -f name=zap)" ]; then
    echo -e "Starting ZAP container..."
    docker run -d --name zap -p 8080:8080 -p 8090:8090 -i owasp/zap2docker-stable zap.sh -daemon -host 0.0.0.0 -port 8080 -config api.key=$ZAP_API_KEY
    sleep 10 # Give ZAP time to start up
  else
    echo -e "ZAP container already running."
  fi
fi

# Run tests with Maven
echo -e "${YELLOW}Running security tests...${NC}"
mvn clean test \
  -Dtest=com.securitytests.runners.BDDSecurityTestRunner \
  -Dcucumber.filter.tags="$TAGS" \
  -DtestEnvironment=$ENVIRONMENT \
  -Dsecurity.test.level=${OWASP_LEVEL:-2}

TEST_STATUS=$?

# Generate and move reports
echo -e "${YELLOW}Generating reports...${NC}"
mvn allure:report

# Create living documentation from feature files
echo -e "${YELLOW}Generating living documentation...${NC}"
java -cp target/classes com.securitytests.utils.docs.LivingDocumentationGenerator \
  src/test/resources/features/security \
  target/living-docs/security

# Clean up ZAP container if it was started by this script and not in CI
if [ "$ZAP_ENABLED" = "true" ] && [ "$CI" != "true" ] && [ "$KEEP_ZAP" != "true" ]; then
  echo -e "${YELLOW}Stopping ZAP container...${NC}"
  docker stop zap
  docker rm zap
fi

# Show test results summary
if [ $TEST_STATUS -eq 0 ]; then
  echo -e "${GREEN}===== Security tests passed successfully! =====${NC}"
  echo -e "Reports available at: ${GREEN}target/site/allure-maven-plugin/${NC}"
  echo -e "Living documentation: ${GREEN}target/living-docs/security/${NC}"
else
  echo -e "${RED}===== Security tests failed! =====${NC}"
  echo -e "Check logs and reports for details."
  echo -e "Reports available at: ${GREEN}target/site/allure-maven-plugin/${NC}"
fi

exit $TEST_STATUS
