FROM maven:3.8-openjdk-11-slim

# Install additional tools
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    git \
    postgresql-client \
    redis-tools \
    && rm -rf /var/lib/apt/lists/*

# Install Allure
ENV ALLURE_VERSION=2.22.1
RUN wget -O allure-${ALLURE_VERSION}.zip https://github.com/allure-framework/allure2/releases/download/${ALLURE_VERSION}/allure-${ALLURE_VERSION}.zip \
    && unzip allure-${ALLURE_VERSION}.zip -d /opt/ \
    && ln -s /opt/allure-${ALLURE_VERSION}/bin/allure /usr/bin/allure \
    && rm allure-${ALLURE_VERSION}.zip

# Set working directory
WORKDIR /workspace

# Copy Maven configuration
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src
COPY docker ./docker

# Create directories for logs and reports
RUN mkdir -p /workspace/target/allure-results \
    && mkdir -p /workspace/target/reports \
    && mkdir -p /var/log/tests

# Set environment variables
ENV MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"

# Default command
CMD ["mvn", "clean", "test"]